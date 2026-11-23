package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.PaymentStatus
import com.example.data.SalesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: SalesRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SalesRepository(db.salesDao(), db.expenseDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("دفتر المبيعات", appName)
    }

    @Test
    fun `partial and full payment lifecycle updates remaining balance and status`() = runBlocking {
        val saleId = repository.addSale(
            customerName = "أحمد محمود",
            totalAmount = 250.0,
            initialPaymentAmount = 100.0,
            note = "بضاعة",
            createdAtMillis = System.currentTimeMillis(),
        )

        var allSales = repository.observeAllSales().first()
        assertEquals(1, allSales.size)
        assertEquals(PaymentStatus.PARTIAL, allSales.first().status)
        assertEquals(150.0, allSales.first().remainingAmount, 0.01)

        repository.addPayment(saleId = saleId, amount = 150.0)
        allSales = repository.observeAllSales().first()
        assertEquals(PaymentStatus.PAID, allSales.first().status)
        assertEquals(0.0, allSales.first().remainingAmount, 0.01)
    }
}
