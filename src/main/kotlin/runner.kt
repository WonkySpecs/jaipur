import java.util.*

class Tournament(private val participants: List<Bot>) {
    private val ITERATIONS = 50

    fun run() {
        for (i in participants.indices) {
            for (j in (i + 1) until participants.size) {
                val results = IntRange(1, ITERATIONS)
                    .map { runGame(participants[i], participants[j]) }
                val p1Wins = results.count { it.p1Score > it.p2Score }
                val p2Wins = results.count { it.p1Score < it.p2Score }
                println("${participants[i].javaClass} $p1Wins - $p2Wins ${participants[j].javaClass} (${ITERATIONS - p1Wins - p2Wins} draws)")
            }
        }
    }
}

data class GameResult(val p1Score: Int, val p2Score: Int)

fun runGame(p1: Bot, p2: Bot): GameResult {
    val game = initGame()

    fun getAction(game: Game): Action {
        return if (game.playerTurn == 0) {
            p1.getAction(game.p1View) { a -> game.isValid(a) }
        } else {
            p2.getAction(game.p2View) { a -> game.isValid(a) }
        }
    }

    while (!game.isOver()) {
        var action = getAction(game)
        while (!game.isValid(action)) {
            action = getAction(game)
        }
        game.execute(action)
    }
    game.awardCamelToken()
    return GameResult(game.p1.score, game.p2.score)
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
