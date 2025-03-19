package com.example.doceurhomeapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProductsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val productList = mutableListOf<Product>()

    private var cartCounter = 0
    private var selectedCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products2)

        // Récupérer la catégorie sélectionnée depuis l'Intent
        selectedCategory = intent.getStringExtra("CATEGORY_NAME")
        if (selectedCategory == null) {
            Toast.makeText(this, "Catégorie non trouvée", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        recyclerView = findViewById(R.id.recyclerViewProducts)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Initialisation de l'adaptateur avec un clic sur l'image du produit
        productAdapter = ProductAdapter(productList,
            onAddToCartClick = { product -> addToCart(product) },
            onFavoriteClick = { product -> addToFavorites(product) },
            onProductImageClick = { product -> navigateToDetails(product) } // Nouveau callback pour le clic sur l'image
        )

        recyclerView.adapter = productAdapter

        val cartIcon = findViewById<ImageView>(R.id.cart)
        cartIcon.setOnClickListener {
            startActivity(Intent(this, MycartActivity::class.java))
        }

        fetchProductsFromFirestore()
        fetchCartCount()
    }

    private fun addToFavorites(product: Product) {
        // Logique des favoris (peut être ajoutée plus tard)
        Toast.makeText(this, "${product.name} ajouté aux favoris", Toast.LENGTH_SHORT).show()
    }

    private fun fetchProductsFromFirestore() {
        firestore.collection("products")
            .whereEqualTo("category", selectedCategory) // Filtrer par catégorie
            .get()
            .addOnSuccessListener { documents ->
                productList.clear()
                for (document in documents) {
                    val product = document.toObject(Product::class.java).copy(id = document.id)
                    productList.add(product)
                }
                productAdapter.notifyDataSetChanged()
                Log.d("ProductsActivity", "✅ Produits chargés avec succès : ${productList.size}")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erreur de chargement", Toast.LENGTH_SHORT).show()
                Log.e("ProductsActivity", "❌ Erreur lors du chargement des produits", it)
            }
    }

    private fun fetchCartCount() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val cartRef = firestore.collection("paniers").document(userId)

        cartRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val cartItems = document.get("products") as? List<Map<String, Any>> ?: listOf()
                cartCounter = cartItems.sumOf { (it["quantity"] as Long).toInt() }
                updateCartCounter()
            }
        }.addOnFailureListener {
            Log.e("ProductsActivity", "❌ Erreur chargement du panier", it)
        }
    }

    private fun addToCart(product: Product) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val cartRef = firestore.collection("paniers").document(userId)

        cartRef.get().addOnSuccessListener { document ->
            val cartItems = mutableListOf<CartItem>()

            if (document.exists()) {
                val existingCart = document.get("products") as? List<Map<String, Any>> ?: listOf()
                cartItems.addAll(existingCart.map {
                    CartItem(
                        id = it["id"] as String,
                        name = it["name"] as String,
                        price = (it["price"] as Number).toDouble(),
                        imageUrl = it["imageUrl"] as String,
                        quantity = (it["quantity"] as Long).toInt()
                    )
                })
            }

            val existingItem = cartItems.find { it.id == product.id }
            if (existingItem != null) {
                existingItem.quantity += 1
            } else {
                cartItems.add(CartItem(product.id, product.name, product.price.toDouble(), product.imageUrl, 1))
            }

            cartRef.set(mapOf("products" to cartItems.map {
                mapOf("id" to it.id, "name" to it.name, "price" to it.price, "imageUrl" to it.imageUrl, "quantity" to it.quantity)
            })).addOnSuccessListener {
                cartCounter++
                updateCartCounter()
                Toast.makeText(this, "${product.name} ajouté au panier", Toast.LENGTH_SHORT).show()
                Log.d("ProductsActivity", "✅ Produit ajouté avec succès : ${product.name}")
            }.addOnFailureListener {
                Log.e("ProductsActivity", "❌ Malheureusement !!!", it)
            }
        }.addOnFailureListener {
            Log.e("ProductsActivity", "❌ Erreur lors de la récupération du panier", it)
        }
    }

    private fun updateCartCounter() {
        val cartIcon = findViewById<TextView>(R.id.cartCounter)
        cartIcon.text = cartCounter.toString()
        cartIcon.visibility = if (cartCounter > 0) View.VISIBLE else View.GONE
        Log.d("ProductsActivity", "🛒 Compteur panier mis à jour : $cartCounter")
    }

    // Fonction pour naviguer vers DetailsActivity
    private fun navigateToDetails(product: Product) {
        val intent = Intent(this, DetailsActivity::class.java).apply {
            putExtra("PRODUCT_ID", product.id)
        }
        startActivity(intent)
    }
}