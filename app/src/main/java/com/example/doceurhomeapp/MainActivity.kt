package com.example.doceurhomeapp

import BestsellersAdapter
import android.annotation.SuppressLint
import android.content.Intent
import com.example.doceurhomeapp.R

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {
    private lateinit var bestsellersRecyclerView: RecyclerView
    private lateinit var bestsellersAdapter: BestsellersAdapter

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private val firestore = FirebaseFirestore.getInstance()
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuration de la fenêtre
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)
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

        bestsellersRecyclerView = findViewById(R.id.bestsellersRecyclerView)
        bestsellersRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        bestsellersAdapter = BestsellersAdapter(emptyList()) { product ->
            // Handle item click
            //showProductDetails(product)
        }

        bestsellersRecyclerView.adapter = bestsellersAdapter


        loadBestsellers()

        // Initialisation Firebase
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

        // Configuration du ViewPager et TabLayout
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

        // Configuration de la BottomNavigation
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> true
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

    private fun loadBestsellers() {
        FirebaseFirestore.getInstance().collection("bestsellers")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                val products = result.documents.mapNotNull { doc ->
                    try {
                        Product(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",

                        )
                    } catch (e: Exception) {
                        Log.e("FirestoreError", "Error parsing product", e)
                        null
                    }
                }
                bestsellersAdapter.updateList(products)
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Error loading bestsellers", e)
                Toast.makeText(this, "Error loading bestsellers", Toast.LENGTH_SHORT).show()
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
    /*  fun updateCartCounter() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("paniers")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val count = if (document.exists()) {
                    (document.get("products") as? List<*>)?.size ?: 0
                } else {
                    0
                }

                val cartIcon = findViewById<TextView>(R.id.cartCounter)
                cartIcon.text = count.toString()
                cartIcon.visibility = if (count > 0) View.VISIBLE else View.GONE
            }
    }
    fun onSeeMoreClicked(view: View) {
        val intent = Intent(this, Bestsellers::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }*/
}