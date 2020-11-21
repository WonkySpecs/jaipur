package bots

import Action
import Bot
import Card
import PlayerGameView
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

// Returns the action from the first step which returns a valid function, or a random action if none are valid
open class AlgoBot(private vararg val steps: (v: PlayerGameView) -> Action?) : Bot {
    override fun getAction(view: PlayerGameView, validChecker: (a: Action) -> Boolean): Action {
        return steps.mapNotNull { it.invoke(view) }
            .firstOrNull { validChecker.invoke(it) } ?: generateSequence { somethingRandom(view) }
            .first { validChecker.invoke(it) }
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
class BigSpender : AlgoBot({ v -> sellBestIfAtLeast(v, 0) })

class BasicBot : AlgoBot(
    { v -> sellBestIfAtLeast(v, 4) },
    { v -> takeSingleIfAtLeast(v, 2) },
    { v -> takeCamelsIfAtLeast(v.market, min(v.herd - v.opponentHerd, 4)) },
)

fun sellBestIfAtLeast(view: PlayerGameView, min: Int): Action? {
    val toSell = biggestSeller(view)
    return if (toSell != null && toSell.second >= min) {
        Action.sell(view.hand.filter { it == toSell.first })
    } else {
        null
    }
}

// The card that, if all are sold, gives the most. Null if no cards. Will try to sell single ruby/gold/silver
fun biggestSeller(view: PlayerGameView): Pair<Card, Int>? {
    return view.hand.distinct()
        .map { type -> Pair(type, saleValue(type, view.hand.count { it == type }, view.goodsTokens)) }
        .maxByOrNull { it.second }
}

fun saleValue(type: Card, num: Int, stacks: Map<Card, Deque<Int>>): Int {
    val stack = stacks[type]
    return stack!!.take(min(num, stack.size)).sum()
}

fun takeSingleIfAtLeast(view: PlayerGameView, min: Int): Action? {
    val toTake = view.market.distinct().filter { it != Card.CAMEL }
        .map { Pair(it, marketCardValue(it, view)) }
        .sortedBy { it.second }
        .firstOrNull { it.second > min }?.first
    return if (toTake != null) {
        Action.take(toTake)
    } else {
        null
    }
}

fun marketCardValue(type: Card, view: PlayerGameView): Float {
    val stack = view.goodsTokens[type]!!
    val num = view.hand.count { it == type } + 1
    val goodsValue = stack.take(min(num, stack.size)).sum()
    val totalValue = when (num) {
        3 -> goodsValue + 2
        4 -> goodsValue + 5
        5 -> goodsValue + 9
        else -> goodsValue
    }
    return totalValue.toFloat() / num
}

fun takeCamelsIfAtLeast(market: List<Card>, min: Int): Action? {
    return if (market.count { it == Card.CAMEL } >= min) {
        Action.takeCamels()
    } else {
        null
    }
}
