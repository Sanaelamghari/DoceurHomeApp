package com.example.doceurhomeapp

data class Panier(
    val idUser: String = "",
    val produits: List<Product> = emptyList()
)
