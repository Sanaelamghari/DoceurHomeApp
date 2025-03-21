package com.example.doceurhomeapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.doceurhomeapp.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class sign_in : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisation du View Binding
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialisation Firebase Auth et Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Vérifier si un utilisateur est déjà connecté
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Si l'utilisateur est déjà connecté, on vérifie s'il est admin ou client
            checkUserRoleAndRedirect(currentUser.email)
        }

        // Clic sur le bouton "Se connecter"
        binding.signinButton.setOnClickListener {
            loginUser()
        }
    }

    private fun loginUser() {
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            return
        }

        // Vérifier l'authentification avec Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("sign_in", "Connexion réussie pour l'utilisateur : $email")
                    Toast.makeText(this, "Connexion réussie !", Toast.LENGTH_SHORT).show()

                    // Vérifier si l'utilisateur est un administrateur
                    checkUserRoleAndRedirect(email)
                } else {
                    Log.e("sign_in", "Échec de connexion : ${task.exception?.message}")
                    Toast.makeText(this, "Échec de connexion : ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun checkUserRoleAndRedirect(email: String?) {
        if (email == null) {
            Log.e("sign_in", "L'email de l'utilisateur est null")
            Toast.makeText(this, "Erreur : email non trouvé", Toast.LENGTH_SHORT).show()
            return
        }

        // Vérifier si l'utilisateur est un administrateur dans la collection "idAdmin"
        db.collection("idAdmin")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("sign_in", "L'utilisateur $email est un client")
                    // Rediriger vers ProductsActivity pour les clients
                    redirectToProductsActivity()
                } else {
                    Log.d("sign_in", "L'utilisateur $email est un administrateur")
                    // Afficher une alerte pour demander le choix de connexion
                    showRoleSelectionAlert()
                }
            }
            .addOnFailureListener { e ->
                Log.e("sign_in", "Erreur lors de la vérification du rôle : ${e.message}")
                Toast.makeText(this, "Erreur lors de la vérification du rôle", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showRoleSelectionAlert() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Choisir un rôle")
            .setMessage("Voulez-vous vous connecter en tant qu'administrateur ou client ?")
            .setPositiveButton("Administrateur") { _, _ ->
                Log.d("sign_in", "L'utilisateur a choisi de se connecter en tant qu'administrateur")
                redirectToAddProductActivity()
            }
            .setNegativeButton("Client") { _, _ ->
                Log.d("sign_in", "L'utilisateur a choisi de se connecter en tant que client")
                redirectToProductsActivity()
            }
            .setCancelable(false) // Empêche la fermeture de l'alerte sans choix
            .create()

        alertDialog.show()
    }

    private fun redirectToAddProductActivity() {
        val intent = Intent(this, AddProductActivity::class.java)
        startActivity(intent)
        finish() // Ferme cette activité pour éviter de revenir en arrière
    }

    private fun redirectToProductsActivity() {
        val intent = Intent(this, ProductsActivity::class.java)
        startActivity(intent)
        finish() // Ferme cette activité pour éviter de revenir en arrière
    }
}

