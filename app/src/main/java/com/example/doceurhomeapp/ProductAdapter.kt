package com.example.doceurhomeapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ProductAdapter(
    private var productList: List<Product>, // Changé en var
    private val onAddToCartClick: (Product) -> Unit,
    private val onFavoriteClick: (Product) -> Unit,
    private val onProductImageClick: (Product) -> Unit // Nouveau callback pour le clic sur l'image
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

        // Charger l'image avec Glide
        Glide.with(holder.itemView.context)
            .load(product.imageUrl)
            .into(holder.productImage)

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

        // Gestion du clic sur l'image du produit
        holder.productImage.setOnClickListener {
            onProductImageClick(product)
        }
    }

    override fun getItemCount() = productList.size
    fun updateList(newList: List<Product>) {
        productList = newList
        notifyDataSetChanged()
    }
}