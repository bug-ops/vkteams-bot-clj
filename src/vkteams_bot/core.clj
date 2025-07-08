(ns vkteams-bot.core
  "VK Teams Bot main namespace"
  (:require [vkteams-bot.client.core :as client]
            [vkteams-bot.config.core :as config]
            [vkteams-bot.event.core :as event]
            [vkteams-bot.keyboard.core :as keyboard]
            [vkteams-bot.error.core :as error]
            [clojure.tools.logging :as log]
            [manifold.deferred :as d])
  (:gen-class))

;; Re-export main functions for convenience
(def create-bot client/create-bot)
(def bot? client/bot?)

;; Configuration
(def get-config config/get-config)
(def config-from-map config/config-from-map)

;; Event handling
(def create-event-handler event/create-event-handler)
(def process-events event/process-events)

;; Keyboard creation
(def create-button keyboard/create-button)
(def callback-button keyboard/callback-button)
(def url-button keyboard/url-button)
(def create-keyboard keyboard/create-keyboard)
(def single-row-keyboard keyboard/single-row-keyboard)
(def yes-no-keyboard keyboard/yes-no-keyboard)
(def confirm-keyboard keyboard/confirm-keyboard)
(def inline-keyboard keyboard/inline-keyboard)

;; Error handling
(def api-error error/api-error)
(def network-error error/network-error)
(def error? error/error?)

;; Re-export macro
(defmacro with-error-handling [& body]
  `(error/with-error-handling ~@body))

(defn send-text-message
  "Send a text message"
  ([bot chat-id text]
   (send-text-message bot chat-id text {}))
  ([bot chat-id text opts]
   (client/send-message bot chat-id text opts)))

(defn send-file
  "Send a file"
  ([bot chat-id file-path]
   (send-file bot chat-id file-path {}))
  ([bot chat-id file-path opts]
   (client/send-file bot chat-id file-path opts)))

(defn send-message-with-keyboard
  "Send a message with inline keyboard"
  [bot chat-id text keyboard]
  (let [keyboard-markup (keyboard/inline-keyboard keyboard)]
    (send-text-message bot chat-id text keyboard-markup)))

(defn reply-to-message
  "Reply to a specific message"
  [bot chat-id text reply-to-message-id]
  (send-text-message bot chat-id text {:replyMsgId reply-to-message-id}))

(defn edit-text-message
  "Edit a text message"
  ([bot chat-id message-id text]
   (edit-text-message bot chat-id message-id text {}))
  ([bot chat-id message-id text opts]
   (client/edit-message bot chat-id message-id text opts)))

(defn delete-message
  "Delete a message"
  [bot chat-id message-id]
  (client/delete-message bot chat-id message-id))

(defn answer-callback
  "Answer callback query"
  ([bot query-id]
   (answer-callback bot query-id ""))
  ([bot query-id text]
   (answer-callback bot query-id text {}))
  ([bot query-id text opts]
   (client/answer-callback-query bot query-id text opts)))

(defn get-chat-info
  "Get chat information"
  [bot chat-id]
  (client/get-chat-info bot chat-id))

(defn get-chat-members
  "Get chat members"
  [bot chat-id]
  (client/get-chat-members bot chat-id))

(defn get-events
  "Get events from VK Teams Bot API using long polling.
  
  Parameters:
  - bot: VK Teams Bot instance
  - opts: Optional parameters map
    - :pollTime: Polling timeout in seconds (default: 30)
    - :lastEventId: ID of last received event (default: 0)
    - :limit: Maximum number of events to return
  
  Returns:
  Deferred containing response with :events key containing array of events.
  
  Example:
  (get-events bot {:pollTime 60 :lastEventId 123})"
  ([bot]
   (get-events bot {}))
  ([bot opts]
   (client/get-events bot opts)))

(defn get-file
  "Download file from VK Teams Bot API.
  
  Parameters:
  - bot: VK Teams Bot instance
  - file-id: Unique file identifier
  - opts: Optional parameters map
    - :path: Local path where to save the file
  
  Returns:
  Deferred containing file data or saved file path.
  
  Example:
  (get-file bot \"BAADBAADrwADBxuGMF5mFBe4ACC\" {:path \"/tmp/myfile.jpg\"})"
  ([bot file-id]
   (get-file bot file-id {}))
  ([bot file-id opts]
   (client/get-file bot file-id opts)))

(defn get-file-info
  "Get file information from VK Teams Bot API.
  
  Parameters:
  - bot: VK Teams Bot instance
  - file-id: Unique file identifier
  
  Returns:
  Deferred containing file metadata including size, name, and type.
  
  Example:
  (get-file-info bot \"BAADBAADrwADBxuGMF5mFBe4ACC\")"
  [bot file-id]
  (client/get-file-info bot file-id))

(defn pin-message
  "Pin message in chat. Requires admin privileges.
  
  Parameters:
  - bot: VK Teams Bot instance
  - chat-id: Unique chat identifier
  - message-id: ID of message to pin
  
  Returns:
  Deferred containing API response.
  
  Example:
  (pin-message bot \"chat@123\" \"msg456\")"
  [bot chat-id message-id]
  (client/pin-message bot chat-id message-id))

(defn unpin-message
  "Unpin message in chat. Requires admin privileges.
  
  Parameters:
  - bot: VK Teams Bot instance
  - chat-id: Unique chat identifier
  - message-id: ID of message to unpin
  
  Returns:
  Deferred containing API response.
  
  Example:
  (unpin-message bot \"chat@123\" \"msg456\")"
  [bot chat-id message-id]
  (client/unpin-message bot chat-id message-id))

(defn get-chat-admins
  "Get list of chat administrators.
  
  Parameters:
  - bot: VK Teams Bot instance
  - chat-id: Unique chat identifier
  
  Returns:
  Deferred containing array of admin user objects with userId and rights.
  
  Example:
  (get-chat-admins bot \"chat @123\")"
  [bot chat-id]
  (client/get-chat-admins bot chat-id))

(defn set-chat-title
  "Set chat title. Requires admin privileges.
  
  Parameters:
  - bot: VK Teams Bot instance
  - chat-id: Unique chat identifier
  - title: New chat title (max 255 characters)
  
  Returns:
  Deferred containing API response.
  
  Example:
  (set-chat-title bot \"chat @123\" \"New Awesome Chat Title\")"
  [bot chat-id title]
  (client/set-chat-title bot chat-id title))

(defn set-chat-about
  "Set chat description. Requires admin privileges.
  
  Parameters:
  - bot: VK Teams Bot instance
  - chat-id: Unique chat identifier
  - about: New chat description (max 512 characters)
  
  Returns:
  Deferred containing API response.
  
  Example:
  (set-chat-about bot \"chat @123\" \"This is a chat for discussing VK Teams Bot development\")"
  [bot chat-id about]
  (client/set-chat-about bot chat-id about))

(defn create-simple-bot
  "Create a simple bot with basic event handling"
  [config handlers]
  (let [bot (create-bot (config/bot-token config))
        event-handler (create-event-handler handlers)]
    {:bot bot
     :event-handler event-handler
     :config config}))

(defn handle-update
  "Handle a single update"
  [bot-instance update]
  (let [{:keys [event-handler]} bot-instance]
    (event/handle-event event-handler (event/parse-event update))))

(defn handle-updates
  "Handle multiple updates"
  [bot-instance updates]
  (doseq [update updates]
    (handle-update bot-instance update)))

(defn start-polling
  "Start polling for updates (placeholder for future implementation)"
  [bot-instance]
  (log/info "Polling mode not implemented yet")
  (throw (ex-info "Polling mode not implemented" {:bot bot-instance})))

(defn start-webhook
  "Start webhook server (placeholder for future implementation)"
  [bot-instance]
  (log/info "Webhook mode not implemented yet")
  (throw (ex-info "Webhook mode not implemented" {:bot bot-instance})))

(defn -main
  "Main entry point"
  [& args]
  (try
    (let [config (get-config (first args))
          bot (create-bot (config/bot-token config))]
      (log/info "VK Teams Bot started successfully")
      (log/info "Bot token configured:" (boolean (config/bot-token config)))
      (log/info "Configuration loaded:" (dissoc config :bot-token))

      ;; Example usage
      (println "VK Teams Bot is ready!")
      (println "Use this bot instance in your application:")
      (println "  (def my-bot (create-bot \"your-token\"))")
      (println "  (send-text-message my-bot \"chat-id\" \"Hello World!\")")

      ;; Keep the application running
      (Thread/sleep Long/MAX_VALUE))
    (catch Exception e
      (log/error "Failed to start bot:" (.getMessage e) e)
      (System/exit 1))))