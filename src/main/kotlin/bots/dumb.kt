package bots

import Action
import Bot
import Card
import PlayerGameView
import java.util.*
import kotlin.math.min
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
            var fallthrough = somethingRandom(view)
            while (!validChecker.invoke(fallthrough)) {
                fallthrough = somethingRandom(view)
            }
            return fallthrough
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

// The card that, if all are sold, gives the most
fun biggestSeller(view: PlayerGameView): Pair<Card, Int>? {
    return view.hand.distinct()
        .map { type -> Pair(type, saleValue(type, view.hand.count { it == type }, view.goodsTokens)) }
        .maxByOrNull { it.second }
}

fun saleValue(type: Card, num: Int, stacks: Map<Card, Deque<Int>>): Int {
    val stack = stacks[type]
    return stack!!.take(min(num, stack.size)).sum()
}

// Always tries to take the camels
class CamelLover : RandomFallthrough() {
    override fun actual(view: PlayerGameView): Action {
        return Action.takeCamels()
    }
}

// Always tries to sell the highest value good in hand
class BigSpender : RandomFallthrough() {
    override fun actual(view: PlayerGameView): Action {
        return sellIfOverThreshold(view, 0) ?: Action.invalid()
    }
}

fun sellIfOverThreshold(view: PlayerGameView, min: Int): Action? {
    val toSell = biggestSeller(view)
    return if (toSell != null && toSell.second >= min) {
        Action.sell(view.hand.filter { it == toSell.first })
    } else {
        null
    }
}

fun mostValuableSingle(view: PlayerGameView) {
    view.market
}
