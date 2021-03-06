(ns build-mon.vso-api
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]))

(defn- validate-200-response [response]
  (if (= (:status response) 200)
    response
    (throw (ex-info "Response status was not 200" {:response response}))))

(defn- get-json-body [get-fn url]
  (-> url get-fn validate-200-response :body (json/parse-string true)))

(defn- retrieve-commit-message [vso-api-data build]
  (let [{:keys [get-fn account logger]} vso-api-data
        repository-id (-> build :repository :id)
        source-version (:sourceVersion build)
        commit-url (str "https://" account ".visualstudio.com/defaultcollection/_apis/tfvc/changesets?api-version=1.0&$top=1")]
;                        repository-id "/commits/" source-version "?api-version=1.0")]
    (try (-> (get-json-body get-fn commit-url) :comment)
         (catch Exception e
           ((:log-exception logger) "Bad Response when attempting to retrieve commit message." e)))))

(defn- retrieve-last-two-builds [vso-api-data build-definition-id]
  (let [{:keys [get-fn account project logger]} vso-api-data
        last-two-builds-url (str "https://" account  ".visualstudio.com/defaultcollection/"
                                 project "/_apis/build/builds?api-version=2.0&$top=2"
                                 "&definitions=" build-definition-id)]
    (try (-> (get-json-body get-fn last-two-builds-url) :value)
         (catch Exception e
           ((:log-exception logger) "Bad Response when attempting to retrieve last two builds." e)))))

(defn- retrieve-build-info [vso-api-data build-definition-id]
  (let [[build previous-build] (retrieve-last-two-builds vso-api-data build-definition-id)
        commit-message (retrieve-commit-message vso-api-data build)]
    (when build
      {:build build
       :previous-build previous-build
       :commit-message commit-message})))

(defn- retrieve-build-definitions [vso-api-data]
  (let [{:keys [get-fn account project build_definition_filter logger]} vso-api-data
        build-definitions-url (str "https://" account  ".visualstudio.com/defaultcollection/"
                                   project "/_apis/build/definitions?api-version=2.0&name="
                                   build_definition_filter)]
    (try (-> (get-json-body get-fn build-definitions-url) :value)
         (catch Exception e
           ((:log-exception logger) "Bad Response when attempting to retrieve build definitions." e)))))

(defn vso-api-get-fn [token]
  (fn [url] @(http/get url {:basic-auth ["USERNAME CAN BE ANY VALUE" token]
                            :accept :json :follow-redirects false})))

(defn vso-api-fns [logger get-fn account project build_definition_filter]
  (let [vso-api-data {:get-fn get-fn :logger logger
                      :account account :project project :build_definition_filter build_definition_filter}]
    {:retrieve-build-info (partial retrieve-build-info vso-api-data)
     :retrieve-build-definitions (partial retrieve-build-definitions vso-api-data)}))
