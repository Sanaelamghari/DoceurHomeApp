package com.example.doceurhomeapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Nouveau: Initialisation des vues pour le header
        val titleTextView: TextView = findViewById(R.id.title_text)
        val subtitleTextView: TextView = findViewById(R.id.subtitle_text)
        val menuIcon: ImageView = findViewById(R.id.menu_icon)

        // Configuration du texte
        titleTextView.text = "Douceur Homeware"
        subtitleTextView.text = "Where Elegance Resides,\nand Beauty Blossoms"

        // Configuration du menu
        menuIcon.setOnClickListener { view ->
            PopupMenu(this, view).apply {
                menuInflater.inflate(R.menu.main_menu, menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_login -> {
                            startActivity(Intent(this@MainActivity, sign_in::class.java))
                            true
                        }
                        R.id.menu_register -> {
                            startActivity(Intent(this@MainActivity, SignupActivity::class.java))
                            true
                        }
                        R.id.menu_logout -> {
                            auth.signOut()
                            Toast.makeText(this@MainActivity, "Déconnexion réussie", Toast.LENGTH_SHORT).show()
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        }

        // Votre code existant inchangé ci-dessous
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        auth.currentUser?.let { user ->
            Log.d("FirebaseAuth", "Utilisateur connecté: ${user.email} (UID: ${user.uid})")
            Toast.makeText(this, "Connecté en tant que ${user.email}", Toast.LENGTH_LONG).show()
        } ?: run {
            Log.d("FirebaseAuth", "Aucun utilisateur connecté")
            Toast.makeText(this, "Aucun utilisateur connecté", Toast.LENGTH_LONG).show()
        }

        testFirebaseConnection()

        val buyText: TextView = findViewById(R.id.buy_text)
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        val images = listOf(
            R.drawable.tab3,
            R.drawable.tab45,
            R.drawable.tb71,
            R.drawable.tb73,
            R.drawable.vig,
            R.drawable.riche,
            R.drawable.tab52
        )

        val adapter = ImageSliderAdapter(images)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
        }.attach()

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Pas besoin de recreate() si vous êtes déjà sur Home
                    true
                }
                R.id.nav_list -> {
                    navigateTo(CategoryActivity::class.java)
                    true
                }
                R.id.nav_cart -> {
                    navigateTo(connectActivity::class.java)
                    true
                }
                R.id.nav_profile -> {
                    navigateTo(Favorites::class.java)
                    true
                }
                else -> false
            }.also { result ->
                if (result) overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
    }

    private fun navigateTo(destination: Class<*>) {
        val intent = Intent(this, destination)
        startActivity(intent)
    }

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

    class ImageSliderAdapter(private val images: List<Int>) : RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image_slider, parent, false)
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

    fun onSeeMoreClicked(view: View) {
        val intent = Intent(this, Bestsellers::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}