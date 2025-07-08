(ns vkteams-bot.event.core
  "VK Teams Bot event handling"
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s]
            [java-time.core :as time]))

;; Event specifications
(s/def ::event-type #{:newMessage :editedMessage :deletedMessage 
                      :pinnedMessage :unpinnedMessage :newChatMembers
                      :leftChatMember :changedChatInfo :callbackQuery})

(s/def ::chat-id string?)
(s/def ::message-id string?)
(s/def ::user-id string?)
(s/def ::text string?)
(s/def ::timestamp int?)

(s/def ::event-data
  (s/keys :req-un [::event-type ::chat-id ::timestamp]
          :opt-un [::message-id ::user-id ::text]))

(defrecord Chat [id type title public])
(defrecord User [id first-name last-name nick])
(defrecord Message [id timestamp text chat from])
(defrecord CallbackQuery [id from message data])

(defn parse-chat
  "Parse chat data from API response"
  [chat-data]
  (when chat-data
    (->Chat 
      (:chatId chat-data)
      (keyword (:type chat-data))
      (:title chat-data)
      (:public chat-data))))

(defn parse-user
  "Parse user data from API response"
  [user-data]
  (when user-data
    (->User
      (:userId user-data)
      (:firstName user-data)
      (:lastName user-data)
      (:nick user-data))))

(defn parse-message
  "Parse message data from API response"
  [message-data]
  (when message-data
    (->Message
      (:msgId message-data)
      (:timestamp message-data)
      (:text message-data)
      (parse-chat (:chat message-data))
      (parse-user (:from message-data)))))

(defn parse-callback-query
  "Parse callback query data from API response"
  [query-data]
  (when query-data
    (->CallbackQuery
      (:queryId query-data)
      (parse-user (:from query-data))
      (parse-message (:message query-data))
      (:callbackData query-data))))

(defn parse-event
  "Parse raw event data into structured event"
  [raw-event]
  (try
    (let [event-type (keyword (:eventType raw-event))
          payload (:payload raw-event)]
      (case event-type
        :newMessage {:type :new-message
                     :message (parse-message payload)}
        :editedMessage {:type :edited-message
                        :message (parse-message payload)}
        :deletedMessage {:type :deleted-message
                         :message (parse-message payload)}
        :pinnedMessage {:type :pinned-message
                        :message (parse-message payload)}
        :unpinnedMessage {:type :unpinned-message
                          :message (parse-message payload)}
        :newChatMembers {:type :new-chat-members
                         :chat (parse-chat (:chat payload))
                         :new-members (map parse-user (:newMembers payload))}
        :leftChatMember {:type :left-chat-member
                         :chat (parse-chat (:chat payload))
                         :left-member (parse-user (:leftMember payload))}
        :changedChatInfo {:type :changed-chat-info
                          :chat (parse-chat payload)}
        :callbackQuery {:type :callback-query
                        :query (parse-callback-query payload)}
        {:type :unknown
         :raw-event raw-event}))
    (catch Exception e
      (log/error "Failed to parse event" raw-event e)
      {:type :error
       :error (str "Failed to parse event: " (.getMessage e))
       :raw-event raw-event})))

(defprotocol EventHandler
  "Protocol for handling VK Teams Bot events"
  (handle-event [this event])
  (handle-message [this message])
  (handle-callback-query [this query])
  (handle-chat-event [this event]))

(defn create-event-handler
  "Create event handler with custom handlers"
  [handlers]
  (reify EventHandler
    (handle-event [this event]
      (log/debug "Handling event:" (:type event))
      (case (:type event)
        :new-message (handle-message this (:message event))
        :edited-message (handle-message this (:message event))
        :callback-query (handle-callback-query this (:query event))
        (:new-chat-members :left-chat-member :changed-chat-info) 
        (handle-chat-event this event)
        (when-let [default-handler (:default handlers)]
          (default-handler event))))
    
    (handle-message [this message]
      (when-let [message-handler (:message handlers)]
        (message-handler message)))
    
    (handle-callback-query [this query]
      (when-let [callback-handler (:callback handlers)]
        (callback-handler query)))
    
    (handle-chat-event [this event]
      (when-let [chat-handler (:chat handlers)]
        (chat-handler event)))))

(defn process-events
  "Process multiple events with event handler"
  [events handler]
  (doseq [raw-event events]
    (let [event (parse-event raw-event)]
      (try
        (handle-event handler event)
        (catch Exception e
          (log/error "Error handling event" event e))))))