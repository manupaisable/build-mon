(ns build-mon.vso-release-api
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]))

(defn- validate-200-response [response]
  (if (= (:status response) 200)
    response
    (throw (ex-info "Response status was not 200" {:response response}))))

(defn- get-json-body [get-fn url]
  (-> url get-fn validate-200-response :body (json/parse-string true)))

(defn- map-release-info [release vso-release-api-data]
  (let [release-url (-> release :_links :self :href)
        get-fn (:get-fn vso-release-api-data)]
    (get-json-body get-fn release-url)))

(defn- retrieve-last-two-releases [vso-release-api-data release-definition-id]
  (let [{:keys [get-fn account project logger]} vso-release-api-data
        last-two-releases-url (str "https://" account  ".vsrm.visualstudio.com/defaultcollection/"
                                   project "/_apis/release/releases?api-version=3.0-preview.2&definitionId=" release-definition-id "&$top=2")]
    (try (-> (get-json-body get-fn last-two-releases-url) :value)
         (catch Exception e
           ((:log-exception logger) "Bad Response when attempting to retrieve last two releases." e)))))

(defn- retrieve-release-info [vso-release-api-data release-definition-id]
  (let [[release previous-release] (retrieve-last-two-releases vso-release-api-data release-definition-id)
        release-info (map-release-info release vso-release-api-data)
        previous-release-info (map-release-info previous-release vso-release-api-data)]
    (when release
      {:release release-info
       :previous-release previous-release-info})))

(defn- retrieve-release-definitions [vso-release-api-data]
  (let [{:keys [get-fn account project release_definition_filter logger]} vso-release-api-data
        url (str "https://" account  ".vsrm.visualstudio.com/defaultcollection/"
                 project "/_apis/release/definitions?api-version=3.0-preview.2&name="
                 release_definition_filter)]
    (try (-> (get-json-body get-fn url) :value)
         (catch Exception e
           ((:log-exception logger) "Bad Response when attempting to retrieve release definitions." e)))))

(defn vso-release-api-fns [logger get-fn account project release_definition_filter]
  (let [vso-release-api-data {:get-fn get-fn :logger logger
                              :account account :project project :release_definition_filter release_definition_filter}]
    {:retrieve-release-info (partial retrieve-release-info vso-release-api-data)
     :retrieve-release-definitions (partial retrieve-release-definitions vso-release-api-data)}))
