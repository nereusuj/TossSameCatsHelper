package com.nereusuj.tosssamecantshelper

import android.graphics.Rect

data class CardResult(
    val rect: Rect,
    val groupId: Int // The number to display (1, 2, 3...)
)
