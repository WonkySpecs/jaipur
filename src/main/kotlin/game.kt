import java.util.*

fun main() {
    val game = initGame()
    while (!game.isOver()) {
        var action = getAction(game)
        while (!game.isValid(action)) {
            action = getAction(game)
        }
        game.printState()
        println("P${game.playerTurn + 1} executing: $action")
        game.execute(action)
    }
    game.awardCamelToken()
    val message = when {
        game.p1.score == game.p2.score -> "Draw!"
        game.p1.score > game.p2.score -> "Player 1 wins!"
        else -> "Player 2 wins!"
    }
    println("$message ${game.p1.score} - ${game.p2.score}")
}

fun initGame(): Game {
    val p1 = Player()
    val p2 = Player()
    val deck = newDeck();
    val market = mutableListOf(Card.CAMEL, Card.CAMEL, Card.CAMEL)
    market.add(deck.pop())
    market.add(deck.pop())
    dealHand(deck, p1)
    dealHand(deck, p2)
    return Game(p1, p2, market, deck)
}

fun dealHand(deck: Deque<Card>, player: Player) {
    repeat(5) {
        val card = deck.pop()
        if (card == Card.CAMEL) {
            player.herd++
        } else {
            player.hand.add(card)
        }
    }
}

enum class Card {
    RUBY, GOLD, SILVER, SILK, SPICE, LEATHER, CAMEL
}

fun newGoodsStacks(): Map<Card, Deque<Int>> {
    return hashMapOf<Card, Deque<Int>>(
        Card.RUBY to ArrayDeque(listOf(7, 7, 5, 5, 5)),
        Card.GOLD to ArrayDeque(listOf(6, 6, 5, 5, 5)),
        Card.SILVER to ArrayDeque(listOf(5, 5, 5, 5, 5)),
        Card.SILK to ArrayDeque(listOf(5, 3, 3, 2, 2, 1, 1)),
        Card.SPICE to ArrayDeque(listOf(5, 3, 3, 2, 2, 1, 1)),
        Card.LEATHER to ArrayDeque(listOf(4, 3, 2, 1, 1, 1, 1, 1, 1)),
    )
}

fun newSetBonusStacks(): Map<Int, Deque<Int>> {
    return hashMapOf<Int, Deque<Int>>(
        3 to ArrayDeque(listOf(1, 1, 2, 2, 2, 3, 3).shuffled()),
        4 to ArrayDeque(listOf(4, 4, 5, 5, 6, 6).shuffled()),
        5 to ArrayDeque(listOf(8, 8, 9, 10, 10).shuffled()),
    )
}

fun newDeck(): Deque<Card> {
    return ArrayDeque(
        hashMapOf(
            Card.RUBY to 6,
            Card.GOLD to 6,
            Card.SILVER to 6,
            Card.SILK to 8,
            Card.SPICE to 8,
            Card.LEATHER to 10,
            Card.CAMEL to 8
        ).flatMap {
            generateSequence { it.key }.take(it.value).toMutableList()
        }.shuffled()
    )
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
        p1View = PlayerGameView(p1.hand, p1.herd, p2.herd, market)
        p2View = PlayerGameView(p2.hand, p2.herd, p1.herd, market)
    }

    fun isOver() = failedToRefill || setTokens.count { it.value.isEmpty() } >= 3
    private fun cur() = if (playerTurn == 0) {
        p1
    } else {
        p2
    }

    fun isValid(action: Action): Boolean {
        return when (action.type) {
            ActionType.TAKE_CAMELS -> market.count { it == Card.CAMEL } > 0
            ActionType.TAKE_SINGLE -> {
                action.cardsFromMarket.size == 1
                        && market.count { it == action.cardsFromMarket[0] } > 0
                        && cur().hand.size < MAX_HAND_SIZE
            }
            ActionType.SELL -> canSell(action.cardsFromHand, cur().hand)
            ActionType.TAKE_SWAP -> {
                val camelsPlaced = action.cardsFromHand.count { it == Card.CAMEL }

                return when {
                    action.cardsFromHand.size != action.cardsFromMarket.size
                            || action.cardsFromHand.isEmpty()
                            || action.cardsFromMarket.any { it == Card.CAMEL }
                            || camelsPlaced > cur().herd
                            || action.cardsFromHand.any { fromHand -> action.cardsFromMarket.any { it == fromHand } }
                            || cur().hand.size + camelsPlaced > MAX_HAND_SIZE
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
        cur().herd += num
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
            3, 4, 5 -> cur().score += setTokens[num]?.pop()!! // Assume set bonus stacks don't run out
        }
        while (num > 0 && tokensForGood.isNotEmpty()) {
            cur().score += tokensForGood.pop()
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
        cur().hand.add(card)
        if (playerTurn == 0) {
            p1View.hand.add(card)
            p2View.opponentHand.add(card)
        } else {
            p2View.hand.add(card)
            p1View.opponentHand.add(card)
        }
    }

    private fun removeFromHand(card: Card) {
        cur().hand.remove(card)
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
        println("P1: ${p1.hand}, ${p1.herd} camels")
        println("P2: ${p2.hand}, ${p2.herd} camels")
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
    }
}

fun getAction(game: Game): Action {
    val view = if (game.playerTurn == 0) {
        game.p1View
    } else {
        game.p2View
    }
    return somethingRandom(view)
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
