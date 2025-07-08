(ns vkteams-bot.config.core
  "VK Teams Bot configuration management"
  (:require [environ.core :refer [env]]
            [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s]))

;; Configuration specifications
(s/def ::bot-token string?)
(s/def ::api-url string?)
(s/def ::webhook-url string?)
(s/def ::webhook-port int?)
(s/def ::log-level #{:debug :info :warn :error})
(s/def ::timeout-ms int?)

(s/def ::config
  (s/keys :req-un [::bot-token]
          :opt-un [::api-url ::webhook-url ::webhook-port ::log-level ::timeout-ms]))

(def default-config
  {:api-url "https://example.com/bot/v1"
   :webhook-port 8080
   :log-level :info
   :timeout-ms 30000})

(defn load-config-from-file
  "Load configuration from YAML file"
  [file-path]
  (try
    (when (.exists (io/file file-path))
      (with-open [reader (io/reader file-path)]
        (yaml/parse-string (slurp reader) true)))
    (catch Exception e
      (log/warn "Failed to load config from file" file-path e)
      {})))

(defn load-config-from-env
  "Load configuration from environment variables"
  []
  (let [env-config {}]
    (cond-> env-config
      (env :vkteams-bot-api-token) (assoc :bot-token (env :vkteams-bot-api-token))
      (env :vkteams-bot-api-url) (assoc :api-url (env :vkteams-bot-api-url))
      (env :vkteams-bot-webhook-url) (assoc :webhook-url (env :vkteams-bot-webhook-url))
      (env :vkteams-bot-webhook-port) (assoc :webhook-port (Integer/parseInt (env :vkteams-bot-webhook-port)))
      (env :vkteams-bot-log-level) (assoc :log-level (keyword (env :vkteams-bot-log-level)))
      (env :vkteams-bot-timeout-ms) (assoc :timeout-ms (Integer/parseInt (env :vkteams-bot-timeout-ms))))))

(defn merge-configs
  "Merge multiple configuration maps with precedence"
  [& configs]
  (apply merge configs))

(defn load-config
  "Load configuration from multiple sources"
  ([]
   (load-config nil))
  ([config-file]
   (let [file-config (if config-file
                       (load-config-from-file config-file)
                       {})
         env-config (load-config-from-env)
         final-config (merge-configs default-config file-config env-config)]
     (log/info "Loaded configuration:" (dissoc final-config :bot-token))
     final-config)))

(defn validate-config
  "Validate configuration against spec"
  [config]
  (if (s/valid? ::config config)
    config
    (let [problems (s/explain-str ::config config)]
      (throw (ex-info "Invalid configuration"
                      {:problems problems :config config})))))

(defn get-config
  "Get validated configuration"
  ([]
   (get-config nil))
  ([config-file]
   (-> (load-config config-file)
       validate-config)))

(defn config-from-map
  "Create configuration from map"
  [config-map]
  (-> (merge-configs default-config config-map)
      validate-config))

;; Configuration helpers
(defn bot-token
  "Get bot token from config"
  [config]
  (:bot-token config))

(defn webhook-config
  "Get webhook configuration"
  [config]
  (select-keys config [:webhook-url :webhook-port]))

(defn has-webhook?
  "Check if webhook is configured"
  [config]
  (boolean (:webhook-url config)))

(defn polling-config?
  "Check if polling mode is configured"
  [config]
  (not (has-webhook? config)))

(defn timeout-ms
  "Get timeout in milliseconds"
  [config]
  (:timeout-ms config))

(defn log-level
  "Get log level"
  [config]
  (:log-level config))

(defn api-url
  "Get API URL"
  [config]
  (:api-url config))
