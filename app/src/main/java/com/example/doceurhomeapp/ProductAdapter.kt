package com.example.doceurhomeapp

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ProductAdapter(
    private var productList: List<Product>,
    private val onAddToCartClick: (Product) -> Unit,
    private val onProductImageClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productImage: ImageView = view.findViewById(R.id.productImage)
        val productName: TextView = view.findViewById(R.id.productName)
        val productPrice: TextView = view.findViewById(R.id.productPrice)
        val addToCartButton: Button = view.findViewById(R.id.addToCartButton)
        val favoriteIcon: ImageView = view.findViewById(R.id.favoriteIcon)

        fun bind(product: Product) {
            Glide.with(itemView.context)
                .load(product.imageUrl)
                .into(productImage)

            productName.text = product.name
            productPrice.text = "${product.price} $"

            addToCartButton.setOnClickListener { onAddToCartClick(product) }
            productImage.setOnClickListener { onProductImageClick(product) }



            favoriteIcon.setOnClickListener {
                product.isFavorite = !product.isFavorite
                updateFavoriteIcon(product.isFavorite)
                toggleFavorite(product) // Déclenche l'opération Firestore
            }
            if (product.id.isBlank()) {
                Log.e("ProductAdapter", "Produit sans ID - ${product.name}")
                favoriteIcon.visibility = View.GONE // Cache l'icône si ID invalide
                return
            }

            checkFavoriteStatus(product)
        }

        private fun updateFavoriteIcon(isFavorite: Boolean) {
            favoriteIcon.setImageResource(
                if (isFavorite) R.drawable.plenne
                else R.drawable.vide
            )
            favoriteIcon.setColorFilter(
                ContextCompat.getColor(
                    itemView.context,
                    if (isFavorite) android.R.color.holo_red_dark
                    else android.R.color.darker_gray
                )
            )
        }

        private fun checkFavoriteStatus(product: Product) {
            // Vérification initiale
            if (product.id.isBlank()) {
                Log.e("Favorites", "ID produit vide")
                return
            }

            auth.currentUser?.uid?.let { userId ->
                db.collection("userFavorites")
                    .document(userId)
                    .collection("products")
                    .document(product.id)
                    .get()
                    .addOnSuccessListener { document ->
                        val isFavorite = document.exists()
                        product.isFavorite = isFavorite
                        updateFavoriteIcon(isFavorite)
                    }
                    .addOnFailureListener { e ->
                        Log.e("Favorites", "Erreur vérification favori", e)
                    }
            }
        }

        // Modifiez toggleFavorite():
        private fun toggleFavorite(product: Product) {
            val userId = auth.currentUser?.uid ?: {
                itemView.context.startActivity(Intent(itemView.context, sign_in::class.java))

            }

            val favoriteRef = db.collection("userFavorites")
                .document(userId.toString())
                .collection("products")
                .document(product.id)

            if (product.isFavorite) {
                favoriteRef.set(mapOf(
                    "productId" to product.id,
                    "name" to product.name,
                    "price" to product.price,
                    "imageUrl" to product.imageUrl,
                    "timestamp" to FieldValue.serverTimestamp()
                ))
            } else {
                favoriteRef.delete()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(productList[position])
    }

    override fun getItemCount() = productList.size

    fun updateList(newList: List<Product>) {
        this.productList = newList
        notifyDataSetChanged()
    }
}

private fun ProductAdapter.ProductViewHolder.startActivity(intent: Intent) {

}
