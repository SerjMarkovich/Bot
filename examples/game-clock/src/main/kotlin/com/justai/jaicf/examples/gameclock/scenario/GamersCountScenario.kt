package com.justai.jaicf.examples.gameclock.scenario

import com.justai.jaicf.builder.createModel
import com.justai.jaicf.channel.alexa.alexa
import com.justai.jaicf.channel.alexa.intent
import com.justai.jaicf.channel.googleactions.actions
import com.justai.jaicf.channel.googleactions.intent
import com.justai.jaicf.helpers.ssml.breakMs
import com.justai.jaicf.model.scenario.Scenario
import com.justai.jaicf.test.context.runInTest

class GamersCountScenario(private val min: Int, private val max: Int) : Scenario {

    companion object {
        const val state = "gamers"
    }

    override val model = createModel {

        state(state) {
            action {
                reactions.say("How many players participate in the game?")
            }

            state("count") {
                activators {
                    intent("GamersIntent")
                    catchAll()
                }

                action {
                    var gamers: Int? = null

                    alexa.intent {
                        gamers = activator.slots["gamers"]?.value?.toInt()
                    }

                    actions.intent {
                        gamers = (activator.slots["gamers"] as? Number)?.toInt()
                    }

                    runInTest {
                        gamers = gamers ?: getVar("gamers") as? Int
                    }

                    if (gamers == null) {
                        reactions.say("Sorry! I didn't get that!" +
                                "Please tell me a number of players ${breakMs(300)}" +
                                "Or say cancel - to finish.")

                    } else if (gamers!! < min || gamers!! > max) {
                        reactions.say("Sorry, but I support from $min to $max players!" +
                                "Please say a number of players in this range.")

                    } else {
                        reactions.goBack(gamers) // return the result back to the GameSetupScenario
                    }
                }
            }

        }
    }
}