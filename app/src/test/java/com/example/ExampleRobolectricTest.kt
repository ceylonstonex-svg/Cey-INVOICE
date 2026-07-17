package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ProductEntity
import com.example.ui.PosCartItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Ceyvana", appName)
  }

  @Test
  fun `test PosCartItem pricing calculation green`() {
    val product = ProductEntity(
      id = 101,
      name = "Ceylon Premium Cinnamon",
      pricePerGram = 15.0,
      sku = "CIN-001"
    )
    val cartItem = PosCartItem(
      product = product,
      pricePerGram = product.pricePerGram,
      selectedWeightGrams = 250.0,
      quantity = 2
    )

    // unitPrice = 15.0 * 250.0 = 3750.0
    assertEquals(3750.0, cartItem.unitPrice, 0.001)
    // totalPrice = 3750.0 * 2 = 7500.0
    assertEquals(7500.0, cartItem.totalPrice, 0.001)
  }

  @Test
  fun `test PosCartItem with custom item`() {
    val cartItem = PosCartItem(
      product = null,
      customName = "Custom Ceylon Green Tea",
      pricePerGram = 5.0,
      selectedWeightGrams = 100.0,
      quantity = 3
    )

    assertEquals("Custom Ceylon Green Tea", cartItem.itemName)
    // unitPrice = 5.0 * 100.0 = 500.0
    assertEquals(500.0, cartItem.unitPrice, 0.001)
    // totalPrice = 500.0 * 3 = 1500.0
    assertEquals(1500.0, cartItem.totalPrice, 0.001)
  }
}
