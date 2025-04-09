package com.example.doceurhomeapp

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.ExecutionException

class ProductsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private lateinit var searchEditText: EditText
    private lateinit var bottomNavigationView: BottomNavigationView
    private val firestore = FirebaseFirestore.getInstance()
    private val productList = mutableListOf<Product>()
    private val filteredProductList = mutableListOf<Product>()
    private val auth = FirebaseAuth.getInstance()
    private var cartCounter = 0
    private var selectedCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dans onCreate(), avant setContentView()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products2)

        // AJOUT: Gestion de la flèche de retour
        findViewById<ImageView>(R.id.backButton)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } ?: Log.w("ProductsActivity", "Flèche de retour non trouvée")

        // Le reste de votre code existant...
        bottomNavigationView = findViewById(R.id.bottom_navigation) ?: run {
            Log.e("ProductsActivity", "BottomNavigationView non trouvé")
            finish()
            return
        }
        setupBottomNavigation()

        searchEditText = findViewById(R.id.cherche) ?: run {
            Log.w("ProductsActivity", "Search EditText non trouvé")
            finish()
            return
        }
        setupSearch()

        selectedCategory = intent.getStringExtra("CATEGORY_NAME") ?: run {
            Toast.makeText(this, "Catégorie non trouvée", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        recyclerView = findViewById(R.id.recyclerViewProducts) ?: run {
            Log.e("ProductsActivity", "RecyclerView non trouvé")
            finish()
            return
        }

        val gridLayoutManager = GridLayoutManager(this, 2)
        recyclerView.layoutManager = gridLayoutManager

        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val position = parent.getChildAdapterPosition(view)
                val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)

                if (position % 2 == 1) {
                    outRect.top = resources.getDimensionPixelSize(R.dimen.grid_offset)
                }

                outRect.left = spacing / 2
                outRect.right = spacing / 2
                outRect.bottom = spacing
            }
        })

        recyclerView.setPadding(0, resources.getDimensionPixelSize(R.dimen.grid_top_padding), 0, 0)
        recyclerView.clipToPadding = false

        productAdapter = ProductAdapter(
            productList,
            onAddToCartClick = { product -> addToCart(product) },
            onProductImageClick = { product -> navigateToDetails(product) }
        )
        recyclerView.adapter = productAdapter

        findViewById<ImageView>(R.id.cart)?.setOnClickListener {
            startActivity(Intent(this, MycartActivity::class.java))
        } ?: Log.w("ProductsActivity", "Bouton panier non trouvé")

        FirebaseAuth.getInstance().addAuthStateListener {
            try {
                refreshFavorites()
            } catch (e: ExecutionException) {
                Log.w("BluetoothStats", "Erreur d'authentification", e)
            } catch (e: RuntimeException) {
                Log.w("BluetoothStats", "Erreur d'authentification", e)
            }
        }

        fetchProductsFromFirestore()
        fetchCartCount()
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
                    // Déjà sur la page panier si on clique depuis le bouton
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
    }

    private fun <T : Activity> navigateTo(activityClass: Class<T>) {
        val intent = Intent(this, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
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
            filteredProductList.addAll(productList)
        } else {
            for (product in productList) {
                if (product.name.contains(query, ignoreCase = true)) {
                    filteredProductList.add(product)
                }
            }
        }
        productAdapter.updateList(filteredProductList)
    }

    private fun fetchProductsFromFirestore() {
        firestore.collection("products")
            .whereEqualTo("category", selectedCategory)
            .get()
            .addOnSuccessListener { documents ->
                productList.clear()
                documents.forEach { document ->
                    try {
                        val data = document.data
                        val product = Product(
                            id = document.id,
                            name = data["name"]?.toString() ?: "",
                            price = when (val price = data["price"]) {
                                is Double -> price
                                is Long -> price.toDouble()
                                is String -> price.toDoubleOrNull() ?: 0.0
                                else -> 0.0
                            },
                            imageUrl = data["imageUrl"]?.toString() ?: "",
                            category = data["category"]?.toString() ?: "",
                            isFavorite = false
                        )
                        productList.add(product)
                    } catch (e: Exception) {
                        Log.e("ProductsActivity", "Error parsing product ${document.id}", e)
                    }
                }
                filteredProductList.clear()
                filteredProductList.addAll(productList)
                productAdapter.notifyDataSetChanged()
                loadInitialFavorites()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur de chargement", Toast.LENGTH_SHORT).show()
                Log.e("ProductsActivity", "Load error", e)
            }
    }

    private fun loadInitialFavorites() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        firestore.collection("userFavorites")
            .document(userId)
            .collection("products")
            .get()
            .addOnSuccessListener { documents ->
                val favoriteIds = documents.map { it.id }.toSet()
                productList.forEach { product ->
                    product.isFavorite = favoriteIds.contains(product.id)
                }
                runOnUiThread {
                    productAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun refreshFavorites() {
        if (auth.currentUser != null) {
            loadInitialFavorites()
        } else {
            productList.forEach { it.isFavorite = false }
            productAdapter.notifyDataSetChanged()
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