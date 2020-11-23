package bots

import Action
import ActionType
import Bot
import Card
import PlayerGameView
import allValidActions
import kotlin.math.pow

class EvaluatorBot(private val config: EvaluatorConfig) : Bot {
    override fun description(): String {
        return "Evaluator: $config"
    }

    override fun getAction(view: PlayerGameView, validChecker: (a: Action) -> Boolean): Action {
        return allValidActions(view.hand, view.herd, view.market)
            .maxByOrNull { scoreAction(it, view) } ?: somethingRandom(view)
    }

    private fun scoreAction(action: Action, view: PlayerGameView): Double {
        val values = CachedValues(
            emptyMap(),
            view.market
                .filter { it != Card.CAMEL }
                .map { it to marketCardValue(it, view) }.toMap()
        )
        return when (action.type) {
            ActionType.TAKE_CAMELS -> camelValue(view, values)
            ActionType.TAKE_SINGLE -> singleValue(action.cardsFromMarket[0], view, values)
            ActionType.TAKE_SWAP -> swapValue(action.cardsFromHand, action.cardsFromMarket, view, values)
            ActionType.SELL -> sellValue(action.cardsFromHand, view)
        } * (config.actionPreferences[action.type] ?: 1.0)
    }

    fun camelValue(view: PlayerGameView, precomputed: CachedValues): Double {
        val numCamels = view.market.count { it == Card.CAMEL }
        val herdDiff = view.opponentHerd - view.herd
        val averageGoodWorth = precomputed.market
            .map { it.value }.sum() / (5 - numCamels)
        return numCamels.toDouble() - averageGoodWorth + herdDiff * config.camelJealousy
    }

    fun singleValue(type: Card, view: PlayerGameView, precomputed: CachedValues): Double {
        val fillingHandPenalty = view.hand.size.toDouble() * config.largeHandPreference / 2
        return precomputed.market[type]!! - fillingHandPenalty
    }

    fun sellValue(cards: List<Card>, view: PlayerGameView): Double {
        if (cards.isEmpty()) return 0.0 // All camels being 'sold'
        val emptyingHandBonus = view.hand.size + view.herd - config.largeHandPreference * 2
        return saleValue(cards.size, cards[0], view) + emptyingHandBonus
    }

    fun swapValue(put: List<Card>, take: List<Card>, view: PlayerGameView, precomputed: CachedValues): Double {
        return take.map { precomputed.market[it]!! }.sum() - sellValue(
            put.filter { it != Card.CAMEL },
            view
        ) - put.count { it == Card.CAMEL } / 2
    }
}

fun sigmoid(x: Double): Double {
    val ex = Math.E.pow(x)
    return ex / (ex + 1)
}

data class CachedValues(
    val hand: Map<Card, Double>,
    val market: Map<Card, Double>,
)

data class EvaluatorConfig(
    val camelJealousy: Double = 0.0,
    val largeHandPreference: Double = 0.5,
    val actionPreferences: Map<ActionType, Double> = ActionType.values().map { it to 1.0 }.toMap()
) {
    companion object {
        fun prefers(preferred: ActionType, factor: Double = 5.0) = EvaluatorConfig(
            actionPreferences = ActionType.values()
                .map {
                    it to if (it == preferred) {
                        factor
                    } else {
                        1.0
                    }
                }.toMap()
        )
    }
}

