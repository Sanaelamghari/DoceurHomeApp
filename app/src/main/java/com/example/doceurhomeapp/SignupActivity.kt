package com.example.doceurhomeapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doceurhomeapp.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupListeners()
    }

    private fun setupListeners() {
        binding.signupButton.setOnClickListener {
            if (validateInputs()) {
                registerUser()
            }
        }


    }

    private fun validateInputs(): Boolean {
        val fullName = binding.fullName.text.toString().trim()
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()
        val phoneNumber = binding.phoneNumber.text.toString().trim()


        // Réinitialiser les erreurs
        listOf(binding.fullName, binding.email, binding.password, binding.phoneNumber)
            .forEach { it.error = null }

        var isValid = true

        when {
            fullName.isEmpty() -> {
                binding.fullName.error = "Le nom complet est requis"
                isValid = false
            }
            fullName.length < 3 -> {
                binding.fullName.error = "Nom trop court (min 3 caractères)"
                isValid = false
            }
        }

        when {
            email.isEmpty() -> {
                binding.email.error = "Email requis"
                isValid = false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.email.error = "Format d'email invalide"
                isValid = false
            }
        }

        when {
            password.isEmpty() -> {
                binding.password.error = "Mot de passe requis"
                isValid = false
            }
            password.length < 8 -> {
                binding.password.error = "Minimum 8 caractères"
                isValid = false
            }
            !password.matches(".*[A-Z].*".toRegex()) -> {
                binding.password.error = "Doit contenir une majuscule"
                isValid = false
            }
            !password.matches(".*[0-9].*".toRegex()) -> {
                binding.password.error = "Doit contenir un chiffre"
                isValid = false
            }
        }

        when {
            phoneNumber.isEmpty() -> {
                binding.phoneNumber.error = "Numéro requis"
                isValid = false
            }
            !phoneNumber.matches(Regex("^[0-9]{10,15}$")) -> {
                binding.phoneNumber.error = "Numéro invalide (10-15 chiffres)"
                isValid = false
            }
        }



        return isValid
    }

    private fun registerUser() {
        val fullName = binding.fullName.text.toString().trim()
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()
        val phoneNumber = binding.phoneNumber.text.toString().trim()


        // UI State - Loading
        binding.signupButton.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Création du compte Firebase Auth
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user ?: throw Exception("User object is null")

                // 2. Mise à jour du profil avec le nom complet
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .build()

                user.updateProfile(profileUpdates).await()

                // 3. Envoi de l'email de vérification
                user.sendEmailVerification().await()

                // 4. Sauvegarde des données supplémentaires dans Firestore
                val userData = hashMapOf(
                    "id" to user.uid,
                    "nom" to fullName,
                    "email" to email,
                    "phoneNumber" to phoneNumber,
                    "createdAt" to System.currentTimeMillis(),
                    "emailVerified" to false
                )

                db.collection("users").document(user.uid).set(userData).await()

                // Succès
                withContext(Dispatchers.Main) {
                    showSuccessAndNavigate()
                }

            } catch (e: FirebaseAuthUserCollisionException) {
                withContext(Dispatchers.Main) {
                    binding.email.error = "Cet email est déjà utilisé"
                    resetFormState()
                }
            } catch (e: FirebaseAuthWeakPasswordException) {
                withContext(Dispatchers.Main) {
                    binding.password.error = "Mot de passe faible: ${e.reason}"
                    resetFormState()
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                withContext(Dispatchers.Main) {
                    binding.email.error = "Email invalide"
                    resetFormState()
                }
            } catch (e: FirebaseFirestoreException) {
                withContext(Dispatchers.Main) {
                    when (e.code) {
                        FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                            Toast.makeText(
                                this@SignupActivity,
                                "Erreur de permission. Contactez l'administrateur",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            Toast.makeText(
                                this@SignupActivity,
                                "Erreur de base de données: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    resetFormState()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SignupActivity,
                        "Erreur: ${e.localizedMessage ?: "Une erreur inconnue est survenue"}",
                        Toast.LENGTH_LONG
                    ).show()
                    resetFormState()
                }
            }
        }
    }

    private fun showSuccessAndNavigate() {
        Toast.makeText(
            this,
            "Inscription réussie! Veuillez vérifier votre email",
            Toast.LENGTH_LONG
        ).show()

        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, sign_in::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun resetFormState() {
        binding.progressBar.visibility = View.GONE
        binding.signupButton.isEnabled = true
    }
}
