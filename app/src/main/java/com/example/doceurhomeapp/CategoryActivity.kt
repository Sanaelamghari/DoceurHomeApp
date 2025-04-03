package com.example.doceurhomeapp

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
        loadCategories()
    }

    private fun setupRecyclerView() {
        categoriesAdapter = CategoriesAdapter { categoryName ->
            navigateToProducts(categoryName)
        }

        binding.rvCategories.apply {
            layoutManager = GridLayoutManager(this@CategoryActivity, 2) // 2 colonnes
            adapter = categoriesAdapter
            addItemDecoration(GridSpacingItemDecoration(2, 16, true))
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

        binding.allCategoriesChip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.searchInput.text?.clear()
                categoriesAdapter.submitList(allCategories)
            }
        }
    }

    private fun filterCategories(query: String) {
        if (query.isEmpty()) {
            categoriesAdapter.submitList(allCategories)
            binding.allCategoriesChip.isChecked = true
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
        binding.progressBar.visibility = View.VISIBLE

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
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Erreur de chargement: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
                binding.progressBar.visibility = View.GONE
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

// Extension pour l'espacement entre les items
class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount

        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount
            if (position < spanCount) outRect.top = spacing
            outRect.bottom = spacing
        } else {
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) outRect.top = spacing
        }
    }
}