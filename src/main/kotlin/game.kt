import bots.*
import java.util.*

fun main() {
    val bots = ActionType.values()
        .map { Analyzer(EvaluatorBot(EvaluatorConfig.prefers(it))) }
    Tournament.against(bots, BasicBot(3, 4))
}

enum class Card {
    RUBY, GOLD, SILVER, SILK, SPICE, LEATHER, CAMEL
}

data class Player(val hand: MutableList<Card>, var herd: Int, var score: Int) {
    constructor() : this(mutableListOf(), 0, 0)
}

data class Game(
    val p1: Player,
    val p2: Player,
    var playerTurn: Int,
    var market: MutableList<Card>,
    val deck: Deque<Card>,
    val goodsTokens: Map<Card, Deque<Int>>,
    val setTokens: Map<Int, Deque<Int>>,
) {
    private var failedToRefill = false
    lateinit var p1View: PlayerGameView
    lateinit var p2View: PlayerGameView

    constructor(p1: Player, p2: Player, market: MutableList<Card>, deck: Deque<Card>)
            : this(p1, p2, 0, market, deck, newGoodsStacks(), newSetBonusStacks()) {
        p1View = PlayerGameView(p1.hand.toMutableList(), p1.herd, p2.herd, market.toList(), goodsTokens)
        p2View = PlayerGameView(p2.hand.toMutableList(), p2.herd, p1.herd, market.toList(), goodsTokens)
    }

    fun isOver(): Boolean {
        return when {
            failedToRefill -> {
                true
            }
            setTokens.count { it.value.isEmpty() } >= 3 -> {
                true
            }
            else -> {
                false
            }
        }
    }

    fun curPlayer() = if (playerTurn == 0) {
        p1
    } else {
        p2
    }

    fun isValid(action: Action): Boolean {
        return when (action.type) {
            ActionType.TAKE_CAMELS -> market.any { it == Card.CAMEL }
            ActionType.TAKE_SINGLE -> {
                action.cardsFromMarket.size == 1
                        && market.count { it == action.cardsFromMarket[0] } > 0
                        && curPlayer().hand.size < MAX_HAND_SIZE
                        && action.cardsFromMarket[0] != Card.CAMEL
            }
            ActionType.SELL -> canSell(action.cardsFromHand, curPlayer().hand)
            ActionType.TAKE_SWAP -> {
                val camelsPlaced = action.cardsFromHand.count { it == Card.CAMEL }

                return when {
                    action.cardsFromHand.size != action.cardsFromMarket.size
                            || action.cardsFromHand.size < 2
                            || action.cardsFromMarket.any { it == Card.CAMEL }
                            || camelsPlaced > curPlayer().herd
                            || action.cardsFromHand.any { fromHand -> action.cardsFromMarket.any { it == fromHand } }
                            || curPlayer().hand.size + camelsPlaced > MAX_HAND_SIZE
                    -> false
                    else -> true
                }
            }
        }
    }

    fun execute(action: Action) {
        when (action.type) {
            ActionType.TAKE_CAMELS -> takeCamels()
            ActionType.TAKE_SINGLE -> takeSingle(action.cardsFromMarket[0])
            ActionType.SELL -> sell(action.cardsFromHand)
            ActionType.TAKE_SWAP -> swap(action.cardsFromHand, action.cardsFromMarket)
        }
        syncPublicViews()
        playerTurn = (playerTurn + 1) % 2
    }

    private fun takeCamels() {
        val num = market.count { it == Card.CAMEL }
        curPlayer().herd += num
        market = market.filter { it != Card.CAMEL } as MutableList<Card>
        if (deck.size < num) {
            failedToRefill = true
        } else {
            repeat(num) { market.add(deck.pop()) }
        }
    }

    private fun takeSingle(type: Card) {
        market.remove(type)
        addToHand(type)
        if (deck.size == 0) {
            failedToRefill = true
        } else {
            market.add(deck.pop())
        }
    }

    private fun sell(cards: List<Card>) {
        val type = cards[0]
        val tokensForGood = goodsTokens[type] ?: error("Card type '$type' not in goodsTokens")
        var num = cards.size
        when (num) {
            3, 4, 5 -> {
                val bonusStack = setTokens[num]!!
                if (bonusStack.isNotEmpty()) {
                    curPlayer().score += bonusStack.pop()
                }
            }
            else -> {}
        }

        while (num > 0 && tokensForGood.isNotEmpty()) {
            curPlayer().score += tokensForGood.pop()
            p1View.goodsTokens[type]!!.pop()
            p2View.goodsTokens[type]!!.pop()
            num--
        }
        for (c in cards) {
            removeFromHand(c)
        }
    }

    private fun swap(put: List<Card>, take: List<Card>) {
        for (c in take) {
            market.remove(c)
            addToHand(c)
        }

        for (c in put) {
            market.add(c)
            removeFromHand(c)
        }
    }

    private fun addToHand(card: Card) {
        curPlayer().hand.add(card)
        if (playerTurn == 0) {
            p1View.hand.add(card)
            p2View.opponentHand.add(card)
        } else {
            p2View.hand.add(card)
            p1View.opponentHand.add(card)
        }
    }

    private fun removeFromHand(card: Card) {
        curPlayer().hand.remove(card)
        if (playerTurn == 0) {
            p1View.hand.remove(card)
            if (card in p2View.opponentHand) {
                p2View.opponentHand.remove(card)
            } else {
                p2View.opponentHand.remove(null)
            }
        } else {
            p2View.hand.remove(card)
            if (card in p1View.opponentHand) {
                p1View.opponentHand.remove(card)
            } else {
                p1View.opponentHand.remove(null)
            }
        }
    }

    fun awardCamelToken() {
        if (p1.herd > p2.herd) {
            p1.score += 5
        } else if (p2.herd > p1.herd) {
            p2.score += 5
        }
    }

    private fun syncPublicViews() {
        p1View.market = market
        p2View.market = market
        p1View.herd = p1.herd
        p1View.opponentHerd = p2.herd
        p2View.herd = p2.herd
        p2View.opponentHerd = p1.herd
    }

    fun printState() {
        println("P1: ${p1.hand}, ${p1.herd} camels, ${p1.score} points")
        println("P2: ${p2.hand}, ${p2.herd} camels, ${p2.score} points")
        println("Market: $market")
    }
}

enum class ActionType {
    TAKE_SINGLE, TAKE_CAMELS, TAKE_SWAP, SELL
}

data class Action(
    val type: ActionType,
    val cardsFromHand: List<Card>,
    val cardsFromMarket: List<Card>,
) {
    companion object {
        fun takeCamels() = Action(ActionType.TAKE_CAMELS, emptyList(), emptyList())
        fun sell(cards: List<Card>) = Action(ActionType.SELL, cards, emptyList())
        fun take(card: Card) = Action(ActionType.TAKE_SINGLE, emptyList(), listOf(card))
        fun swap(hand: List<Card>, market: List<Card>) = Action(ActionType.TAKE_SWAP, hand, market)
        fun invalid() = Action(ActionType.TAKE_SINGLE, emptyList(), listOf(Card.CAMEL))
    }
}

const val MAX_HAND_SIZE: Int = 7

fun canSell(cards: List<Card>, hand: List<Card>): Boolean {
    if (cards.distinct().size != 1) {
        return false
    }

    if (cards[0] == Card.CAMEL) {
        return false
    }

    if (hand.count { it == cards[0] } < cards.size) {
        return false
    }

    return when (cards[0]) {
        Card.RUBY, Card.GOLD, Card.SILVER -> cards.size >= 2
        else -> true
    }
}
