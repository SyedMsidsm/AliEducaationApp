package com.alieducational.app.testing

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alieducational.app.data.local.AppDatabase
import com.alieducational.app.data.local.entity.CustomerEntity
import com.alieducational.app.data.local.entity.ProductEntity
import com.alieducational.app.data.local.entity.StockMovementEntity
import com.alieducational.app.data.repository.AppRepository
import com.alieducational.app.data.repository.PurchaseItemParam
import com.alieducational.app.data.repository.SaleItemParam
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Random

@RunWith(AndroidJUnit4::class)
class MoneyInvariantTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: AppRepository

    @Before
    fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = TestDatabaseProvider.createInMemory(ctx)
        repo = AppRepository(
            db,
            db.productDao(),
            db.productBarcodeDao(),
            db.stockMovementDao(),
            db.supplierDao(),
            db.customerDao(),
            db.purchaseDao(),
            db.saleDao(),
            db.accountPaymentDao(),
            db.shopExpenseDao(),
            db.heldBillDao(),
            db.pigmyDepositDao(),
            db.pigmyWithdrawalDao(),
            db.pigmySchemeDao(),
            db.cashCategoryDao(),
            db.productGiveawayDao(),
            db.serviceDefinitionDao(),
            db.syncEventDao()
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testMoneyInvariants_RandomizedPropertyBased() = runBlocking {
        val seedProperty = System.getProperty("seed")?.toLongOrNull()
        val seed = seedProperty ?: 20261109L
        val rng = Random(seed)
        val iterations = 100
        val history = mutableListOf<String>()

        // 1. Seed 3 products
        for (i in 1..3) {
            val p = ProductEntity(
                id = "prod-$i",
                name = "Product $i",
                category = "General",
                currentSellableStock = 50,
                movingAvgCostPaise = 2000L,
                sellingPricePiecePaise = 3000L
            )
            db.productDao().insertProduct(p)
            db.stockMovementDao().insertMovement(
                StockMovementEntity(
                    productId = p.id,
                    type = "OPENING_STOCK",
                    quantityChangeBase = 50,
                    unitCostPaise = 2000L,
                    reason = "Setup"
                )
            )
        }

        // 2. Seed 2 customers
        for (c in 1..2) {
            db.customerDao().insertCustomer(
                CustomerEntity(
                    id = "cust-$c",
                    name = "Customer $c",
                    currentBalancePaise = 0L
                )
            )
        }

        // 3. Initialize Pigmy Scheme
        repo.createPigmyScheme("DEFAULT_PIGMY", "Pigmy Savings", 1000L)
        repo.updatePigmySchemeOpeningBalance("DEFAULT_PIGMY", 20000L)

        // Loop iterations
        for (step in 1..iterations) {
            val op = rng.nextInt(6)
            var opDesc = "Step $step: "

            when (op) {
                0 -> {
                    // Record sale (Cash or Credit)
                    val prodId = "prod-${1 + rng.nextInt(3)}"
                    val prod = db.productDao().getProductById(prodId)!!
                    val qty = 1 + rng.nextInt(minOf(prod.currentSellableStock.coerceAtLeast(1), 5))
                    val isCredit = rng.nextBoolean()
                    val custId = if (isCredit) "cust-${1 + rng.nextInt(2)}" else null

                    val unitPrice = 3000L
                    val gross = qty * unitPrice
                    val paid = if (isCredit) gross / 2 else gross

                    if (prod.currentSellableStock >= qty) {
                        val sale = repo.recordSale(
                            customerId = custId,
                            customerName = custId,
                            items = listOf(
                                SaleItemParam(
                                    productId = prodId,
                                    productName = prod.name,
                                    quantityBase = qty,
                                    unitSellingPricePaise = unitPrice
                                )
                            ),
                            discountType = "NONE",
                            discountInput = 0.0,
                            amountReceivedPaise = paid,
                            paymentMethod = if (isCredit) "CREDIT" else "CASH",
                            notes = "Test sale step $step"
                        )
                        opDesc += "SALE ${sale.id} prod=$prodId qty=$qty credit=${sale.remainingCreditPaise}"
                    } else {
                        opDesc += "SKIP SALE (low stock)"
                    }
                }
                1 -> {
                    // Void an active sale
                    val activeSales = db.saleDao().getAllSales().first()
                    if (activeSales.isNotEmpty()) {
                        val target = activeSales[rng.nextInt(activeSales.size)]
                        val cond = if (rng.nextBoolean()) "SELLABLE" else "DAMAGED"
                        repo.voidSale(target.id, cond, "Test void step $step")
                        opDesc += "VOID ${target.id} cond=$cond"
                    } else {
                        opDesc += "SKIP VOID (no sales)"
                    }
                }
                2 -> {
                    // Receive stock (purchase)
                    val prodId = "prod-${1 + rng.nextInt(3)}"
                    val qty = 5 + rng.nextInt(10)
                    repo.receiveStock(
                        supplierId = null,
                        items = listOf(
                            PurchaseItemParam(
                                productId = prodId,
                                quantityBase = qty,
                                unitPurchasePricePaise = 2000L
                            )
                        ),
                        amountPaidPaise = qty * 2000L,
                        deliveryChargesPaise = 0L,
                        paymentMethod = "CASH",
                        notes = "Test purchase"
                    )
                    opDesc += "RECEIVE prod=$prodId qty=$qty"
                }
                3 -> {
                    // Customer repayment
                    val custId = "cust-${1 + rng.nextInt(2)}"
                    val cust = db.customerDao().getCustomerById(custId)!!
                    if (cust.currentBalancePaise > 0) {
                        val pay = minOf(cust.currentBalancePaise, (1 + rng.nextInt(5)) * 1000L)
                        repo.recordPayment(
                            accountType = "CUSTOMER",
                            accountId = custId,
                            amountPaise = pay,
                            paymentMethod = "CASH",
                            notes = "Repayment step $step"
                        )
                        opDesc += "REPAYMENT cust=$custId amount=$pay"
                    } else {
                        opDesc += "SKIP REPAYMENT"
                    }
                }
                4 -> {
                    // Pigmy deposit
                    val amt = (1 + rng.nextInt(5)) * 5000L
                    repo.recordPigmyDeposit(
                        schemeId = "DEFAULT_PIGMY",
                        amountPaise = amt,
                        paymentSource = "CASH",
                        notes = "Deposit step $step"
                    )
                    opDesc += "PIGMY_DEPOSIT +$amt"
                }
                5 -> {
                    // Pigmy withdrawal (only if balance > 0)
                    val totalDep = db.pigmyDepositDao().getTotalForScheme("DEFAULT_PIGMY").first() ?: 0L
                    val totalWith = db.pigmyWithdrawalDao().getTotalForScheme("DEFAULT_PIGMY").first() ?: 0L
                    val bal = 20000L + totalDep - totalWith
                    if (bal > 10000L) {
                        val withdraw = minOf(bal, 5000L)
                        repo.recordPigmyWithdrawal(
                            schemeId = "DEFAULT_PIGMY",
                            amountPaise = withdraw,
                            destination = "CASH",
                            notes = "Withdrawal step $step"
                        )
                        opDesc += "PIGMY_WITHDRAWAL -$withdraw"
                    } else {
                        opDesc += "SKIP PIGMY_WITHDRAWAL"
                    }
                }
            }
            history.add(opDesc)

            // ── ASSERT INVARIANTS ──────────────────────────────────────────

            // Invariant 1: Sellable stock equals sum of stock movements affecting sellable stock
            for (i in 1..3) {
                val pid = "prod-$i"
                val p = db.productDao().getProductById(pid)!!
                val movements = db.stockMovementDao().getMovementsForProduct(pid)
                val expectedStock = movements.filter { it.type != "DAMAGED" }.sumOf { it.quantityChangeBase }
                assertEquals(
                    "Invariant 1 failed for $pid at step $step (seed=$seed)\nHistory:\n${history.takeLast(10).joinToString("\n")}",
                    expectedStock,
                    p.currentSellableStock
                )
            }

            // Invariant 2: sale.netTotal = sum(line.net), net = gross - discount
            val allSales = db.saleDao().getAllSalesWithVoided().first()
            for (s in allSales) {
                val lines = db.saleDao().getLinesForSale(s.id)
                val sumNet = lines.sumOf { it.lineNetPaise }
                val sumGross = lines.sumOf { it.lineGrossPaise }
                assertEquals("Invariant 2 (net) at step $step", s.netTotalPaise, sumNet)
                assertEquals("Invariant 2 (gross) at step $step", s.grossTotalPaise, sumGross)
                assertEquals("Invariant 2 (net = gross - discount) at step $step", s.netTotalPaise, s.grossTotalPaise - s.discountValuePaise)
            }

            // Invariant 3: Customer balance equals sum of remaining credit on active sales
            for (c in 1..2) {
                val cid = "cust-$c"
                val cust = db.customerDao().getCustomerById(cid)!!
                val activeCredit = allSales.filter { !it.isVoided && it.customerId == cid }.sumOf { it.remainingCreditPaise }
                assertEquals(
                    "Invariant 3 failed for $cid at step $step (seed=$seed)\nHistory:\n${history.takeLast(10).joinToString("\n")}",
                    activeCredit,
                    cust.currentBalancePaise
                )
            }

            // Invariant 8: Pigmy balances never go negative
            val dep = db.pigmyDepositDao().getTotalForScheme("DEFAULT_PIGMY").first() ?: 0L
            val wth = db.pigmyWithdrawalDao().getTotalForScheme("DEFAULT_PIGMY").first() ?: 0L
            val pBal = 20000L + dep - wth
            assertTrue("Invariant 8: Pigmy balance >= 0 (bal=$pBal) at step $step", pBal >= 0L)
        }
    }
}
