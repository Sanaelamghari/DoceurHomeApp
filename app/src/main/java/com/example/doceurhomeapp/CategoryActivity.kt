package com.example.doceurhomeapp

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doceurhomeapp.databinding.ActivityCategoryBinding
import com.google.firebase.firestore.FirebaseFirestore

class CategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryBinding
    private lateinit var categoriesAdapter: CategoriesAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private var allCategories = listOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearch()
        setupBottomNavigation() // Ajout de la configuration de la navigation
        loadCategories()
    }
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    navigateTo(MainActivity::class.java)
                    finish()
                    true
                }
                R.id.nav_list -> {
                    // Déjà sur la page des catégories
                    true
                }
                R.id.nav_cart -> {
                    navigateTo(MycartActivity::class.java)
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    navigateTo(Favorites::class.java)
                    finish()
                    true
                }
                else -> false
            }.also { result ->
                if (result) overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }

        // Marquer l'item actif
        binding.bottomNavigation.selectedItemId = R.id.nav_list
    }

    private fun <T : Activity> navigateTo(activityClass: Class<T>) {
        val intent = Intent(this, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun setupRecyclerView() {
        categoriesAdapter = CategoriesAdapter { categoryName ->
            navigateToProducts(categoryName)
        }

        binding.rvCategories.apply {
            layoutManager = GridLayoutManager(this@CategoryActivity, 2) // 2 colonnes
            adapter = categoriesAdapter
            // Ajout du padding top pour la première colonne
            setPadding(0, resources.getDimensionPixelSize(R.dimen.grid_top_padding), 0, 0)
            clipToPadding = false

            // Remplacement de GridSpacingItemDecoration par notre nouveau ItemDecoration
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    val position = parent.getChildAdapterPosition(view)
                    val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)

                    // Appliquer un margin différent pour les éléments de la colonne de droite
                    if (position % 2 == 1) { // éléments impairs (colonne de droite)
                        outRect.top = resources.getDimensionPixelSize(R.dimen.grid_offset) // décalage vertical
                    }

                    // Espacement horizontal et vertical de base
                    outRect.left = spacing / 2
                    outRect.right = spacing / 2
                    outRect.bottom = spacing
                }
            })
        }
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                filterCategories(s.toString())
            }
        })
    }

    private fun filterCategories(query: String) {
        if (query.isEmpty()) {
            categoriesAdapter.submitList(allCategories)
            return
        }

        val filtered = allCategories.filter {
            it.name.contains(query, ignoreCase = true)
        }
        categoriesAdapter.submitList(filtered)

        if (filtered.isEmpty()) {
            Toast.makeText(this, "Aucune catégorie trouvée", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCategories() {
        firestore.collection("categories")
            .get()
            .addOnSuccessListener { result ->
                allCategories = result.map { document ->
                    Category(
                        name = document.getString("name") ?: "",
                        imageUrl = document.getString("imageUrl") ?: ""
                    )
                }
                categoriesAdapter.submitList(allCategories)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Erreur de chargement: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun navigateToProducts(categoryName: String) {
        val intent = Intent(this, ProductsActivity::class.java).apply {
            putExtra(CATEGORY_NAME, categoryName)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    companion object {
        const val CATEGORY_NAME = "CATEGORY_NAME"
    }
}