package com.alieducational.app.audit

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import com.alieducational.app.data.local.AppDatabase
import com.alieducational.app.data.local.entity.*
import com.alieducational.app.data.preferences.AppPreferences
import com.alieducational.app.data.repository.*
import com.alieducational.app.data.service.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class RepairIntegrationTest {
    private val ctx get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase
    private lateinit var repo: AppRepository
    @Before fun setup() {
        ctx.deleteDatabase("ali_educational.db")
        db = Room.databaseBuilder(ctx, AppDatabase::class.java, "ali_educational.db").build()
        repo = AppRepository(db, db.productDao(), db.productBarcodeDao(), db.stockMovementDao(), db.supplierDao(), db.customerDao(), db.purchaseDao(), db.saleDao(), db.accountPaymentDao(), db.shopExpenseDao(), db.heldBillDao(), db.pigmyDepositDao(), db.pigmyWithdrawalDao(), db.pigmySchemeDao(), db.cashCategoryDao(), db.productGiveawayDao(), db.serviceDefinitionDao(), db.syncEventDao())
    }
    @After fun close() { db.close() }

    @Test fun savingsEditsAndCancellationCannotSpendSameDepositTwice() = runBlocking {
        repo.ensureDefaultPigmySchemes()
        val depositId=repo.recordPigmyDeposit(amountPaise=10000)
        repo.recordPigmyWithdrawal("DEFAULT_PIGMY",8000)
        assertTrue(runCatching { repo.recordPigmyWithdrawal("DEFAULT_PIGMY",3000) }.isFailure)
        assertTrue(runCatching { repo.voidPigmyDeposit(depositId,"Cannot erase spent savings") }.isFailure)
        val deposit=db.pigmyDepositDao().getDepositById(depositId)!!
        assertTrue(runCatching { repo.updateShopExpense(deposit.linkedExpenseId!!,5000,"Pigmy","CASH",null,System.currentTimeMillis()) }.isFailure)
        assertEquals(10000L,db.pigmyDepositDao().getDepositById(depositId)!!.amountPaise)
        assertEquals(8000L,db.pigmyWithdrawalDao().getTotalForScheme("DEFAULT_PIGMY").first())
    }

    @Test fun snapshotRoundTripPreservesEveryApplicationTableAndColumn() = runBlocking {
        val sql=db.openHelper.writableDatabase
        val names=mutableListOf<String>()
        sql.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT IN ('android_metadata','room_master_table') ORDER BY name").use { c -> while(c.moveToNext()) names.add(c.getString(0)) }
        fun quote(s: String)="\""+s.replace("\"","\"\"")+"\""
        fun snapshot(): Map<String,List<String>> = names.associateWith { table ->
            sql.query("SELECT * FROM ${quote(table)}").use { c ->
                val result=mutableListOf<String>()
                while(c.moveToNext()) result.add((0 until c.columnCount).joinToString("|") { i -> "${c.columnNames[i]}:${c.getType(i)}:${if(c.isNull(i)) "NULL" else c.getString(i)}" })
                result.sorted()
            }
        }
        db.withTransaction {
            for(table in names) {
                val columns=mutableListOf<Pair<String,String>>()
                sql.query("PRAGMA table_info(${quote(table)})").use { c -> while(c.moveToNext()) columns.add(c.getString(1) to c.getString(2)) }
                val values=columns.map { (column,type) -> when { type.contains("INT") -> 7L; type.contains("REAL") -> 7.5; else -> "fixture-$table-$column" } }.toTypedArray()
                sql.execSQL("INSERT INTO ${quote(table)} (${columns.joinToString { quote(it.first) }}) VALUES (${columns.joinToString { "?" }})",values)
            }
        }
        val before=snapshot()
        val file=File(ctx.cacheDir,"all-tables-${java.util.UUID.randomUUID()}.db")
        db.withTransaction { DatabaseSnapshot.export(sql,file) }
        db.withTransaction {
            names.forEach { sql.execSQL("DELETE FROM ${quote(it)}") }
            android.database.sqlite.SQLiteDatabase.openDatabase(file.path,null,android.database.sqlite.SQLiteDatabase.OPEN_READONLY).use { DatabaseSnapshot.replace(it,sql) }
        }
        assertTrue("Schema coverage unexpectedly empty",names.size >= 15)
        assertEquals(before,snapshot())
        file.delete()
        Unit
    }

    @Test fun duplicateCheckoutCannotDeductStockTwice() = runBlocking {
        repo.addProduct(ProductEntity(id="p",name="Pen",category="Audit",currentSellableStock=10,movingAvgCostPaise=100))
        val rows=listOf(SaleItemParam("p","Pen",2,unitSellingPricePaise=200,lineId="checkout-once"))
        val sale=repo.recordSale(null,null,rows,"NONE",0.0,500,"CASH",null)
        assertEquals(100L,sale.changeGivenPaise)
        assertTrue(runCatching { repo.recordSale(null,null,rows,"NONE",0.0,500,"CASH",null) }.isFailure)
        assertEquals(8,repo.getProductById("p")!!.currentSellableStock)
        assertEquals(1,db.saleDao().getAllSalesWithVoided().first().size)
    }

    @Test fun corruptedArchiveCannotChangeLiveData() = runBlocking {
        val prefs = AppPreferences(ctx)
        repo.addProduct(ProductEntity(id="safe",name="Original",category="Audit",currentSellableStock=42))
        val service = BackupRestoreService(ctx, db, prefs, ShopRecordsExcelGenerator())
        val backup = service.createBackup()
        val damaged = File(ctx.cacheDir,"damaged-recovery.zip")
        java.util.zip.ZipFile(backup.completeBackupFilePath!!).use { input ->
            java.util.zip.ZipOutputStream(damaged.outputStream()).use { output ->
                input.entries().asSequence().forEach { entry ->
                    output.putNextEntry(java.util.zip.ZipEntry(entry.name))
                    if (entry.name == "ali_educational.db") output.write("not a database".toByteArray())
                    else input.getInputStream(entry).use { it.copyTo(output) }
                    output.closeEntry()
                }
            }
        }
        assertTrue(runCatching { service.restoreBackup(damaged) }.isFailure)
        assertEquals(42,repo.getProductById("safe")!!.currentSellableStock)
    }

    @Test fun billingRoleCannotPromoteItselfOrUseUniversalPin() = runBlocking {
        val prefs = AppPreferences(ctx)
        prefs.setOwnerPin("672945")
        prefs.setDeviceRole("BILLING")
        try {
            assertTrue(runCatching { prefs.setDeviceRole("OWNER") }.isFailure)
            assertFalse(prefs.authorizeOwner("1234"))
            assertEquals("BILLING",prefs.deviceRole.first())
        } finally { assertTrue(prefs.authorizeOwner("672945")) }
    }

    @Test fun backupRoundTripKeepsSettingsStockAndOpenDatabase() = runBlocking {
        val prefs = AppPreferences(ctx)
        prefs.setShopName("Recovery Test Shop"); prefs.setAppTextSize("Large")
        repo.addProduct(ProductEntity(id="p", name="Notebook", category="Audit", currentSellableStock=17, movingAvgCostPaise=345))
        repo.addCustomer(CustomerEntity(id="c", name="Customer", currentBalancePaise=450))
        val service = BackupRestoreService(ctx, db, prefs, ShopRecordsExcelGenerator())
        val backup = service.createBackup()
        repo.updateProduct(repo.getProductById("p")!!.copy(currentSellableStock=2))
        prefs.setShopName("Changed"); prefs.setAppTextSize("Small")
        assertTrue(service.restoreBackup(File(backup.filePath)))
        assertEquals(17, repo.getProductById("p")!!.currentSellableStock)
        assertEquals(450L, repo.getCustomerById("c")!!.currentBalancePaise)
        assertEquals("Recovery Test Shop", prefs.shopName.first())
        assertEquals("Large", prefs.appTextSize.first())
        repo.updateProduct(repo.getProductById("p")!!.copy(name="Database remains usable"))
        assertEquals("Database remains usable", repo.getProductById("p")!!.name)
    }

    @Test fun metadataOnlyEditDoesNotAddStockAndPurchaseHasLine() = runBlocking {
        val p = repo.addOrUpdateProductWithStock(null,"Pen",null,"Audit",null,1,0,10,100,200,0,5,"Supplier", "INV",100,false)
        val updated = repo.addOrUpdateProductWithStock(p.id,"Blue Pen",null,"Audit",null,1,0,0,p.movingAvgCostPaise,250,0,5,null)
        assertEquals(10, updated.currentSellableStock)
        assertEquals(1, db.purchaseDao().getAllPurchaseLines().first().size)
        assertEquals(110L, p.movingAvgCostPaise)
    }

    @Test fun settlementThenCancellationReconcilesDebt() = runBlocking {
        repo.addProduct(ProductEntity(id="p",name="Pen",category="Audit",currentSellableStock=10,movingAvgCostPaise=100))
        repo.addCustomer(CustomerEntity(id="c",name="Customer"))
        val sale=repo.recordSale("c","Customer",listOf(SaleItemParam("p","Pen",2,unitSellingPricePaise=200)),"NONE",0.0,100,"CASH",null)
        repo.recordAccountPayment("CUSTOMER","c",100,"CASH",null,sale.receiptNumber)
        repo.voidSale(sale.id,"SELLABLE","Customer return")
        assertEquals(0L,repo.getCustomerById("c")!!.currentBalancePaise)
        assertEquals(200L,db.saleDao().getSaleById(sale.id)!!.refundPayoutPaise)
        assertEquals(10,repo.getProductById("p")!!.currentSellableStock)
    }

    @Test fun catalogImportReplayDoesNotAddStockTwice() = runBlocking {
        val file=File(ctx.cacheDir,"audit-catalog.csv").apply { writeText("Name,Qty,Cost,Price\nNotebook,10,1.50,2.00\n") }
        val service=ImportExportService(ctx,db.productDao(),db.stockMovementDao(),db.importBatchDao(),db)
        val mapping=ColumnMapping(nameIndex=0,openingQtyIndex=1,purchaseCostIndex=2,sellingPricePieceIndex=3)
        val first=service.importProducts(android.net.Uri.fromFile(file),file.name,0,mapping,true)
        assertTrue(first.failureReason,first.isSuccess)
        service.importProducts(android.net.Uri.fromFile(file),file.name,0,mapping,true)
        assertEquals(10,db.productDao().getAllProducts().first().single().currentSellableStock)
    }

    @Test fun tlsCatalogRequiresPairingAndCarriesOpaqueCosts() = runBlocking {
        val prefs=AppPreferences(ctx)
        val token=prefs.ensureSyncToken()
        repo.addProduct(ProductEntity(id="p",name="اردو قلم",category="Audit",currentSellableStock=10,movingAvgCostPaise=123))
        val service=TwoPhoneSyncService(ctx,db,prefs,db.productDao(),db.serviceDefinitionDao(),db.saleDao(),db.customerDao(),db.stockMovementDao(),db.syncEventDao())
        service.startOwnerServer(18993)
        try {
            kotlinx.coroutines.withTimeout(10000) { service.connectionState.first { it is SyncConnectionState.Hosting || it is SyncConnectionState.Error } }
            fun request(auth: String, pin: String, body: String? = null): Pair<Int,String> {
                val path = if (body == null) "/api/catalog" else "/api/sync-bills"
                val c=URL("https://127.0.0.1:18993$path").openConnection() as HttpsURLConnection
                SyncTls.configure(c,pin); c.connectTimeout=5000;c.readTimeout=5000;c.setRequestProperty("Authorization","Bearer $auth")
                if (body != null) { c.requestMethod="POST"; c.doOutput=true; c.setRequestProperty("Content-Type","application/json; charset=utf-8"); c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) } }
                return try { val code=c.responseCode; code to (if(code==200)c.inputStream.bufferedReader().readText() else "") } finally { c.disconnect() }
            }
            assertEquals(401,request("wrong",SyncTls.fingerprint()).first)
            val result=request(token,SyncTls.fingerprint())
            assertEquals(200,result.first)
            val product=org.json.JSONObject(result.second).getJSONArray("products").getJSONObject(0)
            assertFalse(product.has("movingAvgCostPaise"))
            assertEquals("اردو قلم",product.getString("name"))
            val quote=product.getString("costQuote")
            assertEquals(123L,CostQuote.resolve(quote,"p"))
            assertNull(CostQuote.resolve(quote,"different-product"))
            val line=org.json.JSONObject().put("id","sync-line").put("productId","p").put("productName","اردو قلم")
                .put("quantityBase",2).put("unitSellingPricePaise",200).put("lineGrossPaise",400).put("lineNetPaise",400).put("costQuote",quote)
            val bill=org.json.JSONObject().put("id","sync-sale").put("receiptNumber","SYNC-TEST").put("customerId","offline-customer")
                .put("customerName","Offline Customer").put("customer",org.json.JSONObject().put("id","offline-customer").put("name","Offline Customer"))
                .put("grossTotalPaise",400).put("discountType","NONE").put("discountValuePaise",0).put("netTotalPaise",400)
                .put("amountReceivedPaise",0).put("remainingCreditPaise",400).put("paymentMethod","CASH").put("timestamp",123456L)
                .put("lines",org.json.JSONArray().put(line))
            val body=org.json.JSONArray().put(bill).toString()
            assertEquals(200,request(token,SyncTls.fingerprint(),body).first)
            assertEquals(200,request(token,SyncTls.fingerprint(),body).first)
            assertEquals(8,repo.getProductById("p")!!.currentSellableStock)
            assertEquals(400L,repo.getCustomerById("offline-customer")!!.currentBalancePaise)
            assertEquals(123L,db.saleDao().getLinesForSale("sync-sale").single().unitCostSnapshotPaise)
            assertTrue(runCatching { request(token,"0".repeat(64)) }.isFailure)
        } finally { service.stopOwnerServer() }
    }
}
