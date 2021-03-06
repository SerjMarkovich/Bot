---
layout: default
title: activator
permalink: activator
parent: Scenario DSL
---

[ActivatorContext](https://github.com/just-ai/jaicf-kotlin/blob/master/core/src/main/kotlin/com/justai/jaicf/context/ActivatorContext.kt) contains an [activator](activators)-related details of every request and is available through `activator` variable in action block.

A corresponding activator that handled a user's request generates its class instance.
An activator-related instance could be achieved through a null-safe extension of the corresponding activator.
For example:

```kotlin
state("helper") {
    state("ask4name") {
        activators {
            catchAll()
            intent("name")
        }

        action {
            var name: String? = null

            activator.dialogflow?.run {
                name = slots.fieldsMap["name"]?.stringValue
            }
            activator.alexaIntent?.run {
                name = slots["firstName"]?.value
            }
            activator.catchAll?.run {
                name = request.input
            }

            if (name.isNullOrBlank()) {
                reactions.say("Sorry, I didn't get it. Could you repeat please?")
            } else {
                reactions.goBack(name)
            }
        }
    }
}
```

In general this class contains only a `confidence` field with value between 0 and 1.
It shows a confidence of the recognised user's intent. For event and query requests it is always 1.

Each activator specifies its own implementation of this class appending additional activator-related fields like `slots` for intent activators like [AlexaIntentActivatorContext](https://github.com/just-ai/jaicf-kotlin/blob/master/channels/alexa/src/main/kotlin/com/justai/jaicf/channel/alexa/activator/AlexaIntentActivatorContext.kt) or `groups` in [RegexActivatorContext](https://github.com/just-ai/jaicf-kotlin/blob/master/core/src/main/kotlin/com/justai/jaicf/activator/regex/RegexActivatorContext.kt) that is generated by [RegexActivator](https://github.com/just-ai/jaicf-kotlin/blob/master/core/src/main/kotlin/com/justai/jaicf/activator/regex/RegexActivator.kt).

> Learn more about activators [here](activators).