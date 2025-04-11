package com.example.doceurhomeapp

import android.annotation.SuppressLint
import android.content.Intent
import androidx.activity.viewModels
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText

import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class AddProductActivity : AppCompatActivity() {


    // Variables pour les composants UI

    private lateinit var etProductName: EditText
    private lateinit var etProductDescription: EditText
    private lateinit var etProductPrice: EditText
    private lateinit var etCategoryName: EditText
    private lateinit var btnSelectCategoryImage: Button
    private lateinit var btnSelectProductImage: Button
    private lateinit var btnSelectDetailImages: Button
    private lateinit var btnAddCategory: Button
    private lateinit var spinnerCategories: Spinner
    private lateinit var btnAddProduct: Button
    private lateinit var btnManageBestsellers: Button
    private lateinit var etBestseller: CheckBox
    // Variables pour la gestion des images et des données
    private var selectedCategoryImageUri: Uri? = null
    private var selectedProductImageUri: Uri? = null
    private var selectedDetailImageUris: MutableList<Uri> = mutableListOf()
    private val firestore = FirebaseFirestore.getInstance()
    private val categories = mutableListOf<String>()
    private lateinit var categoriesAdapter: ArrayAdapter<String>
    // Constantes pour Cloudinary
    private val CLOUD_NAME = "dbmk56fhn"
    private val UPLOAD_PRESET = "doceurhome_upload"
    private lateinit var spinnerProducts: Spinner
    private val products = mutableListOf<String>()
    private lateinit var productsAdapter: ArrayAdapter<String>
    private val CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)
        // Initialisation du bouton (ajoutez avec les autres findViewById)
        btnManageBestsellers = findViewById(R.id.btnManageBestsellers)

        //etBestseller = findViewById(R.id.etBestseller)

        // Initialisation des composants
        etProductName = findViewById(R.id.etProductName)
        etProductDescription = findViewById(R.id.etProductDescription)
        etProductPrice = findViewById(R.id.etProductPrice)
        etCategoryName = findViewById(R.id.etCategoryName)
        btnSelectCategoryImage = findViewById(R.id.btnSelectCategoryImage)
        btnSelectProductImage = findViewById(R.id.btnSelectImage)
        btnSelectDetailImages = findViewById(R.id.btnSelectDetailImages)
        btnAddCategory = findViewById(R.id.btnAddCategory)
        spinnerCategories = findViewById(R.id.spinnerCategories)
        btnAddProduct = findViewById(R.id.btnAddProduct)

        // Configurer l'adaptateur pour le Spinner
        categoriesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategories.adapter = categoriesAdapter

        spinnerProducts = findViewById(R.id.spinnerProducts)
        productsAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, products)
        productsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProducts.adapter = productsAdapter

        // Charger les catégories existantes
        loadCategories()
        loadProducts()

        // Gestion des clics
        btnSelectCategoryImage.setOnClickListener {
            openImagePicker(IMAGE_PICKER_REQUEST_CATEGORY)
        }

        btnSelectProductImage.setOnClickListener {
            openImagePicker(IMAGE_PICKER_REQUEST_PRODUCT)
        }

        btnSelectDetailImages.setOnClickListener {
            openMultiImagePicker()
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
            Log.d("AddProduct", "Bouton Ajouter Produit cliqué")
            val productName = etProductName.text.toString()
            val productDescription = etProductDescription.text.toString()
            val productPrice = etProductPrice.text.toString()
            val categoryName = spinnerCategories.selectedItem?.toString() ?: ""

            // Vérifier les champs obligatoires
            if (productName.isEmpty() || productDescription.isEmpty() || productPrice.isEmpty() || categoryName.isEmpty()) {
                Log.e("AddProduct", "Champs manquants: Nom=$productName, Description=$productDescription, Prix=$productPrice, Catégorie=$categoryName")
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Vérifier l'image du produit
            if (selectedProductImageUri == null) {
                Log.e("AddProduct", "Aucune image de produit sélectionnée")
                Toast.makeText(this, "Veuillez sélectionner une image pour le produit", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Upload de l'image du produit vers Cloudinary
            uploadImageToCloudinary(selectedProductImageUri!!) { imageUrl ->
                Log.d("AddProduct", "Image du produit uploadée: $imageUrl")

                // Upload des images détaillées vers Cloudinary
                uploadDetailImages { detailImageUrls ->
                    Log.d("AddProduct", "Images détaillées uploadées: $detailImageUrls")

                    // Enregistrer le produit dans Firestore
                    saveProductToFirestore(productName, productDescription, productPrice, categoryName, imageUrl, detailImageUrls)
                }
            }
        }

        btnManageBestsellers.setOnClickListener {
            val selectedProductName = spinnerProducts.selectedItem?.toString() ?: run {
                Toast.makeText(this, "Sélectionnez un produit", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Récupérez les infos du produit depuis Firestore
            firestore.collection("products")
                .whereEqualTo("name", selectedProductName)
                .limit(1)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        toggleBestsellerStatus(
                            document.id,
                            document.getString("name") ?: "",
                            document.getString("imageUrl") ?: ""
                        )
                    }
                }
        }

    }

    // Ouvrir le sélecteur d'images
    private fun openImagePicker(requestCode: Int) {
        Log.d("ImagePicker", "Ouverture du sélecteur d'images")
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, requestCode)
    }

    // Ouvrir le sélecteur d'images multiples
    private fun openMultiImagePicker() {
        Log.d("ImagePicker", "Ouverture du sélecteur d'images multiples")
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICKER_REQUEST_DETAIL)
    }

    // Gérer le résultat du sélecteur d'images
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                IMAGE_PICKER_REQUEST_CATEGORY -> {
                    selectedCategoryImageUri = data?.data
                    if (selectedCategoryImageUri != null) {
                        Log.d("ImagePicker", "Image de catégorie sélectionnée: $selectedCategoryImageUri")
                    } else {
                        Log.e("ImagePicker", "Aucune image de catégorie sélectionnée")
                    }
                }
                IMAGE_PICKER_REQUEST_PRODUCT -> {
                    selectedProductImageUri = data?.data
                    if (selectedProductImageUri != null) {
                        Log.d("ImagePicker", "Image de produit sélectionnée: $selectedProductImageUri")
                    } else {
                        Log.e("ImagePicker", "Aucune image de produit sélectionnée")
                    }
                }
                IMAGE_PICKER_REQUEST_DETAIL -> {
                    selectedDetailImageUris.clear()
                    val clipData = data?.clipData
                    if (clipData != null) {
                        // Plusieurs images sélectionnées
                        for (i in 0 until clipData.itemCount) {
                            val imageUri = clipData.getItemAt(i).uri
                            selectedDetailImageUris.add(imageUri)
                        }
                    } else if (data?.data != null) {
                        // Une seule image sélectionnée
                        selectedDetailImageUris.add(data.data!!)
                    }
                    Log.d("ImagePicker", "Images détaillées sélectionnées: $selectedDetailImageUris")
                }
            }
        } else {
            Log.e("ImagePicker", "Résultat du sélecteur d'images non valide: resultCode=$resultCode")
        }
    }

    // Uploader l'image vers Cloudinary
    private fun uploadImageToCloudinary(imageUri: Uri, callback: (String) -> Unit) {
        Log.d("Cloudinary", "Début de l'upload vers Cloudinary")
        val file = getFileFromUri(imageUri) ?: run {
            Log.e("Cloudinary", "Échec de la conversion de l'URI en fichier")
            return
        }

        Log.d("Cloudinary", "Taille du fichier: ${file.length()} bytes")

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("upload_preset", UPLOAD_PRESET)
            .build()

        val request = Request.Builder().url(CLOUDINARY_URL).post(requestBody).build()
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Cloudinary", "Échec de l'upload: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    Log.d("Cloudinary", "Réponse de Cloudinary: $responseData")
                    val jsonResponse = JSONObject(responseData ?: "")
                    val imageUrl = jsonResponse.getString("secure_url")
                    Log.d("Cloudinary", "Upload réussi: $imageUrl")
                    callback(imageUrl)
                } else {
                    Log.e("Cloudinary", "Échec de la réponse: ${response.code} - ${response.message}")
                }
            }
        })
    }

    // Uploader les images détaillées vers Cloudinary
    private fun uploadDetailImages(callback: (List<String>) -> Unit) {
        Log.d("Cloudinary", "Début de l'upload des images détaillées")
        val detailImageUrls = mutableListOf<String>()
        val totalImages = selectedDetailImageUris.size
        var uploadedImages = 0

        selectedDetailImageUris.forEach { uri ->
            uploadImageToCloudinary(uri) { imageUrl ->
                detailImageUrls.add(imageUrl)
                uploadedImages++
                if (uploadedImages == totalImages) {
                    Log.d("Cloudinary", "Toutes les images détaillées uploadées: $detailImageUrls")
                    callback(detailImageUrls)
                }
            }
        }
    }

    // Ajouter une catégorie dans Firestore
    private fun addCategoryToFirestore(categoryName: String, imageUrl: String) {
        Log.d("Firestore", "Ajout de la catégorie: $categoryName")
        val category = hashMapOf(
            "name" to categoryName,
            "imageUrl" to imageUrl
        )

        firestore.collection("categories")
            .add(category)
            .addOnSuccessListener {
                Log.d("Firestore", "Catégorie ajoutée avec succès")
                Toast.makeText(this, "Catégorie ajoutée avec succès", Toast.LENGTH_SHORT).show()
                loadCategories()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Échec de l'ajout de la catégorie: ${e.message}")
                Toast.makeText(this, "Erreur lors de l'ajout de la catégorie", Toast.LENGTH_SHORT).show()
            }
    }

    // Enregistrer le produit dans Firestore
    // Modifiez la méthode saveProductToFirestore pour mettre à jour la liste après ajout
    private fun saveProductToFirestore(
        productName: String,
        productDescription: String,
        productPrice: String,
        categoryName: String,
        productImageUrl: String,
        detailImageUrls: List<String>
    ) {
        Log.d("Firestore", "Ajout du produit: $productName")
        val product = hashMapOf(
            "name" to productName,
            "description" to productDescription,
            "price" to productPrice,
            "category" to categoryName,
            "imageUrl" to productImageUrl,
            "detailImageUrls" to detailImageUrls
        )

        firestore.collection("products")
            .add(product)
            .addOnSuccessListener {
                Log.d("Firestore", "Produit ajouté avec succès")
                Toast.makeText(this, "Produit ajouté avec succès", Toast.LENGTH_SHORT).show()
                loadProducts() // Recharger la liste des produits
                loadCategories() // Recharger les catégories au cas où
                clearForm() // Optionnel: vider le formulaire
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Échec de l'ajout du produit: ${e.message}")
                Toast.makeText(this, "Erreur lors de l'ajout du produit", Toast.LENGTH_SHORT).show()
            }
    }

    // Ajoutez cette méthode pour vider le formulaire (optionnel)
    private fun clearForm() {
        etProductName.text.clear()
        etProductDescription.text.clear()
        etProductPrice.text.clear()
        selectedProductImageUri = null
        selectedDetailImageUris.clear()
    }
    // Charger les catégories depuis Firestore
    private fun loadCategories() {
        Log.d("Firestore", "Chargement des catégories")
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
                Log.e("Firestore", "Échec du chargement des catégories: ${e.message}")
            }
    }

    private fun loadProducts() {
        Log.d("Firestore", "Chargement des produits")
        firestore.collection("products")
            .get()
            .addOnSuccessListener { result ->
                products.clear()
                for (document in result) {
                    val productName = document.getString("name") ?: ""
                    products.add(productName)
                }
                productsAdapter.notifyDataSetChanged()
                Log.d("Firestore", "Produits chargés: $products")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Échec du chargement des produits: ${e.message}")
            }
    }


    // Modifiez la méthode saveProductToFirestore pour mettre à jour la liste après ajout


    // Ajoutez cette méthode pour vider le formulaire (optionnel)


    // Convertir Uri en File
    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(cacheDir, "temp_image.jpg")
            file.outputStream().use { inputStream.copyTo(it) }
            if (file.exists() && file.length() > 0) {
                file
            } else {
                Log.e("FileError", "Le fichier est vide ou n'existe pas")
                null
            }
        } catch (e: Exception) {
            Log.e("FileError", "Erreur de conversion URI en fichier: ${e.message}")
            null
        }

    }

    private fun toggleBestsellerStatus(productId: String, productName: String, imageUrl: String) {
        val bestsellerRef = firestore.collection("bestsellers").document(productId)

        bestsellerRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (task.result?.exists() == true) {
                    // Retirer des bestsellers
                    bestsellerRef.delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Produit retiré des bestsellers", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Ajouter aux bestsellers
                    val bestsellerData = hashMapOf(
                        "productId" to productId,
                        "name" to productName,
                        "imageUrl" to imageUrl,
                        "timestamp" to FieldValue.serverTimestamp()
                    )

                    bestsellerRef.set(bestsellerData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Produit ajouté aux bestsellers", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val IMAGE_PICKER_REQUEST_CATEGORY = 1
        private const val IMAGE_PICKER_REQUEST_PRODUCT = 2
        private const val IMAGE_PICKER_REQUEST_DETAIL = 3
    }
}