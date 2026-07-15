package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class InternetService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    fun isLocalNetworkConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun verifyWanAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://www.google.com/generate_204")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.requestMethod = "GET"
            connection.connect()
            val responseCode = connection.responseCode
            responseCode == 204
        } catch (e: Exception) {
            Log.e("InternetService", "WAN check failed", e)
            false
        }
    }

    suspend fun syncInvoice(invoiceWithItems: InvoiceWithLineItems): Boolean = withContext(Dispatchers.IO) {
        try {
            val invoice = invoiceWithItems.invoice
            val items = invoiceWithItems.lineItems

            val jsonObject = JSONObject().apply {
                put("id", invoice.id)
                put("invoiceNumber", invoice.invoiceNumber)
                put("invoiceDate", invoice.invoiceDate)
                put("dueDate", invoice.dueDate)
                put("customerName", invoice.customerName)
                put("customerEmail", invoice.customerEmail)
                put("customerPhone", invoice.customerPhone)
                put("currencyCode", invoice.currencyCode)
                put("currencySymbol", invoice.currencySymbol)
                put("subTotal", invoice.subTotal)
                put("discount", invoice.discount)
                put("deliveryCharge", invoice.deliveryCharge)
                put("taxRate", invoice.taxRate)
                put("taxAmount", invoiceWithItems.taxAmount)
                put("grandTotal", invoiceWithItems.grandTotal)

                val itemsArray = JSONArray()
                for (item in items) {
                    val itemJson = JSONObject().apply {
                        put("id", item.id)
                        put("invoiceId", item.invoiceId)
                        put("description", item.description)
                        put("quantity", item.quantity)
                        put("unitPrice", item.unitPrice)
                        put("total", item.quantity * item.unitPrice)
                    }
                    itemsArray.put(itemJson)
                }
                put("lineItems", itemsArray)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonObject.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://httpbin.org/post")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("InternetService", "Invoice sync failed", e)
            false
        }
    }
}
