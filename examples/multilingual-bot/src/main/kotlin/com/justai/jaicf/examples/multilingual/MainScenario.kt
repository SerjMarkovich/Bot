package com.justai.jaicf.examples.multilingual

import com.justai.jaicf.api.routing.routing
import com.justai.jaicf.builder.Scenario
import com.justai.jaicf.examples.multilingual.service.LanguageDetectService
import com.justai.jaicf.hook.AnyErrorHook
import com.justai.jaicf.reactions.buttons

val MainScenario = Scenario {

    handle<AnyErrorHook> {
        logger.error("", exception)
        reactions.say("Sorry, I can't handle these technical difficulties, but you can try repeat your question!")
    }

    state("Main") {
        activators {
            regex("/start")
        }
        action {
            reactions.say("Hello! Please, select your language")
            reactions.buttons("Русский" to "/Ru", "English" to "/En")
        }
    }

    state("Ru") {
        action {
            routing.route("ru", targetState = "/Welcome")
        }
    }

    state("En") {
        action {
            routing.route("en", targetState = "/Welcome")
        }
    }

    fallback {
        when (val lang = LanguageDetectService.detectLanguage(request.input)) {
            null -> {
                reactions.say("Sorry, I can't get it! Please, select your language")
                reactions.buttons("Русский" to "/Ru", "English" to "/En")
            }
            else -> routing.route(lang.name, "/Welcome")
        }
    }
}
