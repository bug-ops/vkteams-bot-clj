(ns vkteams-bot.client.core
  "VK Teams Bot API Client core functionality"
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [manifold.deferred :as d]
            [clojure.string :as str]
            [vkteams-bot.config.core :as config]))

(defprotocol BotClient
  "Protocol for VK Teams Bot API client"
  (send-message [this chat-id text opts])
  (send-file [this chat-id file-path opts])
  (get-chat-info [this chat-id])
  (get-chat-members [this chat-id])
  (edit-message [this chat-id message-id text opts])
  (delete-message [this chat-id message-id])
  (answer-callback-query [this query-id text opts])
  (get-events [this opts])
  (get-file [this file-id opts])
  (get-file-info [this file-id])
  (pin-message [this chat-id message-id])
  (unpin-message [this chat-id message-id])
  (get-chat-admins [this chat-id])
  (set-chat-title [this chat-id title])
  (set-chat-about [this chat-id about]))

(defn- make-request
  "Make HTTP request to VK Teams Bot API.

  Handles HTTP communication with VK Teams Bot API endpoints.
  Automatically adds authentication token and converts responses to Clojure data.

  Parameters:
  - method: HTTP method (:get, :post, etc.)
  - endpoint: API endpoint path (e.g., '/messages/sendText')
  - token: Bot authentication token
  - params: Request parameters map
  - config: Bot configuration map

  Returns:
  Deferred containing parsed JSON response or error map."
  [method endpoint token params config]
  (let [url (str (config/api-url config) endpoint)
        request-opts {:method method
                      :url url
                      :headers {"Content-Type" "application/json"}
                      :query-params (merge {:token token} params)}]
    (log/debug "Making request to" url "with params" params)
    (d/chain
     (http/request request-opts)
     (fn [response]
       (let [status (:status response)
             body (:body response)]
         (if (= 200 status)
           (try
             (json/parse-string body true)
             (catch Exception e
               (log/error "Failed to parse response body" body e)
               {:error "Failed to parse response"}))
           (do
             (log/error "Request failed with status" status "body" body)
             {:error (str "Request failed with status " status)})))))))

(defn- prepare-params
  "Prepare parameters for API request.

  Converts Clojure data structures to API-compatible format:
  - Removes nil values
  - Converts keywords to strings
  - Serializes maps to JSON strings
  - Converts all other values to strings

  Parameters:
  - params: Map of request parameters

  Returns:
  Map with processed parameters ready for HTTP request."
  [params]
  (into {}
        (for [[k v] params
              :when (not (nil? v))]
          [k (cond
               (keyword? v) (name v)
               (map? v) (json/generate-string v)
               :else (str v))])))

(defrecord VKTeamsBot [token config]
  BotClient

  (send-message [this chat-id text opts]
    (let [params (prepare-params
                  (merge {:chatId chat-id :text text} opts))]
      (make-request :get "/messages/sendText" token params config)))

  (send-file [this chat-id file-path opts]
    (let [params (prepare-params
                  (merge {:chatId chat-id :file file-path} opts))]
      (make-request :get "/messages/sendFile" token params config)))

  (get-chat-info [this chat-id]
    (let [params (prepare-params {:chatId chat-id})]
      (make-request :get "/chats/getInfo" token params config)))

  (get-chat-members [this chat-id]
    (let [params (prepare-params {:chatId chat-id})]
      (make-request :get "/chats/getMembers" token params config)))

  (edit-message [this chat-id message-id text opts]
    (let [params (prepare-params
                  (merge {:chatId chat-id :msgId message-id :text text} opts))]
      (make-request :get "/messages/editText" token params config)))

  (delete-message [this chat-id message-id]
    (let [params (prepare-params {:chatId chat-id :msgId message-id})]
      (make-request :get "/messages/deleteMessages" token params config)))

  (answer-callback-query [this query-id text opts]
    (let [params (prepare-params
                  (merge {:queryId query-id :text text} opts))]
      (make-request :get "/messages/answerCallbackQuery" token params config)))

  (get-events [this opts]
    (let [params (prepare-params opts)]
      (make-request :get "/events/get" token params config)))

  (get-file [this file-id opts]
    (let [params (prepare-params (merge {:fileId file-id} opts))]
      (make-request :get "/files/getFile" token params config)))

  (get-file-info [this file-id]
    (let [params (prepare-params {:fileId file-id})]
      (make-request :get "/files/getInfo" token params config)))

  (pin-message [this chat-id message-id]
    (let [params (prepare-params {:chatId chat-id :msgId message-id})]
      (make-request :get "/messages/pinMessage" token params config)))

  (unpin-message [this chat-id message-id]
    (let [params (prepare-params {:chatId chat-id :msgId message-id})]
      (make-request :get "/messages/unpinMessage" token params config)))

  (get-chat-admins [this chat-id]
    (let [params (prepare-params {:chatId chat-id})]
      (make-request :get "/chats/getAdmins" token params config)))

  (set-chat-title [this chat-id title]
    (let [params (prepare-params {:chatId chat-id :title title})]
      (make-request :get "/chats/setTitle" token params config)))

  (set-chat-about [this chat-id about]
    (let [params (prepare-params {:chatId chat-id :about about})]
      (make-request :get "/chats/setAbout" token params config))))

(defn create-bot
  "Create a new VK Teams Bot instance"
  ([token]
   (create-bot token (config/merge-configs config/default-config {:bot-token token})))
  ([token config]
   (when (str/blank? token)
     (throw (IllegalArgumentException. "Bot token cannot be empty")))
   (->VKTeamsBot token config)))

(defn create-bot-from-config
  "Create a new VK Teams Bot instance from configuration"
  ([]
   (create-bot-from-config (config/get-config)))
  ([config]
   (let [token (config/bot-token config)]
     (create-bot token config))))

(defn bot?
  "Check if object is a VK Teams Bot instance"
  [obj]
  (instance? VKTeamsBot obj))
