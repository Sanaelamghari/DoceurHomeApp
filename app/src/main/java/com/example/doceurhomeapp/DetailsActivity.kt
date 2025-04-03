package com.example.doceurhomeapp

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore

class DetailsActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var productName: TextView
    private lateinit var productPrice: TextView
    private lateinit var productDescription: TextView
    private lateinit var star1: ImageView
    private lateinit var star2: ImageView
    private lateinit var star3: ImageView
    private lateinit var star4: ImageView
    private lateinit var star5: ImageView

    private var currentRating = 0
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        // Initialisation des vues
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabDots)
        productName = findViewById(R.id.productName)
        productPrice = findViewById(R.id.productPrice)
        productDescription = findViewById(R.id.productDescription)

        // Initialisation des étoiles
        star1 = findViewById(R.id.star1)
        star2 = findViewById(R.id.star2)
        star3 = findViewById(R.id.star3)
        star4 = findViewById(R.id.star4)
        star5 = findViewById(R.id.star5)

        val productId = intent.getStringExtra("PRODUCT_ID") ?: run {
            Log.e("DetailsActivity", "Aucun ID de produit trouvé")
            finish()
            return
        }

        fetchProductDetails(productId)
        setupStarRating()
    }

    private fun fetchProductDetails(productId: String) {
        firestore.collection("products").document(productId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val product = document.toObject(Product::class.java)
                    product?.let {
                        productName.text = it.name
                        productPrice.text = "${it.price} $"
                        productDescription.text = it.description

                        // Configurer le slider d'images
                        val imageUrls = listOf(it.imageUrl)
                        val adapter = ImageSliderAdapter(imageUrls)
                        viewPager.adapter = adapter

                        // Configuration des indicateurs de points
                        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                            // Configuration vide pour les dots
                        }.attach()

                        tabLayout.tabMode = TabLayout.MODE_FIXED
                        tabLayout.tabRippleColor = ColorStateList.valueOf(Color.TRANSPARENT)

                        // Charger la notation existante
                        it.rating?.let { rating ->
                            currentRating = rating
                            updateStars(rating)
                        }
                    }
                } else {
                    Log.e("DetailsActivity", "Produit non trouvé")
                }
            }
            .addOnFailureListener { e ->
                Log.e("DetailsActivity", "Erreur lors du chargement du produit", e)
            }
    }

    private fun setupStarRating() {
        val stars = listOf(star1, star2, star3, star4, star5)

        stars.forEach { star ->
            star.setOnClickListener {
                val clickedRating = (it.tag as String).toInt()
                currentRating = clickedRating
                updateStars(clickedRating)
                saveRatingToDatabase(clickedRating)
            }
        }
    }

    private fun updateStars(rating: Int) {
        val stars = listOf(star1, star2, star3, star4, star5)

        stars.forEachIndexed { index, star ->
            if (index < rating) {
                star.setImageResource(R.drawable.ic_star_filled)
                star.alpha = 1f
            } else {
                star.setImageResource(R.drawable.ic_star_outline)
                star.alpha = 0.6f
            }
        }
    }

    private fun saveRatingToDatabase(rating: Int) {
        val productId = intent.getStringExtra("PRODUCT_ID") ?: return

        firestore.collection("products").document(productId)
            .update("rating", rating)
            .addOnSuccessListener {
                Toast.makeText(this, "Merci pour votre notation !", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("DetailsActivity", "Erreur lors de la sauvegarde de la notation", e)
                Toast.makeText(this, "Erreur lors de l'enregistrement", Toast.LENGTH_SHORT).show()
            }
    }
}