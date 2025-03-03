package com.example.doceurhomeapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doceurhomeapp.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth

class sign_inActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisation du View Binding
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialisation Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Vérifier si un utilisateur est déjà connecté
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Si l'utilisateur est déjà connecté, on le redirige vers MycartActivity
            startActivity(Intent(this, MycartActivity::class.java))
            finish() // Ferme l'activité actuelle pour éviter le retour en arrière
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
                    Toast.makeText(this, "Connexion réussie !", Toast.LENGTH_SHORT).show()
                    // Rediriger vers MycartActivity après connexion réussie
                   val intent = Intent(this, MycartActivity::class.java)
                    startActivity(intent)
                    finish() // Ferme cette activité pour éviter de revenir en arrière
                } else {
                    Toast.makeText(this, "Échec de connexion : ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}

