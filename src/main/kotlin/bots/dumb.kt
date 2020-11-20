package bots

import Action
import Bot
import Card
import PlayerGameView
import java.util.*
import kotlin.math.min
import kotlin.random.Random

// Returns the action from the first step which returns a valid function, or a random action if none are valid
open class AlgoBot(private vararg val steps: (v: PlayerGameView) -> Action?) : Bot {
    override fun getAction(view: PlayerGameView, validChecker: (a: Action) -> Boolean): Action {
        return steps.mapNotNull { it.invoke(view) }
            .firstOrNull { validChecker.invoke(it) } ?: run {
            var fallback = somethingRandom(view)
            while (!validChecker.invoke(fallback)) {
                fallback = somethingRandom(view)
            }
            return fallback
        }
    }
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
class RandomBot : AlgoBot()
// Always tries to take the camels
class CamelLover : AlgoBot({ Action.takeCamels() })
// Always tries to sell the highest value good in hand
class BigSpender : AlgoBot({ v -> sellIfOverThreshold(v, 0) })

fun sellIfOverThreshold(view: PlayerGameView, min: Int): Action? {
    val toSell = biggestSeller(view)
    return if (toSell != null && toSell.second >= min) {
        Action.sell(view.hand.filter { it == toSell.first })
    } else {
        null
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
