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
    private lateinit var btnSelectDetailImages: Button
    private var selectedImageUri: Uri? = null
    private val selectedDetailImages = mutableListOf<Uri>()
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
        btnSelectDetailImages = findViewById(R.id.btnSelectDetailImages)

        btnSelectImage.setOnClickListener {
            openImagePicker(IMAGE_PICKER_REQUEST)
        }

        btnSelectDetailImages.setOnClickListener {
            openImagePicker(DETAIL_IMAGES_PICKER_REQUEST, true)
        }

        btnAddProduct.setOnClickListener {
            uploadImageToCloudinary()
        }
    }

    private fun openImagePicker(requestCode: Int, allowMultiple: Boolean = false) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        if (allowMultiple) intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                IMAGE_PICKER_REQUEST -> {
                    selectedImageUri = data?.data
                    Log.d("ImageDebug", "Image principale sélectionnée: $selectedImageUri")
                }
                DETAIL_IMAGES_PICKER_REQUEST -> {
                    data?.clipData?.let {
                        for (i in 0 until it.itemCount) {
                            selectedDetailImages.add(it.getItemAt(i).uri)
                        }
                    } ?: data?.data?.let {
                        selectedDetailImages.add(it)
                    }
                    Log.d("ImageDebug", "Images détaillées sélectionnées: $selectedDetailImages")
                }
            }
        }
    }

    private fun uploadImageToCloudinary() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Veuillez sélectionner une image principale", Toast.LENGTH_SHORT).show()
            return
        }
        uploadToCloudinary(selectedImageUri!!) { mainImageUrl ->
            uploadDetailImagesToCloudinary(mainImageUrl)
        }
    }

    private fun uploadDetailImagesToCloudinary(mainImageUrl: String) {
        val detailImageUrls = mutableListOf<String>()
        var uploadCount = 0
        if (selectedDetailImages.isEmpty()) {
            saveProductToFirestore(mainImageUrl, detailImageUrls)
            return
        }

        selectedDetailImages.forEach { uri ->
            uploadToCloudinary(uri) { imageUrl ->
                detailImageUrls.add(imageUrl)
                uploadCount++
                if (uploadCount == selectedDetailImages.size) {
                    saveProductToFirestore(mainImageUrl, detailImageUrls)
                }
            }
        }
    }

    private fun uploadToCloudinary(uri: Uri, callback: (String) -> Unit) {
        val file = getFileFromUri(uri) ?: return
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

    private fun saveProductToFirestore(imageUrl: String, detailImages: List<String>) {
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
            "imageUrl" to imageUrl,
            "detailImages" to detailImages
        )

        firestore.collection("products").add(product)
            .addOnSuccessListener {
                Log.d("Firestore", "Produit ajouté avec succès")
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erreur Firestore: ${e.message}")
            }
    }

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
        private const val DETAIL_IMAGES_PICKER_REQUEST = 2
    }
}



