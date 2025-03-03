package com.example.doceurhomeapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doceurhomeapp.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Vérification de View Binding
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialisation Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Clic sur le bouton "Sign Up"
        binding.signupButton.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val fullName = binding.fullName.text.toString().trim()
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()
        val phoneNumber = binding.phoneNumber.text.toString().trim()
        val address = binding.address.text.toString().trim()

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || phoneNumber.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""

                    val user = hashMapOf(
                        "id" to userId,
                        "nom" to fullName,
                        "email" to email,
                        "phoneNumber" to phoneNumber,
                        "address" to address
                    )

                    db.collection("users").document(userId)
                        .set(user)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Inscription réussie !", Toast.LENGTH_SHORT).show()

                            //Redirection vers ProductsActivity
                            val intent = Intent(this, ProductsActivity::class.java)
                            startActivity(intent)
                            finish() // Fermer SignupActivity pour éviter un retour en arrière
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Erreur lors de l'inscription", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Échec de l'inscription : ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}

