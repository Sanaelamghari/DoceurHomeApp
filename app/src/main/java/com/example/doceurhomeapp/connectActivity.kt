package com.example.doceurhomeapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class connectActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)
        supportActionBar?.hide()

        // Initialisation Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    fun sign_up(view: View) {
        val intent = Intent(this, SignupActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun sign_in(view: View) {
        val intent = Intent(this, sign_in::class.java)
        startActivity(intent)
        finish()
    }

    // Nouvelle fonction pour le bouton Management
    fun management(view: View) {
        val intent = Intent(this, AddProductActivity::class.java)
        startActivity(intent)
    }

    // Nouvelle fonction pour le bouton Logout
    fun logout(view: View) {
        // Supprimer l'utilisateur de la collection 'users'
        auth.currentUser?.uid?.let { userId ->
            db.collection("users").document(userId)
                .delete()
                .addOnSuccessListener {
                    // Déconnexion de Firebase Auth
                    auth.signOut()

                    // Redirection vers l'écran de connexion
                    val intent = Intent(this, sign_in::class.java)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    // Gérer l'erreur
                    println("Erreur lors de la suppression: $e")
                }
        } ?: run {
            // Si pas d'utilisateur connecté, simplement rediriger
            val intent = Intent(this, sign_in::class.java)
            startActivity(intent)
            finish()
        }
    }
}


