package com.example.doceurhomeapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CategoryActivity : AppCompatActivity() {

    companion object {
        const val CATEGORY_NAME = "CATEGORY_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        setupCategoryClick(R.id.category_image_1, R.id.category_title_1, "Category1")
        setupCategoryClick(R.id.category_image_2, R.id.category_title_2, "Category2")
    }

    // Fonction pour configurer les clics sur les catégories
    private fun setupCategoryClick(imageId: Int, titleId: Int, categoryName: String) {
        findViewById<ImageView>(imageId).setOnClickListener { navigateToProducts(categoryName) }
        findViewById<TextView>(titleId).setOnClickListener { navigateToProducts(categoryName) }
    }

    // Fonction pour naviguer vers la page ProductsActivity avec la catégorie sélectionnée
    private fun navigateToProducts(categoryName: String) {
        val intent = Intent(this, ProductsActivity::class.java).apply {
            putExtra(CATEGORY_NAME, categoryName)
        }
        startActivity(intent)
        finish() // Optionnel pour éviter d'empiler trop d'activités
    }
}



