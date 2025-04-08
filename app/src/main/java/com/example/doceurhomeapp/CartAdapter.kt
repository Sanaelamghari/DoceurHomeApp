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
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(
    private val cartItems: MutableList<CartItem>,
    private val onQuantityChanged: (CartItem, Int) -> Unit,
    private val onTotalUpdated: (Double) -> Unit,
    private val onDeleteItem: (CartItem) -> Unit,

) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2
    }

    inner class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productName: TextView = view.findViewById(R.id.cartProductName)
        val productPrice: TextView = view.findViewById(R.id.cartProductPrice)
        val productImage: ImageView = view.findViewById(R.id.cartProductImage)
        val quantityText: TextView = view.findViewById(R.id.quantityText)
        val btnIncrease: Button = view.findViewById(R.id.btnIncrease)
        val btnDecrease: Button = view.findViewById(R.id.btnDecrease)
        val btnDelete: ImageView = view.findViewById(R.id.deleteProduct)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart_product, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartItems[position]
        with(holder) {
            productName.text = item.name
            productPrice.text = currencyFormat.format(item.price * item.quantity)
            quantityText.text = item.quantity.toString()

            Glide.with(itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.plenne)
                .error(R.drawable.person)
                .into(productImage)

            btnIncrease.setOnClickListener {
                val newQuantity = item.quantity + 1
                onQuantityChanged(item, newQuantity)
            }

            btnDecrease.setOnClickListener {
                if (item.quantity > 1) {
                    val newQuantity = item.quantity - 1
                    onQuantityChanged(item, newQuantity)
                }
            }

            btnDelete.setOnClickListener {
                onDeleteItem(item)
            }
        }
    }

    override fun getItemCount(): Int = cartItems.size

    fun updateItems(newItems: List<CartItem>) {
        cartItems.clear()
        cartItems.addAll(newItems)
        notifyDataSetChanged()
        updateTotal()
    }

    fun updateItemQuantity(position: Int, newQuantity: Int) {
        if (position in cartItems.indices) {
            cartItems[position].quantity = newQuantity
            notifyItemChanged(position)
            updateTotal()
        }
    }

    fun updateItemById(itemId: String, newQuantity: Int) {
        val position = cartItems.indexOfFirst { it.id == itemId }
        if (position != -1) {
            cartItems[position].quantity = newQuantity
            notifyItemChanged(position)
            updateTotal()
        }
    }

    fun removeItem(item: CartItem) {
        val position = cartItems.indexOfFirst { it.id == item.id }
        if (position != -1) {
            cartItems.removeAt(position)
            notifyItemRemoved(position)
            updateTotal()
        }
    }

    fun getPosition(itemId: String): Int {
        return cartItems.indexOfFirst { it.id == itemId }
    }

    fun calculateTotal(): Double {
        return cartItems.sumOf { it.price * it.quantity }
    }

    private fun updateTotal() {
        onTotalUpdated(calculateTotal())
    }

    // Dans CartAdapter.kt
    fun getCartItems(): List<CartItem> {
        return cartItems.toList() // Retourne une copie immuable de la liste
    }
}