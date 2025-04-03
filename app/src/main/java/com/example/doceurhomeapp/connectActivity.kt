package com.example.doceurhomeapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class connectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)

        // Ajouter ceci pour empêcher le retour automatique à MainActivity
        supportActionBar?.hide()
    }

    fun sign_up(view: View) {
        val intent = Intent(this, SignupActivity::class.java)
        startActivity(intent)
        finish() // Fermer cette activité pour éviter le retour
    }

    fun sign_in(view: View) {
        val intent = Intent(this, sign_in::class.java)
        startActivity(intent)
        finish() // Fermer cette activité pour éviter le retour
    }
}


