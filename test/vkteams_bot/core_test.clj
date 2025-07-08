(ns vkteams-bot.core-test
  (:require [clojure.test :refer :all]
            [vkteams-bot.core :as bot]
            [vkteams-bot.client.core :as client]
            [vkteams-bot.config.core :as config]
            [vkteams-bot.keyboard.core :as keyboard]
            [vkteams-bot.event.core :as event]
            [vkteams-bot.error.core :as error]))

(deftest test-bot-creation
  (testing "Bot creation with valid token"
    (let [bot (bot/create-bot "test-token")]
      (is (client/bot? bot))
      (is (= "test-token" (:token bot)))))
  
  (testing "Bot creation with empty token should fail"
    (is (thrown? IllegalArgumentException
                 (bot/create-bot "")))
    (is (thrown? IllegalArgumentException
                 (bot/create-bot nil)))))

(deftest test-configuration
  (testing "Default configuration"
    (let [config (config/merge-configs config/default-config)]
      (is (= 8080 (:webhook-port config)))
      (is (= :info (:log-level config)))
      (is (= 30000 (:timeout-ms config)))))
  
  (testing "Configuration from map"
    (let [config (config/config-from-map {:bot-token "test-token"})]
      (is (= "test-token" (:bot-token config)))
      (is (= 8080 (:webhook-port config))))))

(deftest test-keyboard-creation
  (testing "Simple button creation"
    (let [button (keyboard/create-button "Test")]
      (is (= "Test" (:text button)))
      (is (nil? (:url button)))
      (is (nil? (:callback-data button)))))
  
  (testing "Callback button creation"
    (let [button (keyboard/callback-button "Click me" "callback_data")]
      (is (= "Click me" (:text button)))
      (is (= "callback_data" (:callback-data button)))))
  
  (testing "URL button creation"
    (let [button (keyboard/url-button "Visit" "https://example.com")]
      (is (= "Visit" (:text button)))
      (is (= "https://example.com" (:url button)))))
  
  (testing "Keyboard creation"
    (let [keyboard (keyboard/create-keyboard
                     [(keyboard/callback-button "Button 1" "data1")
                      (keyboard/callback-button "Button 2" "data2")])]
      (is (= 1 (count (:buttons keyboard))))
      (is (= 2 (count (first (:buttons keyboard)))))))
  
  (testing "Yes/No keyboard"
    (let [keyboard (keyboard/yes-no-keyboard)]
      (is (= 1 (count (:buttons keyboard))))
      (is (= 2 (count (first (:buttons keyboard))))))))

(deftest test-event-parsing
  (testing "Parse new message event"
    (let [raw-event {:eventType "newMessage"
                     :payload {:msgId "123"
                               :text "Hello"
                               :timestamp 1234567890
                               :chat {:chatId "chat123" :type "private"}
                               :from {:userId "user123" :firstName "John"}}}
          event (event/parse-event raw-event)]
      (is (= :new-message (:type event)))
      (is (= "123" (-> event :message :id)))
      (is (= "Hello" (-> event :message :text)))
      (is (= "chat123" (-> event :message :chat :id)))))
  
  (testing "Parse callback query event"
    (let [raw-event {:eventType "callbackQuery"
                     :payload {:queryId "query123"
                               :callbackData "button_clicked"
                               :from {:userId "user123" :firstName "John"}}}
          event (event/parse-event raw-event)]
      (is (= :callback-query (:type event)))
      (is (= "query123" (-> event :query :id)))
      (is (= "button_clicked" (-> event :query :data)))))
  
  (testing "Parse unknown event"
    (let [raw-event {:eventType "unknownEvent" :payload {}}
          event (event/parse-event raw-event)]
      (is (= :unknown (:type event))))))

(deftest test-error-handling
  (testing "Error creation"
    (let [error (error/api-error "API failed" 500)]
      (is (error/error? error))
      (is (= ::error/api-error (error/error-type error)))
      (is (= "API failed" (error/error-message error)))
      (is (= 500 (error/error-code error)))))
  
  (testing "Network error"
    (let [error (error/network-error "Connection failed")]
      (is (error/error? error))
      (is (= ::error/network-error (error/error-type error)))
      (is (= "Connection failed" (error/error-message error)))))
  
  (testing "Config error"
    (let [error (error/config-error "Invalid config")]
      (is (error/error? error))
      (is (= ::error/config-error (error/error-type error))))))

(deftest test-new-api-methods
  (testing "get-events method"
    (let [bot (bot/create-bot "test-token")]
      (is (fn? bot/get-events))
      (is (some? (bot/get-events bot)))))
  
  (testing "get-file method"
    (let [bot (bot/create-bot "test-token")]
      (is (fn? bot/get-file))
      (is (some? (bot/get-file bot "test-file-id")))))
  
  (testing "get-file-info method"
    (let [bot (bot/create-bot "test-token")]
      (is (fn? bot/get-file-info))
      (is (some? (bot/get-file-info bot "test-file-id")))))
  
  (testing "pin-message method"
    (let [bot (bot/create-bot "test-token")]
      (is (fn? bot/pin-message))
      (is (some? (bot/pin-message bot "test-chat-id" "test-message-id")))))
  
  (testing "unpin-message method"
    (let [bot (bot/create-bot "test-token")]
      (is (fn? bot/unpin-message))
      (is (some? (bot/unpin-message bot "test-chat-id" "test-message-id")))))
  
  (testing "get-chat-admins method"
    (let [bot (bot/create-bot "test-token")]
      (is (fn? bot/get-chat-admins))
      (is (some? (bot/get-chat-admins bot "test-chat-id")))))
  
  (testing "set-chat-title method"
    (let [bot (bot/create-bot "test-token")]
      (is (fn? bot/set-chat-title))
      (is (some? (bot/set-chat-title bot "test-chat-id" "New Title")))))
  
  (testing "set-chat-about method"
    (let [bot (bot/create-bot "test-token")]
      (is (fn? bot/set-chat-about))
      (is (some? (bot/set-chat-about bot "test-chat-id" "New Description"))))))