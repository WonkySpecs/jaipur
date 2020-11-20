import java.util.*
import kotlin.random.Random

data class PlayerGameView(
    val hand: MutableList<Card>,
    var herd: Int,
    val opponentHand: MutableList<Card?>,
    var opponentHerd: Int,
    var market: MutableList<Card>,
    val goodsTokens: Map<Card, Deque<Int>>
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
        initialStacks
    )
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