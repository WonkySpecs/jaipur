import kotlin.random.Random

data class PlayerGameView(
    val hand: MutableList<Card>,
    var herd: Int,
    val opponentHand: MutableList<Card?>,
    var opponentHerd: Int,
    var market: MutableList<Card>,
) {
    constructor(hand: List<Card>, herd: Int, opponentHerd: Int, market: List<Card>)
            : this(
        hand as MutableList<Card>,
        herd,
        MutableList(5 - opponentHerd) { null },
        opponentHerd,
        market as MutableList<Card>
    )
}

fun somethingRandom(view: PlayerGameView): Action {
    val num = Random.nextInt(4)
    return when (num) {
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