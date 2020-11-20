package bots

import Action
import Bot
import PlayerGameView
import kotlin.random.Random

class RandomBot : Bot {
    override fun getAction(view: PlayerGameView, validChecker: (a: Action) -> Boolean): Action {
        return somethingRandom(view)
    }
}

// Bot which tries to work out an action, but falls back to a random action if it's not valid
abstract class RandomFallthrough : Bot {
    override fun getAction(view: PlayerGameView, validChecker: (a: Action) -> Boolean): Action {
        val attempt = actual(view)
        return if (validChecker.invoke(attempt)) {
            attempt
        } else {
            somethingRandom(view)
        }
    }

    abstract fun actual(view: PlayerGameView): Action
}

fun somethingRandom(view: PlayerGameView): Action {
    return when (Random.nextInt(4)) {
        0 -> Action.takeCamels()
        1 -> Action.sell(
            view.hand.filter { Random.nextBoolean() }
        )
        2 -> Action.take(view.market.random())
        else -> Action.swap(
            view.hand.filter { Random.nextBoolean() },
            view.market.filter { Random.nextBoolean() },
        )
    }
}
