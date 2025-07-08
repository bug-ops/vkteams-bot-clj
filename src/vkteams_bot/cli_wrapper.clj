(ns vkteams-bot.cli-wrapper
  "CLI wrapper around vkteams-bot-cli Rust binary"
  (:require [clojure.java.shell :as shell]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure.string :as str]))

(def ^:private cli-binary "vkteams-bot-cli")

(defn- execute-cli
  "Execute vkteams-bot-cli command with given arguments"
  [args & {:keys [env input]}]
  (let [env-vars (merge {"VKTEAMS_BOT_API_TOKEN" (System/getenv "VKTEAMS_BOT_API_TOKEN")
                         "VKTEAMS_BOT_API_URL" (System/getenv "VKTEAMS_BOT_API_URL")}
                        env)
        result (apply shell/sh cli-binary (concat args [:env env-vars :in input]))]
    (log/debug "CLI command:" cli-binary args "Exit code:" (:exit result))
    (if (zero? (:exit result))
      {:success true
       :output (:out result)
       :data (when-not (str/blank? (:out result))
               (try (json/parse-string (:out result) true)
                    (catch Exception e
                      (log/warn "Failed to parse JSON output:" (:out result))
                      (:out result))))}
      {:success false
       :error (:err result)
       :exit-code (:exit result)})))

;; Basic messaging operations
(defn send-text
  "Send text message using CLI"
  [user-id message & {:keys [token]}]
  (let [args ["send-text" "-u" user-id "-m" message]
        env (when token {"VKTEAMS_BOT_API_TOKEN" token})]
    (execute-cli args :env env)))

(defn send-file
  "Send file using CLI"
  [user-id file-path & {:keys [token caption]}]
  (let [args (cond-> ["send-file" "-u" user-id "-f" file-path]
               caption (concat ["-c" caption]))
        env (when token {"VKTEAMS_BOT_API_TOKEN" token})]
    (execute-cli args :env env)))

;; Event operations
(defn get-events
  "Get events using CLI with optional live monitoring"
  [& {:keys [live token limit]}]
  (let [args (cond-> ["get-events"]
               live (concat ["-l" "true"])
               limit (concat ["--limit" (str limit)]))
        env (when token {"VKTEAMS_BOT_API_TOKEN" token})]
    (execute-cli args :env env)))

(defn listen-events
  "Listen for events continuously with callback"
  [callback & {:keys [token]}]
  (let [env (when token {"VKTEAMS_BOT_API_TOKEN" token})]
    (future
      (loop []
        (let [result (execute-cli ["get-events" "-l" "true"] :env env)]
          (when (:success result)
            (when-let [events (get-in result [:data :events])]
              (doseq [event events]
                (try
                  (callback event)
                  (catch Exception e
                    (log/error "Error in event callback:" e))))))
          (Thread/sleep 1000)
          (recur))))))

;; File operations
(defn download-file
  "Download file using CLI"
  [file-id output-path & {:keys [token]}]
  (let [args ["get-file" "-i" file-id "-f" output-path]
        env (when token {"VKTEAMS_BOT_API_TOKEN" token})]
    (execute-cli args :env env)))

;; Storage operations (advanced features)
(defn storage-search-semantic
  "Perform semantic search in message history"
  [query & {:keys [token limit]}]
  (let [args (cond-> ["storage" "search-semantic" query]
               limit (concat ["--limit" (str limit)]))
        env (when token {"VKTEAMS_BOT_API_TOKEN" token})]
    (execute-cli args :env env)))

(defn storage-get-context
  "Get conversation context for chat"
  [chat-id & {:keys [token limit]}]
  (let [args (cond-> ["storage" "get-context" "-c" chat-id]
               limit (concat ["--limit" (str limit)]))
        env (when token {"VKTEAMS_BOT_API_TOKEN" token})]
    (execute-cli args :env env)))

(defn storage-stats
  "Get storage statistics"
  [& {:keys [token]}]
  (let [env (when token {"VKTEAMS_BOT_API_TOKEN" token})]
    (execute-cli ["storage" "stats"] :env env)))

;; Utility functions
(defn check-cli-available?
  "Check if vkteams-bot-cli is available in PATH"
  []
  (try
    (let [result (shell/sh "which" cli-binary)]
      (zero? (:exit result)))
    (catch Exception e
      (log/warn "Failed to check CLI availability:" e)
      false)))

(defn install-cli
  "Install vkteams-bot-cli using cargo"
  []
  (log/info "Installing vkteams-bot-cli via cargo...")
  (let [result (shell/sh "cargo" "install" "vkteams-bot-cli")]
    (if (zero? (:exit result))
      (do
        (log/info "Successfully installed vkteams-bot-cli")
        {:success true})
      (do
        (log/error "Failed to install vkteams-bot-cli:" (:err result))
        {:success false :error (:err result)}))))

;; High-level convenience functions
(defn create-bot-wrapper
  "Create a bot wrapper that uses CLI under the hood"
  [token]
  {:token token
   :type ::cli-wrapper
   :send-text (partial send-text)
   :send-file (partial send-file) 
   :get-events (partial get-events)
   :download-file (partial download-file)
   :search-semantic (partial storage-search-semantic)
   :get-context (partial storage-get-context)})

(defn send-message-with-wrapper
  "Send message using bot wrapper"
  [bot chat-id message]
  (when (= ::cli-wrapper (:type bot))
    ((:send-text bot) chat-id message :token (:token bot))))

;; Example usage function
(defn demo-cli-wrapper
  "Demonstrate CLI wrapper usage"
  [token]
  (log/info "Demonstrating CLI wrapper functionality")
  
  ;; Check if CLI is available
  (if-not (check-cli-available?)
    (do
      (log/warn "vkteams-bot-cli not found, attempting to install...")
      (install-cli))
    (log/info "vkteams-bot-cli is available"))
  
  ;; Send a test message
  (let [result (send-text "test-chat" "Hello from Clojure CLI wrapper!" :token token)]
    (if (:success result)
      (log/info "Message sent successfully:" (:data result))
      (log/error "Failed to send message:" (:error result))))
  
  ;; Get events
  (let [events-result (get-events :token token)]
    (if (:success events-result)
      (log/info "Retrieved events:" (count (get-in events-result [:data :events])))
      (log/error "Failed to get events:" (:error events-result))))
  
  ;; Demonstrate semantic search (if storage is configured)
  (let [search-result (storage-search-semantic "deployment issues" :token token :limit 5)]
    (if (:success search-result)
      (log/info "Semantic search completed:" (count (get-in search-result [:data :results])))
      (log/warn "Semantic search failed (storage might not be configured):" (:error search-result)))))

(comment
  ;; Usage examples:
  
  ;; Basic usage
  (send-text "chat@123" "Hello World!")
  (send-file "chat@123" "/path/to/file.jpg" :caption "Check this out!")
  
  ;; Event listening
  (def event-listener 
    (listen-events 
      (fn [event] 
        (println "Received event:" (:type event)))))
  
  ;; Advanced features
  (storage-search-semantic "deployment issues last week" :limit 10)
  (storage-get-context "chat@123" :limit 50)
  
  ;; Bot wrapper
  (def bot (create-bot-wrapper "your-token"))
  (send-message-with-wrapper bot "chat@123" "Hello!"))