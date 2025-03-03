package com.example.doceurhomeapp

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class ProductAdapter(
    private val productList: List<Product>,
    private val onAddToCartClick: (Product) -> Unit,
    private val onFavoriteClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productImage: ImageView = view.findViewById(R.id.productImage)
        val productName: TextView = view.findViewById(R.id.productName)
        val productPrice: TextView = view.findViewById(R.id.productPrice)
        val addToCartButton: Button = view.findViewById(R.id.addToCartButton)
        val favoriteIcon: ImageView = view.findViewById(R.id.favoriteIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        // Charger l'image avec Glide ou Picasso
        Glide.with(holder.itemView.context).load(product.imageUrl).into(holder.productImage)

        holder.productName.text = product.name
        holder.productPrice.text = "${product.price} $"

        // Gestion du clic sur "Add to Cart"
        holder.addToCartButton.setOnClickListener {
            onAddToCartClick(product)
        }

        // Gestion du clic sur l'icône de favori
        holder.favoriteIcon.setOnClickListener {
            onFavoriteClick(product)
        }
    }

    override fun getItemCount() = productList.size
}

