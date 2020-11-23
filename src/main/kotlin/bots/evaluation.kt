package bots

import Card
import PlayerGameView
import kotlin.math.min

fun marketCardValue(type: Card, view: PlayerGameView): Double {
    val num = view.hand.count { it == type } + 1
    return saleValue(num, type, view).toDouble() / num
}

fun handCardValue(type: Card, view: PlayerGameView): Double {
    val num = view.hand.count { it == type }
    return saleValue(num, type, view).toDouble() / num
}

fun saleValue(num: Int, type: Card, view: PlayerGameView): Int {
    val stack = view.goodsTokens[type]!!
    val goodsValue = stack.take(min(num, stack.size)).sum()
    return when (num) {
        3 -> goodsValue + 2
        4 -> goodsValue + 5
        5 -> goodsValue + 9
        else -> goodsValue
    }
}
