package com.example.data

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GoogleDriveService(private val context: Context) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val driveScope = "https://www.googleapis.com/auth/drive.file"

    val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(driveScope))
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    suspend fun getAccessToken(account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        try {
            GoogleAuthUtil.getToken(
                context,
                account.account ?: throw java.io.IOException("No account found"),
                "oauth2:$driveScope"
            )
        } catch (e: Exception) {
            Log.e("GoogleDriveService", "Failed to retrieve OAuth token", e)
            null
        }
    }

    fun signOut(onComplete: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
            onComplete()
        }
    }

    /**
     * Searches for a file by name. Returns file ID if found, null otherwise.
     */
    suspend fun searchFileByName(accessToken: String, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/drive/v3/files?q=name='${fileName}' and trashed=false"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val files = json.getJSONArray("files")
                    if (files.length() > 0) {
                        return@withContext files.getJSONObject(0).getString("id")
                    }
                } else {
                    Log.e("GoogleDriveService", "Search failed: ${response.code} ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveService", "Search file error", e)
        }
        null
    }

    /**
     * Uploads a file or updates it if it already exists.
     */
    suspend fun uploadOrUpdateFile(
        accessToken: String,
        fileName: String,
        mimeType: String,
        fileData: ByteArray
    ): String? = withContext(Dispatchers.IO) {
        try {
            val existingFileId = searchFileByName(accessToken, fileName)

            val boundary = "foo_bar_boundary"
            val metadataJson = JSONObject().apply {
                put("name", fileName)
                put("mimeType", mimeType)
            }

            val multipartBody = MultipartBody.Builder(boundary)
                .setType("multipart/related".toMediaType())
                .addPart(
                    Headers.Builder().add("Content-Type", "application/json; charset=UTF-8").build(),
                    metadataJson.toString().toRequestBody("application/json".toMediaType())
                )
                .addPart(
                    Headers.Builder().add("Content-Type", mimeType).build(),
                    fileData.toRequestBody(mimeType.toMediaType())
                )
                .build()

            val url = if (existingFileId != null) {
                "https://www.googleapis.com/upload/drive/v3/files/${existingFileId}?uploadType=multipart"
            } else {
                "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")

            if (existingFileId != null) {
                requestBuilder.patch(multipartBody)
            } else {
                requestBuilder.post(multipartBody)
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    json.optString("id", null)
                } else {
                    Log.e("GoogleDriveService", "Upload failed: ${response.code} ${response.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveService", "Upload file error", e)
            null
        }
    }

    /**
     * Downloads file contents by file ID.
     */
    suspend fun downloadFile(accessToken: String, fileId: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/drive/v3/files/${fileId}?alt=media"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else {
                    Log.e("GoogleDriveService", "Download failed: ${response.code} ${response.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveService", "Download file error", e)
            null
        }
    }

    /**
     * Helper to serialize app database state to a pretty JSON string.
     */
    fun exportDatabaseToJson(
        invoicesList: List<InvoiceWithLineItems>,
        productsList: List<ProductEntity>
    ): String {
        val backupJson = JSONObject()
        
        // Invoices array
        val invoicesArray = JSONArray()
        for (invoiceWithItems in invoicesList) {
            val inv = invoiceWithItems.invoice
            val items = invoiceWithItems.lineItems
            
            val invJson = JSONObject().apply {
                put("id", inv.id)
                put("invoiceNumber", inv.invoiceNumber)
                put("invoiceDate", inv.invoiceDate)
                put("dueDate", inv.dueDate)
                put("customerName", inv.customerName)
                put("customerEmail", inv.customerEmail)
                put("customerPhone", inv.customerPhone)
                put("currencyCode", inv.currencyCode)
                put("currencySymbol", inv.currencySymbol)
                put("subTotal", inv.subTotal)
                put("discount", inv.discount)
                put("deliveryCharge", inv.deliveryCharge)
                put("taxRate", inv.taxRate)
                
                val itemsArray = JSONArray()
                for (item in items) {
                    val itemJson = JSONObject().apply {
                        put("id", item.id)
                        put("invoiceId", item.invoiceId)
                        put("description", item.description)
                        put("quantity", item.quantity)
                        put("unitPrice", item.unitPrice)
                        put("unit", item.unit)
                    }
                    itemsArray.put(itemJson)
                }
                put("lineItems", itemsArray)
            }
            invoicesArray.put(invJson)
        }
        backupJson.put("invoices", invoicesArray)
        
        // Products array
        val productsArray = JSONArray()
        for (prod in productsList) {
            val prodJson = JSONObject().apply {
                put("id", prod.id)
                put("name", prod.name)
                put("pricePerGram", prod.pricePerGram)
                put("defaultUnit", prod.defaultUnit)
            }
            productsArray.put(prodJson)
        }
        backupJson.put("products", productsArray)
        
        return backupJson.toString(2)
    }
}
