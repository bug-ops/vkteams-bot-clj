(ns vkteams-bot.examples.extended
  "Extended examples demonstrating all VK Teams Bot API methods"
  (:require [vkteams-bot.core :as bot]
            [vkteams-bot.config.core :as config]
            [clojure.tools.logging :as log]
            [manifold.deferred :as d]))

(defn demonstrate-messaging-methods
  "Demonstrate basic messaging methods"
  [bot-instance chat-id]
  (log/info "Demonstrating messaging methods")
  
  ;; Send a simple text message
  (d/chain
    (bot/send-text-message bot-instance chat-id "Hello! This is a test message.")
    (fn [response]
      (log/info "Sent message response:" response)
      
      ;; Edit the message if we have message ID
      (when-let [msg-id (get-in response [:msgId])]
        (bot/edit-text-message bot-instance chat-id msg-id "Updated message content!"))))
  
  ;; Send message with reply
  (bot/reply-to-message bot-instance chat-id "This is a reply!" "some-message-id"))

(defn demonstrate-file-methods
  "Demonstrate file-related methods"
  [bot-instance chat-id file-id]
  (log/info "Demonstrating file methods")
  
  ;; Get file information
  (d/chain
    (bot/get-file-info bot-instance file-id)
    (fn [file-info]
      (log/info "File info:" file-info)
      
      ;; Download file if info is available
      (when file-info
        (bot/get-file bot-instance file-id {:path "/tmp/downloaded-file"}))))
  
  ;; Send a file
  (bot/send-file bot-instance chat-id "/path/to/file.txt" {:caption "Example file"}))

(defn demonstrate-chat-management
  "Demonstrate chat management methods"
  [bot-instance chat-id]
  (log/info "Demonstrating chat management methods")
  
  ;; Get chat information
  (d/chain
    (bot/get-chat-info bot-instance chat-id)
    (fn [chat-info]
      (log/info "Chat info:" chat-info)
      
      ;; Get chat members
      (d/chain
        (bot/get-chat-members bot-instance chat-id)
        (fn [members]
          (log/info "Chat members:" members)
          
          ;; Get chat administrators
          (d/chain
            (bot/get-chat-admins bot-instance chat-id)
            (fn [admins]
              (log/info "Chat admins:" admins)))))))
  
  ;; Update chat settings (admin only)
  (bot/set-chat-title bot-instance chat-id "New Chat Title")
  (bot/set-chat-about bot-instance chat-id "Updated chat description"))

(defn demonstrate-message-management
  "Demonstrate message management methods"
  [bot-instance chat-id message-id]
  (log/info "Demonstrating message management methods")
  
  ;; Pin a message
  (d/chain
    (bot/pin-message bot-instance chat-id message-id)
    (fn [response]
      (log/info "Pin message response:" response)
      
      ;; Later unpin the message
      (bot/unpin-message bot-instance chat-id message-id)))
  
  ;; Delete a message
  (bot/delete-message bot-instance chat-id message-id))

(defn handle-new-message
  "Handle new message event"
  [bot-instance event]
  (let [chat-id (get-in event [:payload :chat :chatId])
        message-text (get-in event [:payload :text])]
    (log/info "New message in chat" chat-id ":" message-text)
    
    ;; Echo the message
    (bot/send-text-message bot-instance chat-id (str "You said: " message-text))))

(defn handle-callback-query
  "Handle callback query event"
  [bot-instance event]
  (let [query-id (get-in event [:payload :queryId])
        callback-data (get-in event [:payload :callbackData])]
    (log/info "Callback query:" callback-data)
    
    ;; Answer the callback
    (bot/answer-callback bot-instance query-id "Callback received!" {:showAlert false})))

(defn demonstrate-events-handling
  "Demonstrate events handling"
  [bot-instance]
  (log/info "Demonstrating events handling")
  
  ;; Get events with polling
  (d/chain
    (bot/get-events bot-instance {:pollTime 30 :lastEventId 0})
    (fn [events-response]
      (log/info "Events response:" events-response)
      
      ;; Process events
      (when-let [events (get events-response :events)]
        (doseq [event events]
          (log/info "Processing event:" event)
          
          ;; Handle different event types
          (case (get event :type)
            "newMessage" (handle-new-message bot-instance event)
            "callbackQuery" (handle-callback-query bot-instance event)
            (log/info "Unknown event type:" (get event :type))))))))

(defn demonstrate-keyboard-methods
  "Demonstrate keyboard creation and usage"
  [bot-instance chat-id]
  (log/info "Demonstrating keyboard methods")
  
  ;; Create inline keyboard
  (let [keyboard (bot/inline-keyboard
                  [[{:text "Button 1" :callbackData "btn1"}
                    {:text "Button 2" :callbackData "btn2"}]
                   [{:text "URL Button" :url "https://example.com"}]])]
    (bot/send-message-with-keyboard bot-instance chat-id "Choose an option:" keyboard)))

(defn run-complete-example
  "Run a complete example demonstrating all methods"
  [config-path]
  (try
    (let [config (config/get-config config-path)
          bot-instance (bot/create-bot (config/bot-token config))
          test-chat-id "test-chat-id"
          test-message-id "test-message-id"
          test-file-id "test-file-id"]
      
      (log/info "Starting complete VK Teams Bot API demonstration")
      
      ;; Demonstrate all method categories
      (demonstrate-messaging-methods bot-instance test-chat-id)
      (demonstrate-file-methods bot-instance test-chat-id test-file-id)
      (demonstrate-chat-management bot-instance test-chat-id)
      (demonstrate-message-management bot-instance test-chat-id test-message-id)
      (demonstrate-keyboard-methods bot-instance test-chat-id)
      
      ;; Start event polling (this would run continuously in a real application)
      (demonstrate-events-handling bot-instance)
      
      (log/info "VK Teams Bot API demonstration completed"))
    
    (catch Exception e
      (log/error "Error in demonstration:" (.getMessage e) e))))

(defn -main
  "Main entry point for extended examples"
  [& args]
  (let [config-path (or (first args) "config.yaml")]
    (run-complete-example config-path)))