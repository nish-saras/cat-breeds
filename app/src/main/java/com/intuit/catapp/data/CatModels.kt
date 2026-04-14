package com.intuit.catapp.data

import com.google.gson.annotations.SerializedName

data class Breed(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("reference_image_id") val referenceImageId: String,
    // Added description field for task 2 - nullable  because he API
    // does not guarantee this field exists for every breed
    @SerializedName("description") val description: String? = null
) {
    fun getImageUrl(): String {
        return "https://cdn2.thecatapi.com/images/${referenceImageId}.jpg"
    }
}
