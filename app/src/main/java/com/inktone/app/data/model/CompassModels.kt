package com.inktone.app.data.model

enum class Direction(val label: String) {
    N("北"), E("东"), S("南"), W("西")
}

fun directionOf(degree: Float): Direction = when {
    degree >= 315f || degree < 45f -> Direction.N
    degree < 135f -> Direction.E
    degree < 225f -> Direction.S
    else -> Direction.W
}

data class Floor(val name: String)

data class Photo(
    val fileName: String,
    val floorName: String,
    val direction: Direction,
    val name: String
)
