package com.example.doceurhomeapp

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.FieldValue

data class CartItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    var quantity: Int = 1
) : Parcelable {

    // Méthode pour convertir en Map (utilisée pour Firestore)
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "price" to price,
            "imageUrl" to imageUrl,
            "quantity" to quantity
        )
    }

    // Méthode pour créer un Map de suppression (avec FieldValue)
    fun toRemovalMap(): Map<String, Any> {
        return mapOf(
            "products" to FieldValue.arrayRemove(toFirestoreMap())
        )
    }

    // Implémentation Parcelable
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readString() ?: "",
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeDouble(price)
        parcel.writeString(imageUrl)
        parcel.writeInt(quantity)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CartItem> {
        override fun createFromParcel(parcel: Parcel): CartItem {
            return CartItem(parcel)
        }

        override fun newArray(size: Int): Array<CartItem?> {
            return arrayOfNulls(size)
        }
    }
}

