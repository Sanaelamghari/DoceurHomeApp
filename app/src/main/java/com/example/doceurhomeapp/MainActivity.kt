package com.example.doceurhomeapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore



class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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
        val cartImage: ImageView = findViewById(R.id.cart_img)
        val buyText: TextView = findViewById(R.id.buy_text)
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Clic sur le panier et "Buy"
        cartImage.setOnClickListener { navigateTo(connectActivity::class.java) }
        buyText.setOnClickListener { navigateTo(connectActivity::class.java) }

        // Configuration de la barre de navigation en bas
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> true // Déjà sur la page principale
                R.id.nav_list -> {
                    navigateTo(CategoryActivity::class.java)
                    true
                }
                R.id.nav_profile -> {
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
}
