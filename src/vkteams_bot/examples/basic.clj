(ns vkteams-bot.examples.basic
  "Basic examples of using VK Teams Bot"
  (:require [vkteams-bot.core :as bot]
            [clojure.tools.logging :as log]
            [manifold.deferred :as d]
            [clojure.pprint :as pp]))

(def config
  {:bot-token "001.0354321036.3374569672:1000003205"
   :log-level :info})

(defn echo-handler
  "Simple echo message handler"
  [message]
  (let [chat-id (-> message :chat :id)
        text (:text message)]
    (when (and chat-id text)
      (log/info "Echoing message:" text)
      ;; Echo the message back
      text)))

(defn callback-handler
  "Handle callback queries"
  [query]
  (let [query-id (:id query)
        data (:data query)]
    (log/info "Received callback:" data)
    (case data
      "yes" "You selected Yes!"
      "no" "You selected No!"
      "confirm" "Confirmed!"
      "cancel" "Cancelled!"
      "Unknown callback")))

(defn create-example-bot
  "Create a bot with example handlers"
  []
  (let [bot (bot/create-bot (:bot-token config))
        handlers {:message echo-handler
                  :callback callback-handler}]
    {:bot bot
     :handlers handlers}))

(defn send-hello-message
  "Send a hello message with keyboard"
  [bot chat-id]
  (let [keyboard (bot/yes-no-keyboard "👍 Yes" "👎 No")]
    (println "📤 Sending hello message with keyboard...")
    (d/chain
      (bot/send-message-with-keyboard
       bot
       chat-id
       "Hello! Do you like this bot?"
       keyboard)
      (fn [response]
        (println "✅ Hello message response:")
        (pp/pprint response)
        response))))

(defn send-menu-message
  "Send a menu message"
  [bot chat-id]
  (let [keyboard (bot/create-keyboard
                  [(bot/callback-button "📊 Statistics" "stats")
                   (bot/callback-button "⚙️ Settings" "settings")]
                  [(bot/callback-button "ℹ️ Help" "help")
                   (bot/callback-button "🚪 Exit" "exit")])]
    (println "📤 Sending menu message...")
    (d/chain
      (bot/send-message-with-keyboard
       bot
       chat-id
       "Choose an option:"
       keyboard)
      (fn [response]
        (println "✅ Menu message response:")
        (pp/pprint response)
        response))))

(defn send-simple-message
  "Send a simple text message"
  [bot chat-id text]
  (println "📤 Sending simple message:" text)
  (d/chain
    (bot/send-text-message bot chat-id text)
    (fn [response]
      (println "✅ Simple message response:")
      (pp/pprint response)
      response)))

(defn get-chat-information
  "Get chat information"
  [bot chat-id]
  (println "📤 Getting chat info for:" chat-id)
  (d/chain
    (bot/get-chat-info bot chat-id)
    (fn [response]
      (println "✅ Chat info response:")
      (pp/pprint response)
      response)))

(defn test-all-api-methods
  "Test various API methods"
  [bot chat-id]
  (println "\n🧪 Testing all API methods...\n")
  
  ;; Simple message
  (d/chain
    (send-simple-message bot chat-id "🤖 Testing VK Teams Bot API!")
    
    ;; Chat info
    (fn [_] (get-chat-information bot chat-id))
    
    ;; Hello message with keyboard
    (fn [_] 
      (Thread/sleep 1000) ; Small delay between messages
      (send-hello-message bot chat-id))
    
    ;; Menu message
    (fn [_]
      (Thread/sleep 1000)
      (send-menu-message bot chat-id))
    
    ;; Final message
    (fn [_]
      (Thread/sleep 1000)
      (send-simple-message bot chat-id "✅ All tests completed!"))
    
    (fn [_]
      (println "\n🎉 All API tests completed successfully!"))))

(defn run-example
  "Run the example bot (manual testing)"
  []
  (try
    (let [{:keys [bot]} (create-example-bot)
          chat-id "1660744@chat.agent"]
      (log/info "Bot created successfully")
      (println "🤖 VK Teams Bot Example Started")
      (println "==========================================")
      (println "Chat ID:" chat-id)
      (println "==========================================")

      ;; Test all API methods with response logging
      @(test-all-api-methods bot chat-id)
      
      ;; Keep the program running for a bit to see all responses
      (println "\n⏱️  Waiting for all async responses...")
      (Thread/sleep 3000)
      
      (println "\n✅ Example completed!")
      (println "Check your VK Teams chat for the messages."))
    (catch Exception e
      (log/error "Error running example:" (.getMessage e) e)
      (println "❌ Error occurred:" (.getMessage e)))))

(defn -main
  "Main function for running examples"
  [& args]
  (run-example))