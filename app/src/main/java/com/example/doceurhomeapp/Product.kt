package com.example.doceurhomeapp

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: String = "",
    val imageUrl: String = "",
    val category: String = "",// Ajout du champ category pour le filtrage
    var isFavorite: Boolean = false,
    val rating: Int = 0// Assurez-vous que ce champ existe
)/*{
    constructor() : this("", "", "", 0.0, "", 1, false) // CONSTRUCTEUR VIDE
}*/

