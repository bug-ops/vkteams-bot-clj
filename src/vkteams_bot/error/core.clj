(ns vkteams-bot.error.core
  "VK Teams Bot error handling"
  (:require [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s]))

;; Error types
(derive ::api-error ::error)
(derive ::network-error ::error)
(derive ::config-error ::error)
(derive ::parsing-error ::error)
(derive ::validation-error ::error)

;; Error specifications
(s/def ::error-type keyword?)
(s/def ::error-message string?)
(s/def ::error-code int?)
(s/def ::error-data any?)

(s/def ::bot-error
  (s/keys :req-un [::error-type ::error-message]
          :opt-un [::error-code ::error-data]))

(defn create-error
  "Create a bot error"
  ([error-type message]
   (create-error error-type message nil nil))
  ([error-type message error-code]
   (create-error error-type message error-code nil))
  ([error-type message error-code error-data]
   {:error-type error-type
    :error-message message
    :error-code error-code
    :error-data error-data}))

(defn api-error
  "Create an API error"
  ([message]
   (api-error message nil))
  ([message error-code]
   (api-error message error-code nil))
  ([message error-code error-data]
   (create-error ::api-error message error-code error-data)))

(defn network-error
  "Create a network error"
  ([message]
   (network-error message nil))
  ([message error-data]
   (create-error ::network-error message nil error-data)))

(defn config-error
  "Create a configuration error"
  ([message]
   (config-error message nil))
  ([message error-data]
   (create-error ::config-error message nil error-data)))

(defn parsing-error
  "Create a parsing error"
  ([message]
   (parsing-error message nil))
  ([message error-data]
   (create-error ::parsing-error message nil error-data)))

(defn validation-error
  "Create a validation error"
  ([message]
   (validation-error message nil))
  ([message error-data]
   (create-error ::validation-error message nil error-data)))

(defn error?
  "Check if value is a bot error"
  [value]
  (and (map? value)
       (contains? value :error-type)
       (contains? value :error-message)))

(defn error-type
  "Get error type"
  [error]
  (:error-type error))

(defn error-message
  "Get error message"
  [error]
  (:error-message error))

(defn error-code
  "Get error code"
  [error]
  (:error-code error))

(defn error-data
  "Get error data"
  [error]
  (:error-data error))

(defn log-error
  "Log error with appropriate level"
  [error]
  (case (error-type error)
    ::api-error (log/error "API Error:" (error-message error) 
                          "Code:" (error-code error)
                          "Data:" (error-data error))
    ::network-error (log/error "Network Error:" (error-message error)
                              "Data:" (error-data error))
    ::config-error (log/error "Config Error:" (error-message error)
                             "Data:" (error-data error))
    ::parsing-error (log/warn "Parsing Error:" (error-message error)
                             "Data:" (error-data error))
    ::validation-error (log/warn "Validation Error:" (error-message error)
                                "Data:" (error-data error))
    (log/error "Unknown Error:" (error-message error)
              "Type:" (error-type error)
              "Data:" (error-data error))))

(defn wrap-error-logging
  "Wrap function to log errors"
  [f]
  (fn [& args]
    (try
      (let [result (apply f args)]
        (when (error? result)
          (log-error result))
        result)
      (catch Exception e
        (let [error (create-error ::exception (.getMessage e) nil e)]
          (log-error error)
          error)))))

(defn handle-api-response
  "Handle API response and convert to error if needed"
  [response]
  (cond
    (error? response) response
    (and (map? response) (:error response))
    (api-error (:error response) (:error-code response) response)
    (and (map? response) (:ok response) (not (:ok response)))
    (api-error "API request failed" nil response)
    :else response))

(defmacro with-error-handling
  "Execute body with error handling"
  [& body]
  `(try
     ~@body
     (catch Exception e#
       (let [error# (create-error ::exception (.getMessage e#) nil e#)]
         (log-error error#)
         error#))))

(defn error->exception
  "Convert error to exception"
  [error]
  (ex-info (error-message error)
           {:error-type (error-type error)
            :error-code (error-code error)
            :error-data (error-data error)}))