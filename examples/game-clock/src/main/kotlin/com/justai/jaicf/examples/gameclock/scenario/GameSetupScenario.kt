package com.justai.jaicf.examples.gameclock.scenario

import com.justai.jaicf.builder.createModel
import com.justai.jaicf.examples.gameclock.GameController
import com.justai.jaicf.examples.gameclock.model.supportedColors
import com.justai.jaicf.model.scenario.Scenario

object GameSetupScenario : Scenario {

    const val state = "/setup"

    override val model = createModel {

        state(state) {

            append(GamersCountScenario(2, supportedColors.size))
            append(GamersColorsScenario)

            action {
                val game = GameController(context)

                if (game.gamers == null) {
                    reactions.say("Okay! Let's start a new game!")
                    reactions.go(GamersCountScenario.state, "next")
                } else {
                    reactions.go("next")
                }
            }

            state("next") {
                action {
                    val game = GameController(context)
                    game.gamers = game.gamers ?: context.result as Int

                    reactions.run {
                        say("${game.gamers} gamers! Cool! Now you have to choose a color for each of you!")
                        go("../${GamersColorsScenario.state}", "../complete")
                    }
                }
            }

            state("complete") {
                action {
                    reactions.goBack()
                }
            }
        }
    }
}