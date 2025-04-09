package com.example.doceurhomeapp


import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class Bestsellers : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter
    private val productList = mutableListOf<Product>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bestsellers)

        // Initialisation des vues
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Initialisation de l'adapteur
        adapter = ProductAdapter(
            productList = productList,  // Utilisez productList au lieu de products
            onAddToCartClick = { product -> addToCart(product) },
            onProductImageClick = { product -> showProductDetails(product) },
            showBestsellerBadge = false
        )

        recyclerView.adapter = adapter
        loadBestsellers()
    }

    private fun loadBestsellers() {
        db.collection("bestsellers")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                productList.clear()
                val bestsellerIds = result.documents.map { it.id }

                if (bestsellerIds.isNotEmpty()) {
                    db.collection("products")
                        .whereIn("id", bestsellerIds)  // Utilisez "id" au lieu de "name" pour la correspondance
                        .get()
                        .addOnSuccessListener { productsResult ->
                            for (document in productsResult) {
                                val product = document.toObject(Product::class.java)
                                product?.let {
                                    productList.add(it.copy(isBestseller = true))  // Marquer comme bestseller
                                }
                            }
                            adapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Erreur de chargement des produits", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur de chargement des bestsellers", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addToCart(product: Product) {
        val userId = auth.currentUser?.uid ?: run {
            startActivity(Intent(this, sign_in::class.java))
            return
        }

        val cartItem = hashMapOf(
            "productId" to product.id,
            "name" to product.name,
            "price" to product.price,
            "imageUrl" to product.imageUrl,
            "quantity" to 1
        )

        db.collection("carts")
            .document(userId)
            .collection("items")
            .document(product.id)
            .set(cartItem)
            .addOnSuccessListener {
                Toast.makeText(this, "${product.name} ajouté au panier", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showProductDetails(product: Product) {
        val intent = Intent(this, DetailsActivity::class.java).apply {
            putExtra("product_id", product.id)
        }
        startActivity(intent)
    }
}