package com.example.doceurhomeapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doceurhomeapp.databinding.ActivityBestsellersBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class Bestsellers : AppCompatActivity() {

    // Solution 1: Utilisation de lateinit + vérification
    private lateinit var bestsellersRecyclerView: RecyclerView

    // Solution alternative 2: Initialisation immédiate (plus sûre)
    // private var bestsellersRecyclerView: RecyclerView? = null

    private lateinit var adapter: ProductAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val bestsellersList = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var binding = ActivityBestsellersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bestsellersRecyclerView.apply {
            layoutManager = GridLayoutManager(this@Bestsellers, 2)

        }
    }

    private fun loadBestsellers() {
        db.collection("bestsellers")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val products = documents.map { doc ->
                    Product(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: ""
                    )
                }
                adapter.updateList(products)
            }
    }
    private fun showProductDetails(product: Product) {
        startActivity(Intent(this, DetailsActivity::class.java).apply {
            putExtra("product_id", product.id)
        })
    }

    private fun addToCart(product: Product) {
        // Implémentation existante
    }
}