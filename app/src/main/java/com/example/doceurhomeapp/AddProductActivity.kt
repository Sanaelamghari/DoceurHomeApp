package com.example.doceurhomeapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

class AddProductActivity : AppCompatActivity() {

    private lateinit var etProductName: EditText
    private lateinit var etProductDescription: EditText
    private lateinit var etProductPrice: EditText
    private lateinit var btnSelectImage: Button
    private lateinit var btnAddProduct: Button
    private var selectedImageUri: Uri? = null
    private val firestore = FirebaseFirestore.getInstance()

    private val CLOUD_NAME = "dbmk56fhn"
    private val UPLOAD_PRESET = "doceurhome_upload"
    private val CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        etProductName = findViewById(R.id.etProductName)
        etProductDescription = findViewById(R.id.etProductDescription)
        etProductPrice = findViewById(R.id.etProductPrice)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnAddProduct = findViewById(R.id.btnAddProduct)

        btnSelectImage.setOnClickListener {
            openImagePicker()
        }

        btnAddProduct.setOnClickListener {
            uploadImageToCloudinary()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICKER_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICKER_REQUEST && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            if (selectedImageUri != null) {
                Log.d("ImageDebug", "Image sélectionnée : $selectedImageUri")
                Toast.makeText(this, "Image sélectionnée", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("ImageDebug", "Aucune image sélectionnée")
            }
        }
    }

    private fun uploadImageToCloudinary() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Veuillez sélectionner une image", Toast.LENGTH_SHORT).show()
            return
        }

        val file = getFileFromUri(selectedImageUri!!)
        if (file == null) {
            Toast.makeText(this, "Erreur lors de la récupération du fichier", Toast.LENGTH_SHORT).show()
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("upload_preset", UPLOAD_PRESET)
            .build()

        val request = Request.Builder()
            .url(CLOUDINARY_URL)
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Cloudinary", "Erreur d'upload : ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@AddProductActivity, "Erreur d'upload de l'image", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    val jsonResponse = JSONObject(responseData ?: "")
                    val imageUrl = jsonResponse.getString("secure_url")

                    Log.d("Cloudinary", "Image uploadée : $imageUrl")
                    runOnUiThread {
                        Toast.makeText(this@AddProductActivity, "Image uploadée !", Toast.LENGTH_SHORT).show()
                        saveProductToFirestore(imageUrl)
                    }
                } else {
                    Log.e("Cloudinary", "Réponse invalide : ${response.message}")
                    runOnUiThread {
                        Toast.makeText(this@AddProductActivity, "Erreur lors de l'upload", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun saveProductToFirestore(imageUrl: String) {
        val productName = etProductName.text.toString()
        val productDescription = etProductDescription.text.toString()
        val productPrice = etProductPrice.text.toString()

        if (productName.isEmpty() || productDescription.isEmpty() || productPrice.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            return
        }

        val product = hashMapOf(
            "name" to productName,
            "description" to productDescription,
            "price" to productPrice,
            "imageUrl" to imageUrl
        )

        firestore.collection("products")
            .add(product)
            .addOnSuccessListener {
                Toast.makeText(this, "Produit ajouté avec succès", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Erreur Firestore : ${e.message}")
                Toast.makeText(this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(cacheDir, "temp_image.jpg")
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            file
        } catch (e: Exception) {
            Log.e("FileError", "Erreur lors de la conversion de l'URI en fichier : ${e.message}")
            null
        }
    }

    companion object {
        private const val IMAGE_PICKER_REQUEST = 1
    }
}


