package net.ccbluex.liquidbounce.utils.render.animations

enum class Direction {
    FORWARDS,
    BACKWARDS;

    fun opposite(): Direction = if (this == FORWARDS) BACKWARDS else FORWARDS

    fun forwards(): Boolean = this == FORWARDS
}
