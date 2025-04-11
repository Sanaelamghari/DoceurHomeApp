package com.example.doceurhomeapp

import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class paimentActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etBillingAddress: EditText
    private lateinit var etShippingAddress: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var etEmail: EditText
    private lateinit var rbCashOnDelivery: RadioButton
    private lateinit var btnConfirmPayment: Button

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dans onCreate(), avant setContentView()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paiment)

        etFullName = findViewById(R.id.etFullName)
        etBillingAddress = findViewById(R.id.etBillingAddress)
        etShippingAddress = findViewById(R.id.etShippingAddress)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        etEmail = findViewById(R.id.etEmail)
        rbCashOnDelivery = findViewById(R.id.rbCashOnDelivery)
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment)

        btnConfirmPayment.setOnClickListener {
            savePaymentInfo()
        }
        val totalAmount = intent.getStringExtra("TOTAL_AMOUNT") ?: "00.00 $"
        findViewById<TextView>(R.id.totalPriceText).apply {
            text = totalAmount
            // Style supplémentaire si besoin
            setTextColor(ContextCompat.getColor(this@paimentActivity, R.color.black))
            textSize = 24f
        }
    }

    private fun savePaymentInfo() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("PaymentActivity", "❌ Utilisateur non connecté")
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show()
            return
        }

        val fullName = etFullName.text.toString().trim()
        val billingAddress = etBillingAddress.text.toString().trim()
        val shippingAddress = etShippingAddress.text.toString().trim()
        val phoneNumber = etPhoneNumber.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val paymentMethod = if (rbCashOnDelivery.isChecked) "Paiement à la livraison" else ""

        if (fullName.isEmpty() || billingAddress.isEmpty() || phoneNumber.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs obligatoires", Toast.LENGTH_SHORT).show()
            Log.w("PaymentActivity", "⚠️ Champs vides détectés")
            return
        }

        val paymentData = hashMapOf(
            "userId" to userId,
            "fullName" to fullName,
            "billingAddress" to billingAddress,
            "shippingAddress" to if (shippingAddress.isEmpty()) billingAddress else shippingAddress,
            "phoneNumber" to phoneNumber,
            "email" to email,
            "paymentMethod" to paymentMethod,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("payments")
            .add(paymentData)
            .addOnSuccessListener { docRef ->
                Log.d("PaymentActivity", "✅ Paiement enregistré avec ID : ${docRef.id}")
                Toast.makeText(this, "Paiement confirmé", Toast.LENGTH_SHORT).show()
                finish() // Fermer l'activité après l'enregistrement
            }
            .addOnFailureListener { e ->
                Log.e("PaymentActivity", "❌ Erreur enregistrement paiement : ${e.message}", e)
                Toast.makeText(this, "Erreur lors de l'enregistrement", Toast.LENGTH_SHORT).show()
            }
    }
}
