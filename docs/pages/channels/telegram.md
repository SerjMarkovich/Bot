---
layout: default
title: Telegram
permalink: Telegram
parent: Channels
---

<p align="center">
    <img src="/assets/images/channels/telegram.png" width="128" height="128"/>
</p>

<h1 align="center">Telegram messenger channel</h1>

Allows to create chatbots for [Telegram](https://core.telegram.org/bots).

_Built on top of [Kotlin Telegram Bot](https://github.com/kotlin-telegram-bot/kotlin-telegram-bot) library._

## How to use

#### 1. Include Telegram dependency to your _build.gradle_

```kotlin
implementation("com.just-ai.jaicf:telegram:$jaicfVersion")
```

**Replace `$jaicfVersion` with the latest version ![](https://img.shields.io/github/v/release/just-ai/jaicf-kotlin?color=%23000&label=&style=flat-square)**

Also add _Jitpack_ to repositories:

```kotlin
repositories {
    mavenCentral()
    jcenter()
    maven(uri("https://jitpack.io"))
}
```

#### 2. Use Telegram `request` and `reactions` in your scenarios' actions

```kotlin
action {
    // Telegram incoming message
    val message = request.telegram?.message

    // Fetch username
    val username = message?.chat?.username
    
    // Use Telegram-specified response builders
    reactions.telegram?.say("Are you agree?", listOf("Yes", "No"))
    reactions.telegram?.image("https://address.com/image.jpg", "Image caption")
    reactions.telegram?.api?.sendAudio(message?.chat?.id, File("audio.mp3"))

    // Or use standard response builders
    reactions.say("Hello there!")
    reactions.image("https://address.com/image.jpg")
}
```

_Note that Telegram bot works as long polling. This means that every reactions' method actually sends a response to the user._

> Refer to the [TelegramReactions](https://github.com/just-ai/jaicf-kotlin/blob/master/channels/telegram/src/main/kotlin/com/justai/jaicf/channel/telegram/TelegramReactions.kt) class to learn more about available response builders.

#### Native API

You can use native Telegram API directly via `reactions.telegram?.api`.
This enables you to build any response that Telegram supports using channel-specific features.
As well as fetch some data from Telegram bot API (like [getMe](https://core.telegram.org/bots/api#getme) for example).

```kotlin
action {
    val me = reactions.telegram?.run {
        api.getMe().first?.body()?.result
    }
}
```

> Learn more about available API methods [here](https://github.com/kotlin-telegram-bot/kotlin-telegram-bot/blob/main/telegram/src/main/kotlin/com/github/kotlintelegrambot/Bot.kt).

#### 3. Create a new bot in Telegram

Create a new bot using Telegram's `@BotFather` and any Telegram client as described [here](https://core.telegram.org/bots#6-botfather).
Copy your new bot's **access token** to the clipboard.

#### 4. Create and run Telegram channel

Using [JAICP](https://github.com/just-ai/jaicf-kotlin/tree/master/channels/jaicp)

_For local development:_
```kotlin
fun main() {
    JaicpPollingConnector(
        botApi = helloWorldBot,
        accessToken = "your JAICF project token",
        channels = listOf(
            TelegramChannel
        )
    ).runBlocking()
}
```

_For cloud production:_
```kotlin
fun main() {
    JaicpServer(
        botApi = helloWorldBot,
        accessToken = "your JAICF project token",
        channels = listOf(
            TelegramChannel
        )
    ).start(wait = true)
}
```

Or locally:
```kotlin
fun main() {
    TelegramChannel(helloWorldBot, "access token").run()
}
```

## Commands

Telegram enables users not only to send a text queries or use buttons.
It also provides an ability to send [commands](https://core.telegram.org/bots#commands) that start from slash.

The most known command of the Telegram is "/start" that is sending once the user starts using your chatbot.
Your scenario must handle this command via [regex activator](https://github.com/just-ai/jaicf-kotlin/wiki/Regex-Activator) to react on the first user's request.

```kotlin
val HelloWorldScenario = Scenario {
    state("main") {
        activators {
            regex("/start")
        }

        action {
            reactions.say("Hello there!")
        }
    }
}
```

To make it work, just add `RegexActivator` to the array of activators in your agent's configuration:

```kotlin
val helloWorldBot = BotEngine(
    scenario = HelloWorldScenario,
    activators = arrayOf(
        RegexActivator,
        CatchAllActivator
    )
)
```

The same way you can react on ony other Telegram commands.

## Events

User can send not only a text queries to your Telegram bot.
They can also send contacts and locations for example.
These messages contain non-text queries and can be handled in your scenarios via `event` activators.

```kotlin
state("events") {
    activators {
        event(TelegramEvent.LOCATION)
        event(TelegramEvent.CONTACT)
    }

    action {
        val location = request.telegram?.location
        val contact = request.telegram?.contact
    }
}
```

## Buttons

Telegram allows to add [keyboard](https://core.telegram.org/bots#keyboards) or [inline keyboard](https://core.telegram.org/bots#inline-keyboards-and-on-the-fly-updating) to the text message reply.
This means that it's not possible to add a keyboard without an actual text response.

```kotlin
action {
    reactions.say("Click on the button below")
    reactions.buttons("Click me", "Or me")
}
```

> This code generates [inline]((https://core.telegram.org/bots#inline-keyboards-and-on-the-fly-updating)) keyboard right below the text "Click on the button below".
Once the user clicks on any of these buttons, the title of the clicked one returns to the bot as a new query.

To add any keyboard to the response, you can use a channel-specific methods:

```kotlin
action {
    // Append inline keyboard
    reactions.telegram?.say("Are you agree?", listOf("Yes", "No"))

    // Append arbitrary keyboard layout
    reactions.telegram?.say(
        "Could you please send me your contact?", 
        replyMarkup = KeyboardReplyMarkup(
            listOf(listOf(KeyboardButton("Send", requestContact = true), KeyboardButton("No")))
        )
    )
}
```

You can also remove keyboard sending a `ReplyKeyboardRemove` in the response:

```kotlin
action {
    reactions.telegram?.say("Okay then!", replyMarkup = ReplyKeyboardRemove())
}
```

> Refer to the [TelegramReactions](https://github.com/just-ai/jaicf-kotlin/blob/master/channels/telegram/src/main/kotlin/com/justai/jaicf/channel/telegram/TelegramReactions.kt) class to learn more about buttons replies.

## Payments

You can accept payments for services or goods you provide from Telegram users.
To do this, you need to [connect a payment system and obtain its unique token](https://core.telegram.org/bots/payments#getting-a-token).

```kotlin
action {
    val info = PaymentInvoiceInfo(
        "title",
        "description",
        "unique payload",
        "381964478:TEST:67912",
        "unique-start-parameter",
        "USD",
        listOf(LabeledPrice("price", BigInteger.valueOf(20_00)))
    )
    reactions.telegram?.sendInvoice(info)
}
```

To learn about available currencies and more, you can read [the telegram payment documentation](https://core.telegram.org/bots/payments).

### Goods availability

Before proceeding with the payment, Telegram sends a request to bot to check the goods availability. 
This request triggers preCheckout event in scenario. Add the preCheckout as **a top level state**.

> Note that when paying in group chats, payment confirmation is sent to the user who sent the payment request, not the entire chat. So there will be created a separate context for the user. 
>If the user communicates with the user in a personal chat the context remains the same.

```kotlin
state("preCheckout") {
    activators {
        event(TelegramEvent.PRE_CHECKOUT)
    }

    action(telegram.preCheckout) {
        reactions.answerPreCheckoutQuery(request.preCheckoutQuery.id, true)
    }
}
```

> You always need to handle the telegramPreCheckout event in the script. Otherwise payments will fail, and all subsequent user messages will be handled in the CatchAll state.

Also you can handle successfulPayment event inside nested states in the TelegramPayment state

```kotlin
state("successfulPayment") {
    activators {
        event(TelegramEvent.SUCCESSFUL_PAYMENT)
    }

    action(telegram.successfulPayment) {
        reactions.say("We are glad you bought from us")
    }
}
```
