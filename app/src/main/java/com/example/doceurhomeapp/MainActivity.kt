package com.example.doceurhomeapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    // Dans MainActivity.kt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisation Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Vérifier si un utilisateur est connecté
        auth.currentUser?.let { user ->
            Log.d("FirebaseAuth", "Utilisateur connecté: ${user.email} (UID: ${user.uid})")
            Toast.makeText(this, "Connecté en tant que ${user.email}", Toast.LENGTH_LONG).show()
        } ?: run {
            Log.d("FirebaseAuth", "Aucun utilisateur connecté")
            Toast.makeText(this, "Aucun utilisateur connecté", Toast.LENGTH_LONG).show()
        }

        // Vérifier la connexion à Firestore
        testFirebaseConnection()

        // Initialisation des vues
        setupViews()
    }

    private fun setupViews() {
        val buyText: TextView = findViewById(R.id.buy_text)
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Initialisation du ViewPager2 et TabLayout
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        // Liste des images à afficher dans le slider
        val images = listOf(
            R.drawable.produit1,
            R.drawable.image,
            R.drawable.detail
        )

        // Adapter pour le ViewPager2
        val adapter = ImageSliderAdapter(images)
        viewPager.adapter = adapter

        // Lier le TabLayout au ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // Optionnel: Personnaliser les indicateurs de page
        }.attach()

        // Clic sur "Buy"
        buyText.setOnClickListener { navigateTo(connectActivity::class.java) }

        // Configuration de la barre de navigation en bas
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Déjà sur la page principale
                    true
                }
                R.id.nav_list -> {
                    // Redirige vers CategoryActivity
                    navigateTo(CategoryActivity::class.java)
                    true
                }
                R.id.nav_cart -> {
                    // Redirige vers connectActivity
                    navigateTo(connectActivity::class.java)
                    true
                }
                R.id.nav_profile -> {
                    // Redirige vers AddProductActivity
                    navigateTo(AddProductActivity::class.java)
                    true
                }
                else -> false
            }
        }
    }

    // Fonction générique pour changer d'activité
    private fun navigateTo(destination: Class<*>) {
        val intent = Intent(this, destination)
        startActivity(intent)
    }

    // Vérifier la connexion à Firestore
    private fun testFirebaseConnection() {
        db.collection("produits").limit(1).get()
            .addOnSuccessListener {
                Log.d("Firestore", "Connexion réussie à Firestore")
                Toast.makeText(this, "Connexion Firebase OK", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Échec de connexion à Firestore", e)
                Toast.makeText(this, "Échec de connexion Firebase", Toast.LENGTH_LONG).show()
            }
    }

    // Adapter pour le ViewPager2
    class ImageSliderAdapter(private val images: List<Int>) : RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_slider_image, parent, false)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            holder.bind(images[position])
        }

        override fun getItemCount(): Int = images.size

        class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageView: ImageView = itemView.findViewById(R.id.imageView)

            fun bind(imageRes: Int) {
                imageView.setImageResource(imageRes)
            }
        }
    }
}