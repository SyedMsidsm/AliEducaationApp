package com.alieducational.app.audit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.alieducational.app.data.local.AppDatabase
import com.alieducational.app.data.local.entity.*
import com.alieducational.app.data.repository.*
import com.alieducational.app.data.service.*
import com.alieducational.app.data.preferences.AppPreferences
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.ui.BillingDraftItem
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

/** Independent audit: expectations describe required behavior, not current implementation. */
@RunWith(AndroidJUnit4::class)
class AuditDatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: AppRepository
    private val ctx get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
        repo = AppRepository(db, db.productDao(), db.productBarcodeDao(), db.stockMovementDao(),
            db.supplierDao(), db.customerDao(), db.purchaseDao(), db.saleDao(), db.accountPaymentDao(),
            db.shopExpenseDao(), db.heldBillDao(), db.pigmyDepositDao(), db.pigmyWithdrawalDao(),
            db.pigmySchemeDao(), db.cashCategoryDao(), db.productGiveawayDao(), db.serviceDefinitionDao(), db.syncEventDao())
    }
    @After fun teardown() { db.close() }
    private suspend fun product(id: String = "p", stock: Int = 10, cost: Long = 1000) {
        db.productDao().insertProduct(ProductEntity(id=id, name=id, category="Audit", currentSellableStock=stock, movingAvgCostPaise=cost, sellingPricePiecePaise=2000))
    }
    private suspend fun sale(qty: Int = 2, customer: String? = null, paid: Long = qty * 2000L) =
        repo.recordSale(customer, customer, listOf(SaleItemParam("p", "p", qty, unitSellingPricePaise=2000)), "NONE", 0.0, paid, "CASH", null)

    @Test fun baselineSaleKeepsHistoricalCost() = runBlocking {
        product(); val s = sale()
        db.productDao().updateWeightedAverageCost("p", 1500)
        assertEquals(8, repo.getProductById("p")!!.currentSellableStock)
        assertEquals(1000L, repo.getLinesForSale(s.id).single().unitCostSnapshotPaise)
        assertEquals(2000L, repo.getLinesForSale(s.id).single().lineProfitPaise)
    }
    @Test fun receiveWithNewSellingPricePreservesNewStockAndCost() = runBlocking {
        product(); repo.addSupplier(SupplierEntity(id="s", name="supplier"))
        repo.receiveGoods("s", "INV1", listOf(PurchaseItemParam("p", 10, 2000, 3000)), 0, "SUPPLIER", 0, "CASH", null)
        val p = repo.getProductById("p")!!
        assertEquals("Received quantity must survive repricing", 20, p.currentSellableStock)
        assertEquals(1500L, p.movingAvgCostPaise)
    }
    @Test fun baselinePurchaseAndSupplierSettlement() = runBlocking {
        product(stock=0); repo.addSupplier(SupplierEntity(id="s", name="supplier"))
        repo.receiveGoods("s", "INV1", listOf(PurchaseItemParam("p", 100, 1000)), 10000, "CARRIER", 60000, "CASH", null)
        assertEquals(1100L, repo.getProductById("p")!!.movingAvgCostPaise)
        assertEquals(40000L, repo.getSupplierById("s")!!.currentBalancePaise)
        repo.recordAccountPayment("SUPPLIER", "s", 40000, "CASH", null, null)
        assertEquals(0L, repo.getSupplierById("s")!!.currentBalancePaise)
    }
    @Test fun packDraftAndSavedBillMustAgree() = runBlocking {
        product(stock=100)
        db.productDao().updateProduct(repo.getProductById("p")!!.copy(packSize=10))
        val draft = BillingDraftItem(productId="p", productName="p", unitType="PACK", packSize=10, quantity=2, unitPricePaise=15000)
        // Exact conversion currently used by postCurrentBill.
        val s = repo.recordSale(null, null, listOf(SaleItemParam("p", "p", draft.totalPieces, true, draft.unitPricePaise)), "NONE", 0.0, draft.lineGrossPaise, "CASH", null)
        assertEquals("Saved pack sale must equal displayed draft total", draft.lineGrossPaise, s.netTotalPaise)
    }
    @Test fun damagedReturnDoesNotRemoveMoreSellableStock() = runBlocking {
        product(); val s = sale(); repo.voidSale(s.id, "DAMAGED", "audit return")
        val p = repo.getProductById("p")!!
        assertEquals(8, p.currentSellableStock)
        assertEquals(2, p.currentDamagedStock)
    }
    @Test fun expenseEditMustUpdateLinkedPigmyDeposit() = runBlocking {
        repo.ensureDefaultPigmySchemes()
        val id = repo.recordPigmyDeposit(amountPaise=10000)
        val d = db.pigmyDepositDao().getDepositById(id)!!
        repo.updateShopExpense(d.linkedExpenseId!!, 20000, "Pigmy", "CASH", null, System.currentTimeMillis())
        assertEquals(20000L, db.pigmyDepositDao().getDepositById(id)!!.amountPaise)
    }
    @Test fun cannotReduceOriginalExpenseBelowAlreadyRefundedAmount() = runBlocking {
        db.shopExpenseDao().insertExpense(ShopExpenseEntity(id="e", category="Rent", amountPaise=10000, isIncome=false))
        repo.recordExpenseRefund("e", 8000)
        val result = runCatching { repo.updateShopExpense("e", 5000, "Rent", "CASH", null, System.currentTimeMillis()) }
        assertTrue("Must reject inconsistent expense edit", result.isFailure)
    }
    @Test fun cannotVoidExpenseWithActiveRefundViaCashEditApi() = runBlocking {
        db.shopExpenseDao().insertExpense(ShopExpenseEntity(id="e", category="Rent", amountPaise=10000, isIncome=false))
        repo.recordExpenseRefund("e", 8000)
        assertTrue(runCatching { repo.voidShopExpense("e", "audit") }.isFailure)
    }
    @Test fun generatedBarcodesNeverResolveToAnotherProduct() = runBlocking {
        product("abcdef00-0000-0000-0000-000000000001")
        product("abcdef00-0000-0000-0000-000000000002")
        repo.generateAndRegisterBarcode("abcdef00-0000-0000-0000-000000000001")
        val b = repo.generateAndRegisterBarcode("abcdef00-0000-0000-0000-000000000002")
        assertEquals("abcdef00-0000-0000-0000-000000000002", b.productId)
    }
    @Test fun rejectOversellingBeforeChangingStock() = runBlocking {
        product(stock=1)
        assertTrue("Oversell requires rejection or explicit recorded policy", runCatching { sale(qty=2) }.isFailure)
    }
    @Test fun rejectNegativeSaleQuantity() = runBlocking {
        product()
        assertTrue(runCatching { sale(qty=-2, paid=0) }.isFailure)
    }
    @Test fun unknownCustomerMustNotCreateUntrackedDebt() = runBlocking {
        product()
        assertTrue(runCatching { sale(customer="does-not-exist", paid=0) }.isFailure)
    }
    @Test fun corruptDatabaseArchiveMustNotBeVerified() {
        val service = BackupRestoreService(ctx, db, AppPreferences(ctx), ShopRecordsExcelGenerator())
        val file = File(ctx.cacheDir, "audit-invalid.zip")
        ZipOutputStream(file.outputStream()).use { z -> z.putNextEntry(ZipEntry("ali_educational.db")); z.write("not a database".toByteArray()); z.closeEntry() }
        val method = service.javaClass.getDeclaredMethod("inspectPackageOrBackup", File::class.java).apply { isAccessible=true }
        val result = method.invoke(service, file) as Triple<*,*,*>
        assertEquals("Corrupt DB must be invalid", false, result.first)
    }
    @Test fun negativeDecimalAndOverflowMoneyMustBeRejected() {
        assertNull("Negative decimal", CostCalculator.parseRupeesToPaise("-0.50"))
        assertNull("Overflow", CostCalculator.parseRupeesToPaise("92233720368547759"))
    }
    @Test fun baselineNotesSeparatedAndDeduplicated() = runBlocking {
        repo.recordShopExpense("General", 100, false, "Rent")
        repo.recordShopExpense("General", 100, false, " rent ")
        repo.recordShopExpense("General", 100, true, "Refund")
        assertEquals(listOf("rent"), repo.getNoteSuggestions(false).map { it.lowercase() })
        assertEquals(listOf("Refund"), repo.getNoteSuggestions(true))
    }
    @Test fun baselineRefundLimitEnforced() = runBlocking {
        db.shopExpenseDao().insertExpense(ShopExpenseEntity(id="e", category="Rent", amountPaise=10000, isIncome=false))
        repo.recordExpenseRefund("e", 8000)
        assertTrue(runCatching { repo.recordExpenseRefund("e", 3000) }.isFailure)
    }
    @Test fun migrationFromEveryExportedSchemaPreservesProduct() {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val migrations = arrayOf(
            AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9
        )
        for (version in 1..8) {
            val name = "audit-migrate-$version.db"
            ctx.deleteDatabase(name)
            val file = ctx.getDatabasePath(name).apply { parentFile!!.mkdirs() }
            val oldDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null)
            val schema = org.json.JSONObject(assets.open("audit-schemas/$version.json").bufferedReader().readText()).getJSONObject("database")
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                oldDb.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.getJSONArray("indices")
                for (j in 0 until indices.length()) oldDb.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                if (table == "products") {
                    val row = android.content.ContentValues()
                    val fields = entity.getJSONArray("fields")
                    for (j in 0 until fields.length()) {
                        val field = fields.getJSONObject(j)
                        val column = field.getString("columnName")
                        if (field.getBoolean("notNull")) {
                            if (field.getString("affinity") == "TEXT") row.put(column, "audit") else row.put(column, 0L)
                        }
                    }
                    row.put("currentSellableStock", 73); row.put("movingAvgCostPaise", 12345L)
                    oldDb.insertOrThrow(table, null, row)
                }
            }
            oldDb.version = version; oldDb.close()
            val migrated = Room.databaseBuilder(ctx, AppDatabase::class.java, name).addMigrations(*migrations).build()
            try {
                val product = runBlocking { migrated.productDao().getProductById("audit") }!!
                assertEquals("v$version stock", 73, product.currentSellableStock)
                assertEquals("v$version cost", 12345L, product.movingAvgCostPaise)

                // Verify integrity
                val cursor = migrated.openHelper.readableDatabase.query("PRAGMA integrity_check")
                assertTrue(cursor.moveToFirst())
                assertEquals("ok", cursor.getString(0))
                cursor.close()
            } finally { migrated.close(); ctx.deleteDatabase(name) }
        }
    }
    @Test fun receiveDuplicateProductRowsAllocatesEachRowExactlyOnce() = runBlocking {
        product(stock=0); repo.addSupplier(SupplierEntity(id="s", name="supplier"))
        val id = repo.receiveGoods("s", "dup", listOf(PurchaseItemParam("p", 1, 10000), PurchaseItemParam("p", 1, 30000)), 4000, "SUPPLIER", 0, "CASH", null)
        val lines = db.purchaseDao().getAllPurchaseLines().first().filter { it.purchaseId == id }
        assertEquals("Transport shares must sum to original charge", 4000L, lines.sumOf { it.allocatedDeliveryPaise })
        assertEquals(22000L, repo.getProductById("p")!!.movingAvgCostPaise)
    }
    @Test fun deletingCustomerWithHistoryMustPreserveAccount() = runBlocking {
        product(); repo.addCustomer(CustomerEntity(id="c", name="Customer")); sale(customer="c", paid=0)
        runCatching { repo.deleteCustomer("c") }
        assertNotNull("Deletion must be blocked or archive account", repo.getCustomerById("c"))
    }
    @Test fun negativeStockMustNotReduceInventoryValue() = runBlocking {
        product("good", stock=10, cost=1000); product("short", stock=-5, cost=1000)
        assertEquals(10000L, repo.totalStockValuePaise.first())
    }
    @Test fun customerRepaymentMustClearBillDueToo() = runBlocking {
        product(); repo.addCustomer(CustomerEntity(id="c", name="Customer"))
        val s = sale(customer="c", paid=0)
        repo.recordAccountPayment("CUSTOMER", "c", 4000, "CASH", null, s.receiptNumber)
        assertEquals(0L, repo.getCustomerById("c")!!.currentBalancePaise)
        assertEquals("Bill history due must agree with settled account", 0L, db.saleDao().getSaleById(s.id)!!.remainingCreditPaise)
    }
    @Test fun billHistorySourceMustIncludeCancelledBills() = runBlocking {
        product(); val s = sale(); repo.voidSale(s.id, "SELLABLE", "audit")
        assertTrue("Voided history filter needs cancelled records", repo.allSales.first().any { it.id == s.id && it.isVoided })
    }
    @Test fun realBackupMustContainSupportedPreferences() = runBlocking {
        db.close(); ctx.deleteDatabase("ali_educational.db")
        db = Room.databaseBuilder(ctx, AppDatabase::class.java, "ali_educational.db").build()
        db.productDao().insertProduct(ProductEntity(id="backup-fixture", name="Audit backup product", category="Audit"))
        val prefs = AppPreferences(ctx); prefs.setShopName("Audit Custom Shop"); prefs.setAppTextSize("Extra Large")
        val service = BackupRestoreService(ctx, db, prefs, ShopRecordsExcelGenerator())
        val backup = service.createBackup()
        val names = java.util.zip.ZipFile(backup.completeBackupFilePath!!).use { z -> z.entries().asSequence().map { it.name }.toList() }
        assertTrue("Backup omits preferences: $names", names.any { it.contains("preferences", true) || it.contains("settings", true) })
    }
    @Test fun restoreMustRejectSqliteWithOnlyFiveEmptyTables() = runBlocking {
        val raw = File(ctx.cacheDir, "audit-wrong-schema.db"); raw.delete()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(raw, null).use { old ->
            listOf("products", "sales", "suppliers", "customers", "shop_expenses").forEach { old.execSQL("CREATE TABLE $it (id TEXT)") }
            old.version = 999
        }
        val archive = File(ctx.cacheDir, "audit-wrong-schema.zip")
        ZipOutputStream(archive.outputStream()).use { z -> z.putNextEntry(ZipEntry("ali_educational.db")); z.write(raw.readBytes()); z.closeEntry() }
        val service = BackupRestoreService(ctx, db, AppPreferences(ctx), ShopRecordsExcelGenerator())
        val result = runCatching { service.restoreBackup(archive) }
        assertTrue("Unsupported incomplete schema must be rejected before replacement", result.isFailure || result.getOrNull() == false)
    }
    @Test fun moneyOverflowMustNotBecomeNegative() {
        assertNull(CostCalculator.parseRupeesToPaise("92233720368547759"))
    }
    @Test fun trendMustKeepDifferentYearsSeparate() {
        fun bill(year: Int): SaleEntity {
            val c = java.util.Calendar.getInstance().apply { clear(); set(year, 0, 1, 12, 0) }
            return SaleEntity(id="year$year", receiptNumber="year$year", grossTotalPaise=100, netTotalPaise=100, amountReceivedPaise=100, timestamp=c.timeInMillis)
        }
        val buckets = com.alieducational.app.ui.reports.generateTrendBuckets(listOf(bill(2025), bill(2026)), emptyList(), 0, Long.MAX_VALUE, "All Time")
        assertEquals("Same day/month in different years must not merge", 2, buckets.size)
    }
}
