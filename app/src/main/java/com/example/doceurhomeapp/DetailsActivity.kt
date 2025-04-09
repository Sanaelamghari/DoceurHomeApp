package com.example.doceurhomeapp

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.Button
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
    private var currentProduct: Product? = null
    private var currentRating = 0

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dans onCreate(), avant setContentView()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        // Gestion de la flèche de retour
        findViewById<ImageView>(R.id.imageView8)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } ?: Log.w("DetailsActivity", "Flèche de retour non trouvée")

        // Initialisation des vues
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabDots)
        productName = findViewById(R.id.productName)
        productPrice = findViewById(R.id.productPrice)
        productDescription = findViewById(R.id.productDescription)

        // Configuration des étoiles
        star1 = findViewById(R.id.star1)
        star2 = findViewById(R.id.star2)
        star3 = findViewById(R.id.star3)
        star4 = findViewById(R.id.star4)
        star5 = findViewById(R.id.star5)

        // Gestion du bouton d'ajout au panier
        val addToCartButton = findViewById<Button>(R.id.addToCartButton)
        addToCartButton?.setOnClickListener {
            currentProduct?.let { product ->
                CartUtils.addToCart(
                    context = this@DetailsActivity,
                    product = product,
                    onSuccess = {
                        Toast.makeText(
                            this@DetailsActivity,
                            "${product.name} ajouté au panier",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateCartCounter()
                    },
                    onFailure = { e ->
                        Toast.makeText(
                            this@DetailsActivity,
                            "Échec de l'ajout au panier",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("DetailsActivity", "Erreur ajout panier", e)
                    }
                )
            } ?: Toast.makeText(this, "Produit non chargé", Toast.LENGTH_SHORT).show()
        }

        val productId = intent.getStringExtra("PRODUCT_ID") ?: run {
            Log.e("DetailsActivity", "Aucun ID de produit trouvé")
            finish()
            return
        }

        fetchProductDetails(productId)
        setupStarRating()
    }

    // AJOUT: Fonction pour mettre à jour le compteur du panier

    private fun fetchProductDetails(productId: String) {
        firestore.collection("products").document(productId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val product = document.toObject(Product::class.java)?.apply {
                        id = document.id // Assure que l'ID est bien renseigné
                    }

                    product?.let {
                        // Mettre à jour le produit courant
                        currentProduct = it

                        // Mettre à jour les vues
                        productName.text = it.name
                        productPrice.text = "%.2f $".format(it.price) // Formatage à 2 décimales
                        productDescription.text = it.description

                        // Configurer le slider d'images
                        val imageUrls = listOf(it.imageUrl) // Vous pouvez modifier pour supporter plusieurs images
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

                        // Activer le bouton d'ajout au panier
                        findViewById<Button>(R.id.addToCartButton)?.isEnabled = true
                    } ?: run {
                        Log.e("DetailsActivity", "Erreur de parsing du produit")
                        Toast.makeText(this, "Erreur de chargement du produit", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Log.e("DetailsActivity", "Produit non trouvé")
                    Toast.makeText(this, "Produit introuvable", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e("DetailsActivity", "Erreur lors du chargement du produit", e)
                Toast.makeText(this, "Échec du chargement", Toast.LENGTH_SHORT).show()
                finish()
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

    private fun updateCartCounter() {
        // Exemple d'implémentation si vous avez un compteur
        val cartCounterView = findViewById<TextView>(R.id.cartCounter)
        cartCounterView?.text = (cartCounterView?.text?.toString()?.toIntOrNull()?.plus(1) ?: 1).toString()
    }
}