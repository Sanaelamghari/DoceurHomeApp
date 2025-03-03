package com.example.doceurhomeapp

import android.os.Parcelable

data class CartItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    var quantity: Int = 1
)


