import java.util.*
import kotlin.math.min

interface Bot {
    fun getAction(view: PlayerGameView, validChecker: (a: Action) -> Boolean): Action
    fun description(): String = javaClass.toString()
}

data class PlayerGameView(
    val hand: MutableList<Card>,
    var herd: Int,
    val opponentHand: MutableList<Card?>,
    var opponentHerd: Int,
    var market: MutableList<Card>,
    val goodsTokens: Map<Card, Deque<Int>>,
) {
    constructor(
        hand: List<Card>,
        herd: Int,
        opponentHerd: Int,
        market: List<Card>,
        initialStacks: Map<Card, Deque<Int>>
    ) : this(
        hand as MutableList<Card>,
        herd,
        MutableList(5 - opponentHerd) { null },
        opponentHerd,
        market as MutableList<Card>,
        initialStacks.entries
            .map { Pair(it.key, ArrayDeque(ArrayList(it.value))) }
            .toMap()
    )
}

fun allValidActions(hand: List<Card>, herd: Int, market: List<Card>): Sequence<Action> {
    return sequence {
        if (market.any { it == Card.CAMEL }) {
            yield(Action.takeCamels())
        }

        if (hand.size < 7) {
            market.filter { it != Card.CAMEL }
                .forEach { yield(Action.take(it)) }
        }

        validSales(hand).forEach { yield(Action.sell(it)) }
        validSwaps(hand, herd, market).forEach { yield(Action.swap(it.first, it.second)) }
    }
}

fun validSales(hand: List<Card>): Sequence<List<Card>> {
    return sequence {
        hand.distinct()
            .flatMap { cardType ->
                run {
                    val minForSale = when (cardType) {
                        Card.RUBY, Card.GOLD, Card.SILVER -> 2
                        else -> 1
                    }
                    (minForSale..hand.count { it == cardType }).map { List(it) { cardType } }
                }
            }
            .forEach { yield(it) }
    }
}

fun validSwaps(hand: List<Card>, herd: Int, market: List<Card>): Sequence<Pair<List<Card>, List<Card>>> {
    val marketCombos = goodsCombinations(market)
        .filter { it.size > 1 }
        .groupBy { it.size }

    val maxToTake = market.count { it != Card.CAMEL }
    val handCombos = goodsCombinations(hand)
        .flatMap { handCards ->
            run {
                val maxCamels = min(min(herd, maxToTake - handCards.size), MAX_HAND_SIZE - hand.size)
                val camels = (0..maxCamels).map { List(it) { Card.CAMEL } }
                camels.map { it + handCards }
            }
        }
        .filter { it.size > 1 }

    return sequence {
        for (toPut in handCombos) {
            for (toTake in marketCombos[toPut.size] ?: emptyList()) {
                if (toPut.distinct().all { handCard ->
                        toTake.none { handCard == it }
                    }) {
                    yield(toPut to toTake)
                }
            }
        }
    }
}

fun goodsCombinations(cards: List<Card>): List<List<Card>> {
    if (cards.isEmpty()) {
        return listOf(emptyList())
    }

    val combos = mutableListOf<List<Card>>()
    val restCombos = goodsCombinations(cards.subList(1, cards.size))
    combos.addAll(restCombos)
    if (cards[0] != Card.CAMEL) {
        combos.addAll(restCombos.map { listOf(cards[0]) + it })
    }
    return combos.distinct()
}

class Analyzer(private val inner: Bot) : Bot {
    private val actionsTaken = mutableListOf<Action>()
    override fun getAction(view: PlayerGameView, validChecker: (a: Action) -> Boolean): Action {
        val action = inner.getAction(view, validChecker)
        actionsTaken.add(action)
        return action
    }

    fun analysis(): String {
        return actionsTaken.groupBy { it.type }
            .entries.sortedByDescending { it.value.size }
            .joinToString("\n") { "${it.key}: ${it.value.size}" }
    }

    override fun description(): String {
        return inner.description()
    }
}
