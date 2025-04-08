package com.example.doceurhomeapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MycartActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var totalPriceText: TextView
    private lateinit var cartAdapter: CartAdapter
    private lateinit var bottomNavigationView: BottomNavigationView

    private var firestoreListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mycart)

        recyclerView = findViewById(R.id.recyclerViewCart)
        totalPriceText = findViewById(R.id.totalPriceText)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        cartAdapter = CartAdapter(
            mutableListOf(),
            onQuantityChanged = { item, newQuantity ->
                updateCartQuantity(item, newQuantity)
            },
            onTotalUpdated = { total ->
                updateTotalDisplay(total)
            },
            onDeleteItem = { item ->
                showDeleteConfirmationDialog(item)
            }
        )
        recyclerView.adapter = cartAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        setupBottomNavigation()
        setupFirestoreListener()
    }

    private fun setupFirestoreListener() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            showToast("Veuillez vous connecter")
            return
        }

        firestoreListener = FirebaseFirestore.getInstance()
            .collection("paniers")
            .document(userId)
            .addSnapshotListener { document, error ->
                when {
                    error != null -> {
                        Log.e("MyCart", "Listen failed", error)
                        showToast("Erreur de chargement du panier")
                        return@addSnapshotListener
                    }
                    document != null && document.exists() -> {
                        handleCartDocument(document)
                    }
                    else -> {
                        Log.d("MyCart", "Current data: null")
                        cartAdapter.updateItems(emptyList())
                    }
                }
            }
    }

    private fun handleCartDocument(document: DocumentSnapshot?) {
        try {
            val items = (document?.get("products") as? List<Map<String, Any>>)?.mapNotNull {
                try {
                    CartItem(
                        id = it["id"] as? String ?: "",
                        name = it["name"] as? String ?: "Produit inconnu",
                        price = (it["price"] as? Number)?.toDouble() ?: 0.0,
                        imageUrl = it["imageUrl"] as? String ?: "",
                        quantity = (it["quantity"] as? Number)?.toInt() ?: 1
                    )
                } catch (e: Exception) {
                    Log.e("MyCart", "Error parsing item", e)
                    null
                }
            } ?: emptyList()

            cartAdapter.updateItems(items)
            updateTotalDisplay(cartAdapter.calculateTotal())
        } catch (e: Exception) {
            Log.e("MyCart", "Error processing document", e)
            showToast("Erreur de traitement des données")
        }
    }

    private fun updateTotalDisplay(total: Double) {
        runOnUiThread {
            totalPriceText.text = "Total: %.2f $".format(total)
        }
    }

    private fun updateCartQuantity(item: CartItem, newQuantity: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Log.d("MyCart", "Updating item ${item.id} to quantity $newQuantity")

        // Mise à jour optimiste UI
        cartAdapter.updateItemById(item.id, newQuantity)

        // Mise à jour Firestore en utilisant l'ID du produit
        FirebaseFirestore.getInstance().collection("paniers")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val products = document.get("products") as? List<Map<String, Any>> ?: return@addOnSuccessListener
                val updatedProducts = products.map { product ->
                    if ((product["id"] as? String) == item.id) {
                        val mutableProduct = product.toMutableMap()
                        mutableProduct["quantity"] = newQuantity
                        return@map mutableProduct
                    }
                    product
                }

                FirebaseFirestore.getInstance().collection("paniers")
                    .document(userId)
                    .update("products", updatedProducts)
                    .addOnFailureListener { e ->
                        // Rollback en cas d'échec
                        cartAdapter.updateItemById(item.id, item.quantity)
                        Log.e("MyCart", "Update failed", e)
                        showToast("Échec de la mise à jour")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MyCart", "Error getting document", e)
                showToast("Erreur de chargement du panier")
            }
    }

    private fun deleteFromCart(item: CartItem) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // 1. Sauvegarde de la position pour un rollback précis
        val position = cartAdapter.getPosition(item.id)
        val currentItems = cartAdapter.getCartItems().toMutableList()

        // 2. Mise à jour optimiste UI
        cartAdapter.removeItem(item)

        // 3. Suppression Firestore
        FirebaseFirestore.getInstance().collection("paniers")
            .document(userId)
            .update("products", FieldValue.arrayRemove(item.toFirestoreMap()))
            .addOnFailureListener { e ->
                // Rollback précis
                if (position != -1) {
                    currentItems.add(position, item)
                } else {
                    currentItems.add(item)
                }
                cartAdapter.updateItems(currentItems)

                Log.e("Cart", "Delete failed", e)
                showToast("Échec de la suppression")
            }
    }

    private fun showDeleteConfirmationDialog(item: CartItem) {
        AlertDialog.Builder(this)
            .setTitle("Confirmer suppression")
            .setMessage("Supprimer ${item.name} du panier ?")
            .setPositiveButton("Oui") { _, _ ->
                deleteFromCart(item)
            }
            .setNegativeButton("Non", null)
            .show()
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
                    // Déjà sur la page du panier
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
        bottomNavigationView.selectedItemId = R.id.nav_cart
    }

    private fun <T : Activity> navigateTo(activityClass: Class<T>) {
        val intent = Intent(this, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        firestoreListener?.remove()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MyCartActivity"
    }
}