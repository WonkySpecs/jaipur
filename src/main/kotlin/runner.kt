import java.util.*

class Tournament(private val participants: List<Bot>, private val gamesPerMatch: Int) {
    private val results: MutableMap<Bot, MutableList<GameResult>> = mutableMapOf()

    fun run() {
        for (i in participants.indices) {
            for (j in (i + 1) until participants.size) {
                val p1 = participants[i]
                val p2 = participants[j]
                val matchResults = IntRange(1, gamesPerMatch)
                    .map { runGame(p1, p2) }
                results.getOrPut(p1, { mutableListOf() })
                    .addAll(matchResults)
                results.getOrPut(p2, { mutableListOf() })
                    .addAll(matchResults.map { GameResult(it.p2Score, it.p1Score) })
            }
        }
    }

    fun printWinners(num: Int) {
        println("Top $num winners:")
        results.map { Pair(it.key, calculateScore(it.value)) }
            .sortedByDescending { it.second.first }
            .take(num)
            .forEach { println("${it.first.description()}: ${it.second}") }
    }
}

// (Wins, draws, losses)
fun calculateScore(results: List<GameResult>): Triple<Int, Int, Int> {
    return Triple(
        results.count { it.p1Score > it.p2Score },
        results.count { it.p1Score == it.p2Score },
        results.count { it.p1Score < it.p2Score },
    )
}

data class GameResult(val p1Score: Int, val p2Score: Int)

private fun runGame(p1: Bot, p2: Bot): GameResult {
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
            println(
                "Warning: bot tried to do invalid action: $action\n" +
                        "Market: ${game.market}, hand: ${game.curPlayer().hand}, ${game.curPlayer().herd} camels"
            )
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

