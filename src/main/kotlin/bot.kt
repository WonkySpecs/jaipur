import java.util.*

interface Bot {
    fun getAction(view: PlayerGameView, validChecker: (a: Action) -> Boolean): Action
}

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
        initialStacks.entries
            .map { Pair(it.key, ArrayDeque(ArrayList(it.value))) }
            .toMap()
    )
}
