package com.example.doceurhomeapp

data class Product(
    var id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val category: String = "",// Ajout du champ category pour le filtrage
    var isFavorite: Boolean = false,
    val isBestseller: Boolean = false,
    val addedToBestsellers: Long = 0,
    val rating: Int = 0// Assurez-vous que ce champ existe
){
    constructor() : this("", "", "", 0.0, "", "", false, false, 0)
}

