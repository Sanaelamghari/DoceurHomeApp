package com.example.doceurhomeapp

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CartAdapter(
    private val cartItems: MutableList<CartItem>,
    private val onQuantityChanged: (CartItem, Int) -> Unit,
    private val onTotalUpdated: (Double) -> Unit // ✅ Callback pour mettre à jour le total
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productName: TextView = view.findViewById(R.id.cartProductName)
        val productPrice: TextView = view.findViewById(R.id.cartProductPrice)
        val productImage: ImageView = view.findViewById(R.id.cartProductImage)
        val quantityText: TextView = view.findViewById(R.id.quantityText)
        val btnIncrease: Button = view.findViewById(R.id.btnIncrease)
        val btnDecrease: Button = view.findViewById(R.id.btnDecrease)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart_product, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartItems[position]

        holder.productName.text = item.name
        holder.productPrice.text = "${item.price} $"
        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.plenne)
            .error(R.drawable.person)
            .into(holder.productImage)

        holder.quantityText.text = item.quantity.toString()

        // ✅ Gestion des boutons d'incrémentation et décrémentation
        holder.btnIncrease.setOnClickListener {
            val newQuantity = item.quantity + 1
            Log.d("CartAdapter", "🟢 Augmentation de la quantité de ${item.name} à $newQuantity")
            onQuantityChanged(item, newQuantity)
        }

        holder.btnDecrease.setOnClickListener {
            if (item.quantity > 1) {
                val newQuantity = item.quantity - 1
                Log.d("CartAdapter", "🟠 Diminution de la quantité de ${item.name} à $newQuantity")
                onQuantityChanged(item, newQuantity)
            } else {
                Log.w("CartAdapter", "🔴 Impossible de diminuer sous 1 pour ${item.name}")
            }
        }
    }

    override fun getItemCount() = cartItems.size

    // ✅ Fonction appelée depuis l'activité pour recalculer le total
    fun updateTotal() {
        val total = cartItems.sumOf { it.price * it.quantity }
        onTotalUpdated(total)
    }
}


