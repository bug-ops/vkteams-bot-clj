# vkteams-bot-clj
VKTeams Bot with Clojure

# VK Teams Bot - Clojure

A VK Teams Bot API client library for Clojure, inspired by the Rust implementation from [bug-ops/vkteams-bot](https://github.com/bug-ops/vkteams-bot).

## Features

- ✅ **Complete API Client**: Full VK Teams Bot API V1 support
- ✅ **Event Handling**: Comprehensive event processing system
- ✅ **Keyboard Support**: Inline keyboards with buttons
- ✅ **Configuration Management**: YAML files and environment variables
- ✅ **Error Handling**: Robust error management and logging
- ✅ **Async Support**: Built on http-kit for async operations
- 🔄 **Webhook Support**: (Coming soon)
- 🔄 **Polling Support**: (Coming soon)

## Installation

Add to your `project.clj` dependencies:

```clojure
[vkteams-bot "0.1.0-SNAPSHOT"]
```

## Quick Start

### Using Environment Variables (Recommended)

```bash
# Set up environment variables
export VKTEAMS_BOT_API_TOKEN="your-bot-token"
export VKTEAMS_BOT_API_URL="https://example.com/bot/v1"
```

```clojure
(ns my-bot
  (:require [vkteams-bot.core :as bot]))

;; Create a bot from environment variables
(def my-bot (bot/create-bot-from-config))

;; Send a simple message
(bot/send-text-message my-bot "chat-id" "Hello World!")

;; Send a message with keyboard
(let [keyboard (bot/yes-no-keyboard)]
  (bot/send-message-with-keyboard my-bot "chat-id" "Do you like this bot?" keyboard))
```

### Using Direct Token

```clojure
(ns my-bot
  (:require [vkteams-bot.core :as bot]))

;; Create a bot with token (uses default API URL)
(def my-bot (bot/create-bot "your-bot-token"))

;; Send a simple message
(bot/send-text-message my-bot "chat-id" "Hello World!")
```

## Configuration

### Environment Variables

The library supports configuration through environment variables:

```bash
# Required
export VKTEAMS_BOT_API_TOKEN="your-bot-token"

# Optional - API URL (defaults to VK Teams internal API)
export VKTEAMS_BOT_API_URL="https://example.com/bot/v1"

# Optional - Webhook configuration
export VKTEAMS_BOT_WEBHOOK_URL="https://example.com/webhook"
export VKTEAMS_BOT_WEBHOOK_PORT="8080"
export VKTEAMS_BOT_LOG_LEVEL="info"
export VKTEAMS_BOT_TIMEOUT_MS="30000"
```

### Configuration File (config.yaml)

```yaml
# Required
bot-token: "your-bot-token"

# Optional - API URL (defaults to VK Teams internal API)
api-url: "https://example.com/bot/v1"

# Optional - Webhook configuration
webhook-url: "https://example.com/webhook"
webhook-port: 8080
log-level: info
timeout-ms: 30000
```

### Programmatic Configuration

```clojure
(def config (bot/config-from-map
              {:bot-token "your-bot-token"
               :api-url "https://example.com/bot/v1"
               :log-level :info}))
```

## API Reference

### Bot Creation

```clojure
;; Create bot with token (uses default configuration)
(def bot (bot/create-bot "token"))

;; Create bot from environment variables
(def bot (bot/create-bot-from-config))

;; Create bot with configuration file
(def config (bot/get-config "config.yaml"))
(def bot (bot/create-bot (bot/bot-token config) config))

;; Create bot with custom configuration
(def config (bot/config-from-map {:bot-token "token"
                                  :api-url "https://example.com/bot/v1"}))
(def bot (bot/create-bot (bot/bot-token config) config))
```

### Sending Messages

```clojure
;; Send text message
(bot/send-text-message bot "chat-id" "Hello!")

;; Send with options
(bot/send-text-message bot "chat-id" "Hello!" {:parseMode "markdown"})

;; Send file
(bot/send-file bot "chat-id" "/path/to/file.jpg")
(bot/send-file bot "chat-id" "/path/to/file.jpg" {:caption "Image description"})

;; Reply to message
(bot/reply-to-message bot "chat-id" "Reply text" "original-message-id")

;; Edit message
(bot/edit-text-message bot "chat-id" "message-id" "New text")

;; Delete message
(bot/delete-message bot "chat-id" "message-id")
```

### Keyboards

```clojure
;; Simple Yes/No keyboard
(def keyboard (bot/yes-no-keyboard))

;; Custom keyboard
(def keyboard (bot/create-keyboard
                [(bot/callback-button "Button 1" "data1")
                 (bot/callback-button "Button 2" "data2")]
                [(bot/url-button "Visit Site" "https://example.com")]))

;; Send message with keyboard
(bot/send-message-with-keyboard bot "chat-id" "Choose option:" keyboard)
```

### Event Handling

```clojure
;; Create event handler
(def handler (bot/create-event-handler
               {:message (fn [msg] (println "Message:" (:text msg)))
                :callback (fn [query] (println "Callback:" (:data query)))
                :chat (fn [event] (println "Chat event:" (:type event)))}))

;; Handle events
(bot/handle-update {:bot bot :event-handler handler} raw-update)
```

### Chat Management

```clojure
;; Get chat info
(bot/get-chat-info bot "chat-id")

;; Get chat members
(bot/get-chat-members bot "chat-id")

;; Get chat administrators
(bot/get-chat-admins bot "chat-id")

;; Set chat title (requires admin privileges)
(bot/set-chat-title bot "chat-id" "New Chat Title")

;; Set chat description (requires admin privileges)
(bot/set-chat-about bot "chat-id" "New chat description")

;; Pin message (requires admin privileges)
(bot/pin-message bot "chat-id" "message-id")

;; Unpin message (requires admin privileges)
(bot/unpin-message bot "chat-id" "message-id")
```

### Files and Events

```clojure
;; Get file information
(bot/get-file-info bot "file-id")

;; Download file
(bot/get-file bot "file-id")
(bot/get-file bot "file-id" {:path "/local/path/to/save"})

;; Get events (long polling)
(bot/get-events bot)
(bot/get-events bot {:pollTime 30 :lastEventId 123 :limit 10})
```

### Callback Queries

```clojure
;; Answer callback query
(bot/answer-callback bot "query-id" "Response text")
(bot/answer-callback bot "query-id" "Response text" {:showAlert true})
```

## Examples

Check out the examples in `src/vkteams_bot/examples/`:

- `basic.clj` - Basic bot functionality
- `extended.clj` - Advanced features demonstration including files, events, and chat management

## Error Handling

The library includes comprehensive error handling:

```clojure
;; Wrap operations in error handling
(bot/with-error-handling
  (bot/send-text-message bot "chat-id" "Hello!"))

;; Check for errors
(let [result (bot/send-text-message bot "chat-id" "Hello!")]
  (if (bot/error? result)
    (println "Error:" (bot/error-message result))
    (println "Success:" result)))
```

## Development

### Running Tests

```bash
lein test
```

### Building

```bash
lein uberjar
```

### Running Examples

```bash
lein run -m vkteams-bot.examples.basic
```
