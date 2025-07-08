(ns vkteams-bot.keyboard.core
  "VK Teams Bot keyboard and button management"
  (:require [cheshire.core :as json]
            [clojure.spec.alpha :as s]))

;; Button specifications
(s/def ::text string?)
(s/def ::url string?)
(s/def ::callback-data string?)
(s/def ::style #{:primary :secondary :attention})

(s/def ::button
  (s/keys :req-un [::text]
          :opt-un [::url ::callback-data ::style]))

(s/def ::keyboard
  (s/coll-of (s/coll-of ::button) :kind vector?))

(defrecord Button [text url callback-data style])
(defrecord Keyboard [buttons])

(defn create-button
  "Create a new button"
  ([text]
   (create-button text nil nil nil))
  ([text url-or-callback]
   (if (and url-or-callback (not (.startsWith url-or-callback "http")))
     (create-button text nil url-or-callback nil)
     (create-button text url-or-callback nil nil)))
  ([text url callback-data]
   (create-button text url callback-data nil))
  ([text url callback-data style]
   (->Button text url callback-data style)))

(defn url-button
  "Create a URL button"
  ([text url]
   (url-button text url nil))
  ([text url style]
   (create-button text url nil style)))

(defn callback-button
  "Create a callback button"
  ([text callback-data]
   (callback-button text callback-data nil))
  ([text callback-data style]
   (create-button text nil callback-data style)))

(defn create-keyboard
  "Create a new keyboard from button rows"
  [& button-rows]
  (->Keyboard (vec button-rows)))

(defn single-row-keyboard
  "Create a keyboard with a single row of buttons"
  [& buttons]
  (create-keyboard (vec buttons)))

(defn button-grid
  "Create a grid of buttons with specified number of columns"
  [buttons columns]
  (let [rows (partition-all columns buttons)]
    (apply create-keyboard (map vec rows))))

(defn- button->map
  "Convert button to map for JSON serialization"
  [button]
  (let [base-map {:text (:text button)}
        with-url (if (:url button)
                   (assoc base-map :url (:url button))
                   base-map)
        with-callback (if (:callback-data button)
                        (assoc with-url :callbackData (:callback-data button))
                        with-url)
        with-style (if (:style button)
                     (assoc with-callback :style (name (:style button)))
                     with-callback)]
    with-style))

(defn keyboard->json
  "Convert keyboard to JSON string for API"
  [keyboard]
  (let [button-rows (map (fn [row]
                          (map button->map row))
                        (:buttons keyboard))]
    (json/generate-string button-rows)))

(defn inline-keyboard
  "Create inline keyboard markup for messages"
  [keyboard]
  {:inlineKeyboardMarkup (keyboard->json keyboard)})

;; Predefined keyboard helpers
(defn yes-no-keyboard
  "Create a simple yes/no keyboard"
  ([]
   (yes-no-keyboard "Yes" "No"))
  ([yes-text no-text]
   (single-row-keyboard
     (callback-button yes-text "yes" :primary)
     (callback-button no-text "no" :secondary))))

(defn confirm-keyboard
  "Create a confirmation keyboard"
  ([]
   (confirm-keyboard "Confirm" "Cancel"))
  ([confirm-text cancel-text]
   (single-row-keyboard
     (callback-button confirm-text "confirm" :primary)
     (callback-button cancel-text "cancel" :attention))))

(defn menu-keyboard
  "Create a menu keyboard from options"
  [options]
  (let [buttons (map (fn [option]
                      (if (string? option)
                        (callback-button option option)
                        (callback-button (:text option) (:data option))))
                    options)]
    (apply create-keyboard (map vector buttons))))

(defn numbered-keyboard
  "Create a keyboard with numbered options"
  [options]
  (let [buttons (map-indexed (fn [idx option]
                              (callback-button 
                                (str (inc idx) ". " option)
                                (str idx)))
                            options)]
    (apply create-keyboard (map vector buttons))))

(defn remove-keyboard
  "Remove keyboard markup"
  []
  {:removeKeyboard true})