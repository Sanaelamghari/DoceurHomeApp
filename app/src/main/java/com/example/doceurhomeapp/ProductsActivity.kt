package com.example.doceurhomeapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
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
    private lateinit var searchEditText: EditText
    private val firestore = FirebaseFirestore.getInstance()
    private val productList = mutableListOf<Product>()
    private val filteredProductList = mutableListOf<Product>()

    private var cartCounter = 0
    private var selectedCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products2)

        // Initialisation de la recherche
        searchEditText = findViewById(R.id.cherche)
        setupSearch()

        // Récupérer la catégorie sélectionnée depuis l'Intent
        selectedCategory = intent.getStringExtra("CATEGORY_NAME")
        if (selectedCategory == null) {
            Toast.makeText(this, "Catégorie non trouvée", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        recyclerView = findViewById(R.id.recyclerViewProducts)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Initialisation de l'adaptateur avec la liste complète
        productAdapter = ProductAdapter(productList,
            onAddToCartClick = { product -> addToCart(product) },
            onFavoriteClick = { product -> addToFavorites(product) },
            onProductImageClick = { product -> navigateToDetails(product) }
        )

        recyclerView.adapter = productAdapter

        val cartIcon = findViewById<ImageView>(R.id.cart)
        cartIcon.setOnClickListener {
            startActivity(Intent(this, MycartActivity::class.java))
        }

        fetchProductsFromFirestore()
        fetchCartCount()
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterProducts(s?.toString() ?: "")
            }
        })
    }

    private fun filterProducts(query: String) {
        filteredProductList.clear()

        if (query.isEmpty()) {
            // Si la recherche est vide, afficher tous les produits
            filteredProductList.addAll(productList)
        } else {
            // Filtrer les produits dont le nom contient la requête (insensible à la casse)
            for (product in productList) {
                if (product.name.contains(query, ignoreCase = true)) {
                    filteredProductList.add(product)
                }
            }
        }

        // Mettre à jour l'adaptateur avec la liste filtrée
        productAdapter.updateList(filteredProductList)
    }

    private fun addToFavorites(product: Product) {
        Toast.makeText(this, "${product.name} ajouté aux favoris", Toast.LENGTH_SHORT).show()
    }

    private fun fetchProductsFromFirestore() {
        firestore.collection("products")
            .whereEqualTo("category", selectedCategory)
            .get()
            .addOnSuccessListener { documents ->
                productList.clear()
                for (document in documents) {
                    val product = document.toObject(Product::class.java).copy(id = document.id)
                    productList.add(product)
                }
                // Initialiser la liste filtrée avec tous les produits
                filteredProductList.addAll(productList)
                productAdapter.notifyDataSetChanged()
                Log.d("ProductsActivity", "Produits chargés: ${productList.size}")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erreur de chargement", Toast.LENGTH_SHORT).show()
                Log.e("ProductsActivity", "Erreur chargement produits", it)
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
            Log.e("ProductsActivity", "Erreur chargement panier", it)
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
                mapOf(
                    "id" to it.id,
                    "name" to it.name,
                    "price" to it.price,
                    "imageUrl" to it.imageUrl,
                    "quantity" to it.quantity
                )
            })).addOnSuccessListener {
                cartCounter++
                updateCartCounter()
                Toast.makeText(this, "${product.name} ajouté au panier", Toast.LENGTH_SHORT).show()
                Log.d("ProductsActivity", "Produit ajouté: ${product.name}")
            }.addOnFailureListener {
                Log.e("ProductsActivity", "Erreur ajout panier", it)
            }
        }.addOnFailureListener {
            Log.e("ProductsActivity", "Erreur récupération panier", it)
        }
    }

    private fun updateCartCounter() {
        val cartIcon = findViewById<TextView>(R.id.cartCounter)
        cartIcon.text = cartCounter.toString()
        cartIcon.visibility = if (cartCounter > 0) View.VISIBLE else View.GONE
        Log.d("ProductsActivity", "Compteur panier: $cartCounter")
    }

    private fun navigateToDetails(product: Product) {
        val intent = Intent(this, DetailsActivity::class.java).apply {
            putExtra("PRODUCT_ID", product.id)
        }
        startActivity(intent)
    }
}