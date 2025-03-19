package com.example.doceurhomeapp

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.firestore.FirebaseFirestore

class DetailsActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var productName: TextView
    private lateinit var productPrice: TextView
    private lateinit var productDescription: TextView

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        viewPager = findViewById(R.id.viewPager)
        productName = findViewById(R.id.productName)
        productPrice = findViewById(R.id.productPrice)
        productDescription = findViewById(R.id.productDescription)

        val productId = intent.getStringExtra("PRODUCT_ID") ?: run {
            Log.e("DetailsActivity", "Aucun ID de produit trouvé")
            finish()
            return
        }

        fetchProductDetails(productId)
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
                        val imageUrls = listOf(it.imageUrl) // Remplace par une liste d'URLs si disponible
                        val adapter = ImageSliderAdapter(imageUrls)
                        viewPager.adapter = adapter
                    }
                } else {
                    Log.e("DetailsActivity", "Produit non trouvé")
                }
            }
            .addOnFailureListener { e ->
                Log.e("DetailsActivity", "Erreur lors du chargement du produit", e)
            }
    }
}