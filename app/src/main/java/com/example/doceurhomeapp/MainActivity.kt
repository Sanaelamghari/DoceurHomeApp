package com.example.doceurhomeapp

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



    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var bestsellersContainer: LinearLayout
    private lateinit var bestsellersScrollView: HorizontalScrollView

    private val bestsellersList = mutableListOf<Product>()
    private lateinit var bestsellersRecyclerView: RecyclerView  // Pas ScrollView!
    private lateinit var bestsellersAdapter: ProductAdapter


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

        // Initialisation des vues
        bestsellersContainer = findViewById(R.id.bestsellersContainer)
        bestsellersScrollView = findViewById(R.id.bestsellersScrollView)
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

        // Chargement des bestsellers
        loadBestsellers()
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
    // Méthode pour charger les bestsellers
    private fun loadBestsellers(adapter: ProductAdapter) {
        FirebaseFirestore.getInstance().collection("bestsellers")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10) // Limite à 10 bestsellers pour l'affichage horizontal
            .get()
            .addOnSuccessListener { result ->
                val bestsellerIds = result.documents.map { it.id }

                if (bestsellerIds.isNotEmpty()) {
                    FirebaseFirestore.getInstance().collection("products")
                        .whereIn("id", bestsellerIds)
                        .get()
                        .addOnSuccessListener { productsResult ->
                            val bestsellers = productsResult.documents.map { document ->
                                document.toObject(Product::class.java)!!.copy(isBestseller = true)
                            }.sortedByDescending { it.addedToBestsellers }

                            adapter.updateList(bestsellers)
                        }
                        .addOnFailureListener { e ->
                            Log.e("HomeFragment", "Erreur chargement produits", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Erreur chargement bestsellers", e)
            }
    }

    private fun loadBestsellers() {
        FirebaseFirestore.getInstance().collection("bestsellers")
            .get()
            .addOnSuccessListener { result ->
                bestsellersContainer.removeAllViews()

                for (document in result) {
                    val product = document.toObject(Product::class.java)
                    addProductView(bestsellersContainer, product)
                }
            }
    }

    private fun showProductDetails(product: Product) {
        val intent = Intent(this,DetailsActivity::class.java).apply {
            putExtra("product_id", product.id)
        }
        startActivity(intent)
    }

    private fun addToCart(product: Product) {
        // Implémentation de la logique d'ajout au panier
        Toast.makeText(this, "${product.name} ajouté au panier", Toast.LENGTH_SHORT).show()
    }

    private fun addProductView(container: LinearLayout, product: Product) {
        val inflater = LayoutInflater.from(this)
        val productView = inflater.inflate(R.layout.item_bestseller, container, false)

        // Configuration de la vue
        val imageView = productView.findViewById<ImageView>(R.id.productImage)
        val buyText = productView.findViewById<TextView>(R.id.buy_text)
        val favoriteIcon = productView.findViewById<ImageView>(R.id.favoriteIcon)

        Glide.with(this)
            .load(product.imageUrl)
            .into(imageView)

        // Gestion du clic sur "Acheter"
        buyText.setOnClickListener {
            addToCart(product)
        }

        // Gestion des favoris
        favoriteIcon.setImageResource(
            if (product.isFavorite) R.drawable.plenne
            else R.drawable.vide
        )

        favoriteIcon.setOnClickListener {
            toggleFavorite(product, favoriteIcon)
        }

        // Ajout de la vue au container
        val layoutParams = LinearLayout.LayoutParams(
            dpToPx(185), // Convertir dp en pixels
            LinearLayout.LayoutParams.MATCH_PARENT
        ).apply {
            marginEnd = dpToPx(8)
        }

        container.addView(productView, layoutParams)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * this.resources.displayMetrics.density).toInt()
    }

    private fun toggleFavorite(product: Product, icon: ImageView) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            startActivity(Intent(this, sign_in::class.java))
            return
        }

        val favoriteRef = FirebaseFirestore.getInstance()
            .collection("userFavorites")
            .document(userId)
            .collection("products")
            .document(product.id)

        if (product.isFavorite) {
            favoriteRef.delete().addOnSuccessListener {
                icon.setImageResource(R.drawable.vide)
                product.isFavorite = false
            }
        } else {
            favoriteRef.set(mapOf(
                "productId" to product.id,
                "name" to product.name,
                "timestamp" to FieldValue.serverTimestamp()
            )).addOnSuccessListener {
                icon.setImageResource(R.drawable.plenne)
                product.isFavorite = true
            }
        }
    }
}