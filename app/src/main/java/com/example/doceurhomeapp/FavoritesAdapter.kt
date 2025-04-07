package com.example.doceurhomeapp

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class FavoritesAdapter(
    private var favorites: MutableList<Product>,
    private val onFavoriteClick: (Product) -> Unit,
    private val onAddToCartClick: (Product) -> Unit

) : RecyclerView.Adapter<FavoritesAdapter.FavoritesViewHolder>() {

    inner class FavoritesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.productImage)
        val productName: TextView = itemView.findViewById(R.id.productName)
        val productPrice: TextView = itemView.findViewById(R.id.productPrice)
        val favoriteButton: ImageView = itemView.findViewById(R.id.imageView9)
        val addToCartButton: ImageView = itemView.findViewById(R.id.imageView10)
    }

    fun updateList(newList: List<Product>) {
        favorites.clear()
        favorites.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.favorites_item, parent, false)
        return FavoritesViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoritesViewHolder, position: Int) {
        val product = favorites[position]

        Glide.with(holder.itemView.context)
            .load(product.imageUrl)
            .into(holder.productImage)

        holder.productName.text = product.name
        holder.productPrice.text = "$${product.price}"

        holder.favoriteButton.setImageResource(
            if (product.isFavorite) R.drawable.plenne
            else R.drawable.ic_favorite_border
        )

        holder.favoriteButton.setOnClickListener {
            onFavoriteClick(product)
        }

        holder.addToCartButton.setOnClickListener {
            onAddToCartClick(product)
        }

        setClickAnimation(holder.favoriteButton)
        setClickAnimation(holder.addToCartButton)
    }

    override fun getItemCount() = favorites.size

    private fun setClickAnimation(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }
            }
            false
        }
    }
}