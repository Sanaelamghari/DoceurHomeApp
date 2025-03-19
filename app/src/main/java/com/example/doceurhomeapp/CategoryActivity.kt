package com.example.doceurhomeapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class CategoryActivity : AppCompatActivity() {

    // Variables pour les composants UI
    private lateinit var rvCategories: RecyclerView
    private lateinit var categoriesAdapter: CategoriesAdapter

    // Référence à Firestore
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        // Initialisation du RecyclerView
        rvCategories = findViewById(R.id.rvCategories)
        rvCategories.layoutManager = GridLayoutManager(this, 2) // 2 colonnes

        // Initialisation de l'adaptateur
        categoriesAdapter = CategoriesAdapter { categoryName ->
            navigateToProducts(categoryName)
        }
        rvCategories.adapter = categoriesAdapter

        // Charger les catégories depuis Firestore
        loadCategories()
    }

    // Charger les catégories depuis Firestore
    private fun loadCategories() {
        firestore.collection("categories")
            .get()
            .addOnSuccessListener { result ->
                val categories = mutableListOf<Category>()
                for (document in result) {
                    val name = document.getString("name") ?: ""
                    val imageUrl = document.getString("imageUrl") ?: ""
                    categories.add(Category(name, imageUrl))
                }
                categoriesAdapter.submitList(categories)
                Log.d("Firestore", "Catégories chargées: $categories")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erreur chargement catégories: ${e.message}")
            }
    }

    // Fonction pour naviguer vers la page ProductsActivity avec la catégorie sélectionnée
    private fun navigateToProducts(categoryName: String) {
        val intent = Intent(this, ProductsActivity::class.java).apply {
            putExtra(CATEGORY_NAME, categoryName)
        }
        startActivity(intent)
    }

    companion object {
        const val CATEGORY_NAME = "CATEGORY_NAME"
    }
}