package com.example.doceurhomeapp

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class Favorites : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: FavoritesAdapter
    private val favoritesList = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        // AJOUT: Configuration de la Toolbar avec flèche de retour
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Initialisation des vues existantes
        recyclerView = findViewById(R.id.favoritesRecyclerView)
        emptyStateText = findViewById(R.id.emptyStateText)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Configuration du RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Configuration de la navigation
        setupBottomNavigation()
        setupAuthListener()
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    navigateTo(MainActivity::class.java)
                    finish()
                    true
                }
                R.id.nav_list -> {
                    navigateTo(CategoryActivity::class.java)
                    finish()
                    true
                }
                R.id.nav_cart -> {
                    navigateTo(MycartActivity::class.java)
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    // Déjà sur la page des favoris
                    true
                }
                else -> false
            }.also { result ->
                if (result) overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_profile
    }

    private fun <T : Activity> navigateTo(activityClass: Class<T>) {
        val intent = Intent(this, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun setupAuthListener() {
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                loadFavorites()
            } else {
                showLoginMessage()
            }
        }
    }

    private fun loadFavorites() {
        val userId = auth.currentUser?.uid ?: run {
            showLoginMessage()
            return
        }

        db.collection("userFavorites")
            .document(userId)
            .collection("products")
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    showEmptyState()
                } else {
                    val productIds = docs.map { it.id }
                    fetchProductDetails(productIds)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Favorites", "Error loading favorites", e)
                showError("Erreur de chargement des favoris")
            }
    }

    private fun fetchProductDetails(productIds: List<String>) {
        db.collection("products")
            .whereIn(FieldPath.documentId(), productIds)
            .get()
            .addOnSuccessListener { querySnapshot ->
                favoritesList.clear()
                for (document in querySnapshot) {
                    try {
                        val price = when (val priceData = document["price"]) {
                            is Double -> priceData
                            is Long -> priceData.toDouble()
                            is String -> priceData.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }

                        val product = Product(
                            id = document.id,
                            name = document.getString("name") ?: "",
                            price = price,
                            imageUrl = document.getString("imageUrl") ?: "",
                            isFavorite = true
                        )
                        favoritesList.add(product)
                    } catch (e: Exception) {
                        Log.e("Favorites", "Error parsing product ${document.id}", e)
                        favoritesList.add(Product(
                            id = document.id,
                            name = "Produit indisponible",
                            price = 0.0,
                            imageUrl = "",
                            isFavorite = true
                        ))
                    }
                }
                if (favoritesList.isEmpty()) {
                    showEmptyState()
                } else {
                    setupRecyclerView(favoritesList)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Favorites", "Error fetching product details", e)
                showError("Erreur de chargement des détails")
            }
    }

    private fun setupRecyclerView(products: List<Product>) {
        emptyStateText.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE

        adapter = FavoritesAdapter(products.toMutableList(),
            onFavoriteClick = { product ->
                removeFromFavorites(product)
            },
            onAddToCartClick = { product ->
                addToCart(product)
            }
        )

        recyclerView.adapter = adapter
    }

    private fun removeFromFavorites(product: Product) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("userFavorites")
            .document(userId)
            .collection("products")
            .document(product.id)
            .delete()
            .addOnSuccessListener {
                favoritesList.removeAll { it.id == product.id }
                adapter.updateList(favoritesList)

                if (favoritesList.isEmpty()) showEmptyState()
                Toast.makeText(this, "Retiré des favoris", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("Favorites", "Error removing favorite", e)
                showError("Erreur lors de la suppression")
            }
    }

    private fun addToCart(product: Product) {
        Toast.makeText(this, "${product.name} ajouté au panier", Toast.LENGTH_SHORT).show()
    }

    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        emptyStateText.visibility = View.VISIBLE
    }

    private fun showLoginMessage() {
        Toast.makeText(this, "Veuillez vous connecter pour voir vos favoris", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}