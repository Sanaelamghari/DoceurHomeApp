package com.example.doceurhomeapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
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

    // Variables pour les composants UI
    private lateinit var etProductName: EditText
    private lateinit var etProductDescription: EditText
    private lateinit var etProductPrice: EditText
    private lateinit var etCategoryName: EditText
    private lateinit var btnSelectCategoryImage: Button
    private lateinit var btnAddCategory: Button
    private lateinit var spinnerCategories: Spinner
    private lateinit var btnAddProduct: Button

    // Variables pour la gestion des images et des données
    private var selectedCategoryImageUri: Uri? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val categories = mutableListOf<String>()
    private lateinit var categoriesAdapter: ArrayAdapter<String>

    // Constantes pour Cloudinary
    private val CLOUD_NAME = "dbmk56fhn"
    private val UPLOAD_PRESET = "doceurhome_upload"
    private val CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        // Initialisation des composants
        etProductName = findViewById(R.id.etProductName)
        etProductDescription = findViewById(R.id.etProductDescription)
        etProductPrice = findViewById(R.id.etProductPrice)
        etCategoryName = findViewById(R.id.etCategoryName)
        btnSelectCategoryImage = findViewById(R.id.btnSelectCategoryImage)
        btnAddCategory = findViewById(R.id.btnAddCategory)
        spinnerCategories = findViewById(R.id.spinnerCategories)
        btnAddProduct = findViewById(R.id.btnAddProduct)

        // Configurer l'adaptateur pour le Spinner
        categoriesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategories.adapter = categoriesAdapter

        // Charger les catégories existantes
        loadCategories()

        // Gestion des clics
        btnSelectCategoryImage.setOnClickListener {
            openImagePicker(IMAGE_PICKER_REQUEST)
        }

        btnAddCategory.setOnClickListener {
            val categoryName = etCategoryName.text.toString()
            if (categoryName.isEmpty()) {
                Toast.makeText(this, "Veuillez entrer un nom de catégorie", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedCategoryImageUri == null) {
                Toast.makeText(this, "Veuillez sélectionner une image pour la catégorie", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Upload de l'image de la catégorie vers Cloudinary
            uploadImageToCloudinary(selectedCategoryImageUri!!) { imageUrl ->
                addCategoryToFirestore(categoryName, imageUrl)
            }
        }

        btnAddProduct.setOnClickListener {
            val productName = etProductName.text.toString()
            val productDescription = etProductDescription.text.toString()
            val productPrice = etProductPrice.text.toString()
            val categoryName = spinnerCategories.selectedItem?.toString() ?: ""

            if (productName.isEmpty() || productDescription.isEmpty() || productPrice.isEmpty() || categoryName.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Enregistrer le produit dans Firestore
            saveProductToFirestore(productName, productDescription, productPrice, categoryName)
        }
    }

    // Ouvrir le sélecteur d'images
    private fun openImagePicker(requestCode: Int) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, requestCode)
    }

    // Gérer le résultat du sélecteur d'images
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                IMAGE_PICKER_REQUEST -> {
                    selectedCategoryImageUri = data?.data
                    Log.d("ImageDebug", "Image de catégorie sélectionnée: $selectedCategoryImageUri")
                }
            }
        }
    }

    // Uploader l'image vers Cloudinary
    private fun uploadImageToCloudinary(imageUri: Uri, callback: (String) -> Unit) {
        val file = getFileFromUri(imageUri) ?: return
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("upload_preset", UPLOAD_PRESET)
            .build()

        val request = Request.Builder().url(CLOUDINARY_URL).post(requestBody).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Cloudinary", "Erreur d'upload: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    val jsonResponse = JSONObject(responseData ?: "")
                    val imageUrl = jsonResponse.getString("secure_url")
                    Log.d("Cloudinary", "Image uploadée: $imageUrl")
                    callback(imageUrl)
                }
            }
        })
    }

    // Ajouter une catégorie dans Firestore
    private fun addCategoryToFirestore(categoryName: String, imageUrl: String) {
        val category = hashMapOf(
            "name" to categoryName,
            "imageUrl" to imageUrl
        )

        firestore.collection("categories")
            .add(category)
            .addOnSuccessListener {
                Log.d("Firestore", "Catégorie ajoutée avec succès")
                Toast.makeText(this, "Catégorie ajoutée avec succès", Toast.LENGTH_SHORT).show()
                loadCategories() // Recharger les catégories après ajout
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erreur ajout catégorie: ${e.message}")
                Toast.makeText(this, "Erreur lors de l'ajout de la catégorie", Toast.LENGTH_SHORT).show()
            }
    }

    // Enregistrer le produit dans Firestore
    private fun saveProductToFirestore(
        productName: String,
        productDescription: String,
        productPrice: String,
        categoryName: String
    ) {
        val product = hashMapOf(
            "name" to productName,
            "description" to productDescription,
            "price" to productPrice,
            "category" to categoryName
        )

        firestore.collection("products")
            .add(product)
            .addOnSuccessListener {
                Log.d("Firestore", "Produit ajouté avec succès")
                Toast.makeText(this, "Produit ajouté avec succès", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erreur Firestore: ${e.message}")
                Toast.makeText(this, "Erreur lors de l'ajout du produit", Toast.LENGTH_SHORT).show()
            }
    }

    // Charger les catégories depuis Firestore
    private fun loadCategories() {
        firestore.collection("categories")
            .get()
            .addOnSuccessListener { result ->
                categories.clear()
                for (document in result) {
                    val categoryName = document.getString("name") ?: ""
                    categories.add(categoryName)
                }
                categoriesAdapter.notifyDataSetChanged()
                Log.d("Firestore", "Catégories chargées: $categories")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erreur chargement catégories: ${e.message}")
            }
    }

    // Convertir Uri en File
    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(cacheDir, "temp_image.jpg")
            file.outputStream().use { inputStream.copyTo(it) }
            file
        } catch (e: Exception) {
            Log.e("FileError", "Erreur conversion URI en fichier: ${e.message}")
            null
        }
    }

    companion object {
        private const val IMAGE_PICKER_REQUEST = 1
    }
}