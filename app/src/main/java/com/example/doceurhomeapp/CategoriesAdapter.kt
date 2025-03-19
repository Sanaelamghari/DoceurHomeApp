package com.example.doceurhomeapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CategoriesAdapter(
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder>() {

    private val categories = mutableListOf<Category>()

    // Mettre à jour la liste des catégories
    fun submitList(newCategories: List<Category>) {
        categories.clear()
        categories.addAll(newCategories)
        notifyDataSetChanged()
    }

    // Créer un ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    // Remplir les données dans le ViewHolder
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.bind(category)
        holder.itemView.setOnClickListener {
            onCategoryClick(category.name)
        }
    }

    // Nombre d'éléments dans la liste
    override fun getItemCount(): Int {
        return categories.size
    }

    // ViewHolder pour une catégorie
    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val ivCategoryImage: ImageView = itemView.findViewById(R.id.ivCategoryImage)

        fun bind(category: Category) {
            tvCategoryName.text = category.name
            Glide.with(itemView.context)
                .load(category.imageUrl)
                .into(ivCategoryImage)
        }
    }
}