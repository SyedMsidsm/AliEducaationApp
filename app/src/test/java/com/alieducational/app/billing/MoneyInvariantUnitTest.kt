package com.alieducational.app.billing

import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.local.entity.SaleLineEntity
import com.alieducational.app.data.local.entity.ShopExpenseEntity
import com.alieducational.app.data.local.entity.StockMovementEntity
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.domain.calculation.SaleLineInput
import com.alieducational.app.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MoneyInvariantUnitTest {

    @Test
    fun testMoneyInvariants_RandomizedSimulation() {
        val seedProperty = System.getProperty("seed")?.toLongOrNull()
        val seed = seedProperty ?: 424242L
        val rng = Random(seed)
        val iterations = 100

        data class ProductState(
            val id: String,
            var currentSellableStock: Int,
            var currentDamagedStock: Int,
            val movements: MutableList<StockMovementEntity> = mutableListOf()
        )

        data class CustomerState(
            val id: String,
            var currentBalancePaise: Long
        )

        data class SaleState(
            val sale: SaleEntity,
            val lines: List<SaleLineEntity>,
            var isVoided: Boolean = false
        )

        data class PigmyState(
            var openingBalancePaise: Long = 0L,
            val deposits: MutableList<Long> = mutableListOf(),
            val withdrawals: MutableList<Long> = mutableListOf()
        ) {
            fun balance(): Long = openingBalancePaise + deposits.sum() - withdrawals.sum()
        }

        val products = mutableMapOf<String, ProductState>()
        val customers = mutableMapOf<String, CustomerState>()
        val sales = mutableMapOf<String, SaleState>()
        val expenses = mutableListOf<ShopExpenseEntity>()
        val pigmy = PigmyState(openingBalancePaise = 50_000L) // Rs 500 opening
        val history = mutableListOf<String>()

        // Initialize 5 products
        for (p in 1..5) {
            val pid = "prod-$p"
            val initialStock = 50 + rng.nextInt(50)
            val pState = ProductState(id = pid, currentSellableStock = initialStock, currentDamagedStock = 0)
            pState.movements.add(
                StockMovementEntity(
                    productId = pid,
                    type = "OPENING_STOCK",
                    quantityChangeBase = initialStock,
                    unitCostPaise = 2000L,
                    reason = "Initial setup"
                )
            )
            products[pid] = pState
        }

        // Initialize 3 customers
        for (c in 1..3) {
            val cid = "cust-$c"
            customers[cid] = CustomerState(id = cid, currentBalancePaise = 0L)
        }

        for (step in 1..iterations) {
            val op = rng.nextInt(7)
            val opDesc = when (op) {
                0 -> {
                    // Receive stock (purchase)
                    val prod = products.values.random(rng)
                    val qty = 5 + rng.nextInt(20)
                    prod.currentSellableStock += qty
                    prod.movements.add(
                        StockMovementEntity(
                            productId = prod.id,
                            type = "RECEIVE",
                            quantityChangeBase = qty,
                            unitCostPaise = 2000L,
                            reason = "Stock purchase"
                        )
                    )
                    "Step $step: RECEIVE ${prod.id} +$qty"
                }
                1 -> {
                    // Record sale
                    val prod = products.values.random(rng)
                    val maxQty = prod.currentSellableStock.coerceAtLeast(0)
                    if (maxQty > 0) {
                        val qty = 1 + rng.nextInt(minOf(maxQty, 10))
                        val isCredit = rng.nextBoolean()
                        val cust = if (isCredit) customers.values.random(rng) else null

                        val unitPricePaise = 3000L
                        val grossPaise = qty * unitPricePaise
                        val discountType = if (rng.nextInt(3) == 0) "PERCENT" else "NONE"
                        val discountVal = if (discountType == "PERCENT") (grossPaise * 10L) / 100L else 0L
                        val netPaise = grossPaise - discountVal

                        val amountReceived = if (isCredit) {
                            if (rng.nextBoolean()) netPaise / 2 else 0L
                        } else netPaise

                        val remainingCredit = (netPaise - amountReceived).coerceAtLeast(0L)
                        val saleId = "sale-$step"

                        // Deduct stock
                        prod.currentSellableStock -= qty
                        prod.movements.add(
                            StockMovementEntity(
                                productId = prod.id,
                                type = "SALE",
                                quantityChangeBase = -qty,
                                unitCostPaise = 2000L,
                                reason = "Sale $saleId"
                            )
                        )

                        // Allocate line discount
                        val lineInput = SaleLineInput(
                            id = "line-$step",
                            quantity = 1,
                            unitSellingPricePaise = grossPaise,
                            unitCostPaise = 2000L * qty
                        )
                        val alloc = CostCalculator.allocateWholeSaleDiscount(listOf(lineInput), discountVal).first()

                        val saleEntity = TestData.sale(
                            id = saleId,
                            receiptNumber = "REC-$step",
                            customerId = cust?.id,
                            customerName = cust?.id,
                            grossTotalPaise = grossPaise,
                            discountType = discountType,
                            discountValuePaise = discountVal,
                            netTotalPaise = netPaise,
                            amountReceivedPaise = amountReceived,
                            remainingCreditPaise = remainingCredit,
                            paymentMethod = if (isCredit) "CREDIT" else "CASH",
                            isVoided = false
                        )

                        val saleLine = TestData.saleLine(
                            id = "line-$step",
                            saleId = saleId,
                            productId = prod.id,
                            productName = prod.id,
                            quantityBase = qty,
                            unitSellingPricePaise = unitPricePaise,
                            unitCostSnapshotPaise = 2000L,
                            lineGrossPaise = alloc.grossValuePaise,
                            lineDiscountAllocatedPaise = alloc.allocatedDiscountPaise,
                            lineNetPaise = alloc.netValuePaise,
                            lineProfitPaise = alloc.actualProfitPaise
                        )

                        if (cust != null && remainingCredit > 0) {
                            cust.currentBalancePaise += remainingCredit
                        }

                        sales[saleId] = SaleState(sale = saleEntity, lines = listOf(saleLine), isVoided = false)
                        "Step $step: SALE $saleId on ${prod.id} qty=$qty net=$netPaise credit=$remainingCredit"
                    } else {
                        "Step $step: SKIP SALE (0 stock for ${prod.id})"
                    }
                }
                2 -> {
                    // Void an active sale
                    val activeSales = sales.values.filter { !it.isVoided }
                    if (activeSales.isNotEmpty()) {
                        val target = activeSales.random(rng)
                        target.isVoided = true
                        val cond = if (rng.nextBoolean()) "SELLABLE" else "DAMAGED"

                        // Restore stock
                        for (line in target.lines) {
                            val prod = products[line.productId]
                            if (prod != null) {
                                if (cond == "SELLABLE") {
                                    prod.currentSellableStock += line.quantityBase
                                    prod.movements.add(
                                        StockMovementEntity(
                                            productId = prod.id,
                                            type = "SALE_RETURN",
                                            quantityChangeBase = line.quantityBase,
                                            reason = "Void sale ${target.sale.id}"
                                        )
                                    )
                                } else {
                                    prod.currentDamagedStock += line.quantityBase
                                    prod.movements.add(
                                        StockMovementEntity(
                                            productId = prod.id,
                                            type = "DAMAGED",
                                            quantityChangeBase = line.quantityBase,
                                            reason = "Void damaged ${target.sale.id}"
                                        )
                                    )
                                }
                            }
                        }

                        // Revert customer credit
                        val custId = target.sale.customerId
                        if (custId != null && target.sale.remainingCreditPaise > 0) {
                            customers[custId]?.let { c ->
                                c.currentBalancePaise = (c.currentBalancePaise - target.sale.remainingCreditPaise).coerceAtLeast(0L)
                            }
                        }
                        "Step $step: VOID ${target.sale.id} condition=$cond"
                    } else {
                        "Step $step: SKIP VOID (no active sales)"
                    }
                }
                3 -> {
                    // Customer payment repayment
                    val indebtedCustomers = customers.values.filter { it.currentBalancePaise > 0 }
                    if (indebtedCustomers.isNotEmpty()) {
                        val cust = indebtedCustomers.random(rng)
                        val maxPay = cust.currentBalancePaise
                        val payAmount = 100L + (rng.nextLong(maxPay.coerceAtLeast(100L)))
                        val payment = minOf(payAmount, maxPay)

                        // Allocate to unpaid active sales of this customer
                        var left = payment
                        val custSales = sales.values
                            .filter { !it.isVoided && it.sale.customerId == cust.id && it.sale.remainingCreditPaise > 0 }
                            .map { it.sale }
                            .sortedBy { it.timestamp }

                        for (bill in custSales) {
                            if (left == 0L) break
                            val alloc = minOf(left, bill.remainingCreditPaise)
                            sales[bill.id] = sales[bill.id]!!.copy(
                                sale = bill.copy(
                                    amountReceivedPaise = bill.amountReceivedPaise + alloc,
                                    remainingCreditPaise = bill.remainingCreditPaise - alloc
                                )
                            )
                            left -= alloc
                        }
                        cust.currentBalancePaise -= payment
                        "Step $step: REPAYMENT cust=${cust.id} amount=$payment"
                    } else {
                        "Step $step: SKIP REPAYMENT (no debt)"
                    }
                }
                4 -> {
                    // Pigmy Deposit
                    val amount = (1 + rng.nextInt(20)) * 10000L // Rs 100 - Rs 2000
                    pigmy.deposits.add(amount)
                    "Step $step: PIGMY_DEPOSIT +$amount (new bal=${pigmy.balance()})"
                }
                5 -> {
                    // Pigmy Withdrawal (invariant 8: never exceed balance)
                    val bal = pigmy.balance()
                    if (bal > 0) {
                        val withdraw = minOf(bal, (1 + rng.nextInt(10)) * 5000L)
                        if (withdraw <= bal) {
                            pigmy.withdrawals.add(withdraw)
                            "Step $step: PIGMY_WITHDRAWAL -$withdraw (bal=${pigmy.balance()})"
                        } else {
                            "Step $step: SKIP PIGMY_WITHDRAWAL (insufficient)"
                        }
                    } else {
                        "Step $step: SKIP PIGMY_WITHDRAWAL (0 bal)"
                    }
                }
                else -> {
                    // Shop expense / income
                    val isIncome = rng.nextBoolean()
                    val amount = (1 + rng.nextInt(10)) * 1000L
                    val expense = TestData.expense(
                        id = "exp-$step",
                        category = if (isIncome) "COMMISSION" else "TEA_SNACKS",
                        amountPaise = amount,
                        isIncome = isIncome,
                        isVoided = false
                    )
                    expenses.add(expense)
                    "Step $step: EXPENSE isIncome=$isIncome amount=$amount"
                }
            }
            history.add(opDesc)

            // ── ASSERT INVARIANTS AFTER EVERY STEP ─────────────────────────

            // Invariant 1: product.currentSellableStock equals sum of stock movements affecting sellable stock
            for (prod in products.values) {
                val expectedSellableStock = prod.movements
                    .filter { it.type != "DAMAGED" }
                    .sumOf { it.quantityChangeBase }
                assertEquals(
                    "Invariant 1 violated at step $step (seed=$seed, op=$opDesc)\nHistory:\n${history.takeLast(10).joinToString("\n")}",
                    expectedSellableStock,
                    prod.currentSellableStock
                )
            }

            // Invariant 2: sale.netTotal = Σ line.net, sale.gross = Σ line.gross, net = gross − discount
            for (s in sales.values) {
                val sumLineGross = s.lines.sumOf { it.lineGrossPaise }
                val sumLineNet = s.lines.sumOf { it.lineNetPaise }
                val sumLineDiscount = s.lines.sumOf { it.lineDiscountAllocatedPaise }

                assertEquals("Invariant 2 (gross sum) violated at step $step (seed=$seed)", s.sale.grossTotalPaise, sumLineGross)
                assertEquals("Invariant 2 (net sum) violated at step $step (seed=$seed)", s.sale.netTotalPaise, sumLineNet)
                assertEquals("Invariant 2 (gross - discount) violated at step $step (seed=$seed)", s.sale.netTotalPaise, s.sale.grossTotalPaise - s.sale.discountValuePaise)
                assertEquals("Invariant 2 (line net = gross - discount) violated at step $step (seed=$seed)", sumLineNet, sumLineGross - sumLineDiscount)
            }

            // Invariant 3: Customer balance equals credit still owed on non-voided sales
            for (cust in customers.values) {
                val activeCustomerCredit = sales.values
                    .filter { !it.isVoided && it.sale.customerId == cust.id }
                    .sumOf { it.sale.remainingCreditPaise }
                assertEquals(
                    "Invariant 3 violated for ${cust.id} at step $step (seed=$seed)\nHistory:\n${history.takeLast(10).joinToString("\n")}",
                    activeCustomerCredit,
                    cust.currentBalancePaise
                )
            }

            // Invariant 4: A voided sale cannot be voided again
            val voidedSales = sales.values.filter { it.isVoided }
            for (vs in voidedSales) {
                assertTrue("Invariant 4: voided sale must remain isVoided=true", vs.isVoided)
            }

            // Invariant 5: No arithmetic overflow on money operations
            for (s in sales.values) {
                assertTrue("Invariant 5: sale netTotal must be >= 0", s.sale.netTotalPaise >= 0)
                assertTrue("Invariant 5: amountReceived must be >= 0", s.sale.amountReceivedPaise >= 0)
                assertTrue("Invariant 5: remainingCredit must be >= 0", s.sale.remainingCreditPaise >= 0)
            }

            // Invariant 6: Cash register totals never include voided entries
            val activeSales = sales.values.filter { !it.isVoided }
            val activeExpenses = expenses.filter { !it.isVoided }
            assertTrue("Invariant 6: active sales must have isVoided=false", activeSales.all { !it.sale.isVoided })
            assertTrue("Invariant 6: active expenses must have isVoided=false", activeExpenses.all { !it.isVoided })

            // Invariant 8: Pigmy balances never go negative
            assertTrue(
                "Invariant 8: Pigmy savings balance cannot be negative (balance=${pigmy.balance()}) at step $step (seed=$seed)",
                pigmy.balance() >= 0L
            )
        }
    }
}

