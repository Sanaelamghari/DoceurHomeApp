package com.example.doceurhomeapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MycartActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var totalPriceText: TextView
    private lateinit var cartAdapter: CartAdapter
    private var cartItems = mutableListOf<CartItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mycart)

        recyclerView = findViewById(R.id.recyclerViewCart)
        totalPriceText = findViewById(R.id.totalPriceText) // ✅ Lier la TextView

        recyclerView.layoutManager = LinearLayoutManager(this)

        cartAdapter = CartAdapter(cartItems, { item, newQuantity ->
            updateCartQuantity(item, newQuantity)
        }, { total ->
            totalPriceText.text = "Total: ${total} $" // ✅ Mettre à jour l'affichage du total
        })

        recyclerView.adapter = cartAdapter

        fetchCartItems() // ✅ Récupération des produits
    }

    private fun fetchCartItems() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val cartRef = FirebaseFirestore.getInstance().collection("paniers").document(userId)

        cartRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                Log.w("MyCartActivity", "⚠️ Aucun document trouvé pour l'utilisateur $userId")
                return@addOnSuccessListener
            }

            val existingCart = document.get("products") as? List<Map<String, Any>> ?: emptyList()

            Log.d("MyCartActivity", "✅ Produits récupérés: $existingCart")

            cartItems.clear()
            for (item in existingCart) {
                try {
                    val id = item["id"] as? String ?: ""
                    val name = item["name"] as? String ?: "Produit inconnu"
                    val price = (item["price"] as? Number)?.toDouble() ?: 0.0
                    val imageUrl = item["imageUrl"] as? String ?: ""
                    val quantity = (item["quantity"] as? Number)?.toInt() ?: 1

                    cartItems.add(CartItem(id, name, price, imageUrl, quantity))

                } catch (e: Exception) {
                    Log.e("MyCartActivity", "❌ Erreur conversion item: $item", e)
                }
            }

            cartAdapter.notifyDataSetChanged()
            updateTotal() // ✅ Calculer le total après récupération des produits
        }.addOnFailureListener { e ->
            Log.e("MyCartActivity", "❌ Erreur Firebase : ${e.message}", e)
        }
    }
    private fun updateCartQuantity(item: CartItem, newQuantity: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val cartRef = FirebaseFirestore.getInstance().collection("paniers").document(userId)

        cartRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val products = document.get("products") as? MutableList<Map<String, Any>> ?: return@addOnSuccessListener

                val updatedProducts = products.map { product ->
                    val mutableProduct = product.toMutableMap()
                    if (mutableProduct["id"] == item.id) {
                        mutableProduct["quantity"] = newQuantity
                    }
                    mutableProduct
                }

                cartRef.update("products", updatedProducts)
                    .addOnSuccessListener {
                        Log.d("MyCartActivity", "✅ Quantité mise à jour pour ${item.name} à $newQuantity")

                        item.quantity = newQuantity
                        val position = cartItems.indexOfFirst { it.id == item.id }
                        if (position != -1) {
                            cartAdapter.notifyItemChanged(position)
                        }

                        cartAdapter.updateTotal() // ✅ Recalculer le total après mise à jour
                    }
                    .addOnFailureListener { e ->
                        Log.e("MyCartActivity", "❌ Erreur mise à jour quantité : ${e.message}", e)
                    }
            }
        }.addOnFailureListener { e ->
            Log.e("MyCartActivity", "❌ Erreur récupération panier : ${e.message}", e)
        }
    }


    // ✅ Fonction pour recalculer le total du panier
    private fun updateTotal() {
        val total = cartItems.sumOf { it.price * it.quantity }
        totalPriceText.text = "Total: ${total} $"
    }
    fun goToPayment(view: View) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("MycartActivity", "❌ Utilisateur non connecté")
            return
        }

        val cartRef = FirebaseFirestore.getInstance().collection("paniers").document(userId)

        cartRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val total = cartItems.sumOf { it.price * it.quantity } // Calcul du total
                val commande = hashMapOf(
                    "userId" to userId,
                    "cartId" to document.id,
                    "total" to total,
                    "timestamp" to System.currentTimeMillis()
                )

                FirebaseFirestore.getInstance().collection("commandes")
                    .add(commande)
                    .addOnSuccessListener { docRef ->
                        Log.d("MycartActivity", "✅ Commande enregistrée avec ID : ${docRef.id}")

                        // Afficher un message ou naviguer vers une autre page
                        val intent = Intent(this, paimentActivity::class.java)
                        startActivity(intent)

                    }
                    .addOnFailureListener { e ->
                        Log.e("MycartActivity", "❌ Erreur enregistrement commande : ${e.message}", e)
                    }
            } else {
                Log.w("MycartActivity", "⚠️ Panier introuvable")
            }
        }.addOnFailureListener { e ->
            Log.e("MycartActivity", "❌ Erreur récupération panier : ${e.message}", e)
        }
    }

}

