package com.example.doceurhomeapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class connectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)
    }

    // Fonction appelée lors du clic sur le bouton "Sign up"
    fun sign_up(view: View) {
        // Créer un Intent pour passer à SignupActivity
        val intent = Intent(this, SignupActivity::class.java)
        startActivity(intent) // Démarrer l'activité SignupActivity
    }

    // Fonction appelée lors du clic sur le bouton "Sign in"
    fun sign_in(view: View) {
        // Créer un Intent pour passer à sign_inActivity
        val intent = Intent(this,sign_inActivity::class.java)
        startActivity(intent) // Démarrer l'activité sign_inActivity
    }
}


