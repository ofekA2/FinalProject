package com.example.finalproject

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File

private interface CloudinaryApi {
    @Multipart
    @POST("image/upload")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("upload_preset") uploadPreset: RequestBody
    ): Response<CloudinaryUploadResponse>
}

private data class CloudinaryUploadResponse(
    val secure_url: String?
)

object CloudinaryUploader {

    private fun api(context: Context): CloudinaryApi {
        val cloudName = context.getString(R.string.cloudinary_cloud_name)
        return Retrofit.Builder()
            .baseUrl("https://api.cloudinary.com/v1_1/$cloudName/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudinaryApi::class.java)
    }

    suspend fun upload(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val preset = context.getString(R.string.cloudinary_unsigned_preset)

        val temp = File.createTempFile("upload_", ".tmp", context.cacheDir)
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open input stream for URI" }
            temp.outputStream().use { out -> input.copyTo(out) }
        }

        val fileBody = temp.asRequestBody(mime.toMediaType())
        val filePart = MultipartBody.Part.createFormData("file", temp.name, fileBody)
        val presetBody = preset.toRequestBody("text/plain".toMediaType())

        val resp = api(context).uploadImage(filePart, presetBody)
        if (!resp.isSuccessful) {
            throw Exception("Cloudinary HTTP ${resp.code()} ${resp.errorBody()?.string() ?: ""}")
        }
        val url = resp.body()?.secure_url ?: throw Exception("Missing secure_url in response")
        url
    }
}