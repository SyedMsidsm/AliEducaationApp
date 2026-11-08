package com.alieducational.app.data.service

import android.content.Context
import android.net.wifi.WifiManager
import androidx.room.withTransaction
import com.alieducational.app.data.local.AppDatabase
import com.alieducational.app.data.local.dao.CustomerDao
import com.alieducational.app.data.local.dao.ProductDao
import com.alieducational.app.data.local.dao.SaleDao
import com.alieducational.app.data.local.dao.ServiceDefinitionDao
import com.alieducational.app.data.local.dao.StockMovementDao
import com.alieducational.app.data.local.dao.SyncEventDao
import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.local.entity.SaleLineEntity
import com.alieducational.app.data.local.entity.StockMovementEntity
import com.alieducational.app.data.local.entity.SyncEventEntity
import com.alieducational.app.data.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class SyncConnectionState {
    data object Idle : SyncConnectionState()
    data class Hosting(val ipAddress: String, val port: Int) : SyncConnectionState()
    data class Connected(val host: String) : SyncConnectionState()
    data class Syncing(val message: String) : SyncConnectionState()
    data class Error(val message: String) : SyncConnectionState()
}

@Singleton
class TwoPhoneSyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val preferences: AppPreferences,
    private val productDao: ProductDao,
    private val serviceDao: ServiceDefinitionDao,
    private val saleDao: SaleDao,
    private val customerDao: CustomerDao,
    private val stockMovementDao: StockMovementDao,
    private val syncEventDao: SyncEventDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    private val _connectionState = MutableStateFlow<SyncConnectionState>(SyncConnectionState.Idle)
    val connectionState: StateFlow<SyncConnectionState> = _connectionState.asStateFlow()

    private val _blockedAttempts = MutableStateFlow(0)
    val blockedAttempts: StateFlow<Int> = _blockedAttempts.asStateFlow()

    private val connectionSemaphore = java.util.concurrent.Semaphore(4)
    private val rateLimiter = SyncIpRateLimiter()

    fun getLocalIpAddress(): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                return String.format(
                    Locale.US,
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            }
        } catch (_: Exception) {}

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("TwoPhoneSyncService", "NetworkInterface IP lookup failed: ${e.message}")
        }

        return "127.0.0.1"
    }

    // ── Owner Server Functions ─────────────────────────────────────────

    fun startOwnerServer(port: Int = 8080) {
        if (serverJob != null && serverJob?.isActive == true) return

        serverJob = scope.launch {
            try {
                require(preferences.deviceRole.first() == "OWNER") { "Only the Owner can host" }
                preferences.ensureSyncToken()
                serverSocket = SyncTls.serverFactory().createServerSocket(port)
                if (serverSocket is javax.net.ssl.SSLServerSocket) {
                    (serverSocket as javax.net.ssl.SSLServerSocket).enabledProtocols = arrayOf("TLSv1.3", "TLSv1.2")
                }
                val ip = getLocalIpAddress()
                _connectionState.value = SyncConnectionState.Hosting(ip, port)

                while (serverSocket?.isClosed == false) {
                    val clientSocket = serverSocket?.accept() ?: break
                    if (!connectionSemaphore.tryAcquire()) {
                        try { clientSocket.close() } catch (_: Exception) {}
                        _blockedAttempts.value++
                        continue
                    }
                    launch {
                        try {
                            handleClientRequest(clientSocket)
                        } finally {
                            connectionSemaphore.release()
                        }
                    }
                }
            } catch (e: Exception) {
                _connectionState.value = SyncConnectionState.Error("Server error: ${e.message}")
            }
        }
    }

    fun stopOwnerServer() {
        try {
            serverSocket?.close()
            serverSocket = null
            serverJob?.cancel()
            serverJob = null
            _connectionState.value = SyncConnectionState.Idle
        } catch (_: Exception) {}
    }

    private suspend fun handleClientRequest(socket: Socket) = withContext(Dispatchers.IO) {
        val clientIp = socket.inetAddress?.hostAddress ?: "unknown"
        if (rateLimiter.isBlocked(clientIp)) {
            _blockedAttempts.value++
            try { socket.close() } catch (_: Exception) {}
            return@withContext
        }
        try {
            socket.soTimeout = 10000
            require(preferences.deviceRole.first() == "OWNER") { "Owner hosting is no longer authorized" }
            val input = socket.getInputStream().buffered()
            fun readHeader(): String? {
                val bytes = java.io.ByteArrayOutputStream()
                while (true) { val b = input.read(); if (b < 0) return if (bytes.size() == 0) null else bytes.toString("US-ASCII"); if (b == 10) break; require(bytes.size() < 8192) { "Header too large" }; if (b != 13) bytes.write(b) }
                return bytes.toString("US-ASCII")
            }
            val writer = OutputStreamWriter(socket.getOutputStream())

            val requestLine = readHeader() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0]
            val path = parts[1]

            // Read headers to get content length and authorization
            var contentLength = 0
            var authHeader: String? = null
            var line: String?
            var headerCount = 0
            while (readHeader().also { line = it } != null) {
                require(++headerCount <= 64) { "Too many headers" }
                if (line.isNullOrBlank()) break
                val l = line!!.trim()
                if (l.lowercase().startsWith("content-length:")) {
                    contentLength = l.substringAfter(":").trim().toIntOrNull() ?: 0
                } else if (l.lowercase().startsWith("authorization:")) {
                    authHeader = l.substringAfter(":").trim()
                }
            }

            // Secure authenticated endpoints (A01):
            val requiredToken = preferences.syncToken.first()
            val providedToken = authHeader?.removePrefix("Bearer ")?.trim()

            if (requiredToken.length < 32 || providedToken.isNullOrEmpty() || !java.security.MessageDigest.isEqual(providedToken.toByteArray(), requiredToken.toByteArray())) {
                rateLimiter.recordBadAttempt(clientIp)
                _blockedAttempts.value++
                val errJson = JSONObject().apply {
                    put("status", "UNAUTHORIZED")
                    put("message", "Invalid or missing Bearer authorization token.")
                }.toString().toByteArray(Charsets.UTF_8)

                writer.write("HTTP/1.1 401 Unauthorized\r\n")
                writer.write("Content-Type: application/json; charset=utf-8\r\n")
                writer.write("Content-Length: ${errJson.size}\r\n")
                writer.write("\r\n")
                writer.flush()
                socket.getOutputStream().write(errJson)
                socket.getOutputStream().flush()
                return@withContext
            }

            rateLimiter.recordSuccess(clientIp)

            if (method == "GET" && path.startsWith("/api/catalog")) {
                // Send role-filtered catalog: selling prices and stock only (NO purchase costs or supplier dues!)
                val products = productDao.getAllActiveProducts().first()
                val services = serviceDao.getAllActiveServices().first()

                val root = JSONObject()
                val productsArray = JSONArray()
                products.forEach { p ->
                    val obj = JSONObject()
                    obj.put("id", p.id)
                    obj.put("name", p.name)
                    obj.put("urduName", p.urduName ?: "")
                    obj.put("category", p.category)
                    obj.put("brand", p.brand ?: "")
                    obj.put("sku", p.sku ?: "")
                    obj.put("barcode", p.barcode ?: "")
                    obj.put("packSize", p.packSize)
                    obj.put("sellingPricePiecePaise", p.sellingPricePiecePaise)
                    obj.put("sellingPricePackPaise", p.sellingPricePackPaise)
                    obj.put("currentSellableStock", p.currentSellableStock)
                    obj.put("costQuote", CostQuote.issue(p.id, p.movingAvgCostPaise))
                    val aliases = JSONArray()
                    db.productBarcodeDao().getBarcodesForProduct(p.id).forEach { b -> aliases.put(JSONObject().put("id", b.id).put("barcode", b.barcode).put("unitType", b.unitType).put("packQuantity", b.packQuantity)) }
                    obj.put("aliases", aliases)
                    productsArray.put(obj)
                }
                root.put("products", productsArray)

                val servicesArray = JSONArray()
                services.forEach { s ->
                    val obj = JSONObject()
                    obj.put("id", s.id)
                    obj.put("name", s.name)
                    obj.put("unitLabel", s.unitLabel)
                    obj.put("isPredefined", s.isPredefined)
                    servicesArray.put(obj)
                }
                root.put("services", servicesArray)
                val customers = JSONArray()
                customerDao.getAllCustomers().first().forEach { c -> customers.put(JSONObject().put("id", c.id).put("name", c.name).put("phone", c.phone ?: "").put("balance", c.currentBalancePaise)) }
                root.put("customers", customers)

                val responseBytes = root.toString().toByteArray(Charsets.UTF_8)
                writer.write("HTTP/1.1 200 OK\r\n")
                writer.write("Content-Type: application/json; charset=utf-8\r\n")
                writer.write("Content-Length: ${responseBytes.size}\r\n")
                writer.write("\r\n")
                writer.flush()
                socket.getOutputStream().write(responseBytes)
                socket.getOutputStream().flush()

            } else if (method == "POST" && path.startsWith("/api/sync-bills")) {
                // Read body
                require(contentLength in 1..(4 * 1024 * 1024)) { "Invalid payload size" }
                val bodyBytes = ByteArray(contentLength)
                java.io.DataInputStream(input).readFully(bodyBytes)
                val body = String(bodyBytes, Charsets.UTF_8)
                val billsArray = JSONArray(body)
                require(billsArray.length() <= 1000) { "Too many bills" }

                val results = mutableListOf<IngestResult>()
                for (i in 0 until billsArray.length()) {
                    val billObj = billsArray.getJSONObject(i)
                    val saleId = billObj.optString("id", "")
                    try {
                        val result = db.withTransaction {
                            ingestBill(billObj)
                        }
                        results.add(result)
                    } catch (e: Exception) {
                        val reason = e.message ?: "Validation failed"
                        android.util.Log.w("AliSync", "Bill $saleId rejected during ingestion: $reason")
                        results.add(IngestResult.Rejected(saleId, reason))
                    }
                }

                val responseJson = formatIngestResponseJson(results)
                val responseBytes = responseJson.toByteArray(Charsets.UTF_8)
                writer.write("HTTP/1.1 200 OK\r\n")
                writer.write("Content-Type: application/json; charset=utf-8\r\n")
                writer.write("Content-Length: ${responseBytes.size}\r\n")
                writer.write("\r\n")
                writer.flush()
                socket.getOutputStream().write(responseBytes)
                socket.getOutputStream().flush()
            } else {
                writer.write("HTTP/1.1 404 Not Found\r\n\r\n")
                writer.flush()
            }
        } catch (e: Exception) {
            android.util.Log.w("AliSync", "Sync request blocked or failed from $clientIp: ${e.message}")
            _blockedAttempts.value++
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    // ── Billing Phone Client Functions ──────────────────────────────────

    suspend fun syncWithMainPhone(ownerHost: String, port: Int = 8080, refreshOnly: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            require(preferences.deviceRole.first() == "BILLING") { "Only a Billing device can receive the Owner catalog" }
            val token = preferences.syncToken.first()
            _connectionState.value = SyncConnectionState.Syncing("Connecting to $ownerHost:$port...")

            // 1. Fetch latest catalog
            val catalogUrl = URL("https://$ownerHost:$port/api/catalog")
            val catalogConn = (catalogUrl.openConnection() as javax.net.ssl.HttpsURLConnection).apply {
                SyncTls.configure(this, preferences.ownerCertificatePin.first())
                connectTimeout = 4000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
            }
            try {
            if (catalogConn.responseCode != 200) {
                _connectionState.value = SyncConnectionState.Error("Failed to reach Main Phone (Code ${catalogConn.responseCode})")
                return@withContext false
            }

            val catalogJsonStr = catalogConn.inputStream.bufferedReader().use { it.readText() }
            val catalogRoot = JSONObject(catalogJsonStr)
            val productsArray = catalogRoot.getJSONArray("products")

            db.withTransaction {
            val customerArray = catalogRoot.optJSONArray("customers") ?: JSONArray()
            for (i in 0 until customerArray.length()) {
                val c = customerArray.getJSONObject(i)
                val id = c.getString("id")
                val pendingCredit = saleDao.getPendingSyncSales().filter { it.customerId == id }.sumOf { it.remainingCreditPaise }
                val existing = customerDao.getCustomerById(id)
                customerDao.insertCustomer((existing ?: com.alieducational.app.data.local.entity.CustomerEntity(id=id, name=c.getString("name"))).copy(name=c.getString("name"), phone=c.optString("phone").ifBlank { null }, currentBalancePaise=Math.addExact(c.getLong("balance"), pendingCredit)))
            }
            val pendingQuantities = mutableMapOf<String, Int>()
            saleDao.getPendingSyncSales().forEach { sale -> saleDao.getLinesForSale(sale.id).forEach { line ->
                if (line.productId != null) pendingQuantities[line.productId] = (pendingQuantities[line.productId] ?: 0) + line.quantityBase
            } }
            // Upsert catalog items on billing phone
            for (i in 0 until productsArray.length()) {
                val pObj = productsArray.getJSONObject(i)
                val id = pObj.getString("id")
                val existing = productDao.getProductById(id)
                val updated = (existing ?: com.alieducational.app.data.local.entity.ProductEntity(
                    id = id,
                    name = pObj.getString("name"),
                    category = pObj.getString("category")
                )).copy(
                    name = pObj.getString("name"),
                    urduName = pObj.optString("urduName").ifBlank { null },
                    category = pObj.getString("category"),
                    brand = pObj.optString("brand").ifBlank { null },
                    sku = pObj.optString("sku").ifBlank { null },
                    barcode = pObj.optString("barcode").ifBlank { null },
                    packSize = pObj.optInt("packSize", 1),
                    sellingPricePiecePaise = pObj.getLong("sellingPricePiecePaise"),
                    sellingPricePackPaise = pObj.getLong("sellingPricePackPaise"),
                    currentSellableStock = pObj.getInt("currentSellableStock") - (pendingQuantities[id] ?: 0)
                )
                productDao.insertProduct(updated)
                val quote = pObj.optString("costQuote")
                syncEventDao.insertEvent(SyncEventEntity(id = "catalog-quote-$id", eventType = "CATALOG_QUOTE", entityId = "catalog-quote-$id", payloadJson = quote, originDeviceId = "OWNER", status = "ACKNOWLEDGED"))
                val aliases = pObj.optJSONArray("aliases") ?: JSONArray()
                for (j in 0 until aliases.length()) {
                    val b = aliases.getJSONObject(j)
                    val existingAlias = db.productBarcodeDao().findByBarcode(b.getString("barcode"))
                    require(existingAlias == null || (existingAlias.productId == id && existingAlias.unitType == b.getString("unitType") && existingAlias.packQuantity == b.getInt("packQuantity"))) { "Conflicting barcode in catalog" }
                    if (existingAlias == null) db.productBarcodeDao().insertBarcode(com.alieducational.app.data.local.entity.ProductBarcodeEntity(id = b.getString("id"), barcode = b.getString("barcode"), productId = id, unitType = b.getString("unitType"), packQuantity = b.getInt("packQuantity")))
                }
            }

            val serviceArray = catalogRoot.optJSONArray("services") ?: JSONArray()
            for (i in 0 until serviceArray.length()) {
                val value = serviceArray.getJSONObject(i)
                serviceDao.insertService(com.alieducational.app.data.local.entity.ServiceDefinitionEntity(id = value.getString("id"), name = value.getString("name"), unitLabel = value.getString("unitLabel"), isPredefined = value.optBoolean("isPredefined")))
            }
            }
            } finally {
                catalogConn.disconnect()
            }
            // 2. Deliver pending bills to Main Phone
            val pendingSales = if (refreshOnly) emptyList() else saleDao.getPendingSyncSales().take(500)
            if (pendingSales.isNotEmpty()) {
                _connectionState.value = SyncConnectionState.Syncing("Delivering ${pendingSales.size} pending bills...")

                val billsArray = JSONArray()
                for (sale in pendingSales) {
                    val lines = saleDao.getLinesForSale(sale.id)
                    val sObj = JSONObject().apply {
                        put("id", sale.id)
                        put("receiptNumber", sale.receiptNumber)
                        put("customerId", sale.customerId ?: "")
                        put("customerName", sale.customerName ?: "")
                        sale.customerId?.let { id -> customerDao.getCustomerById(id)?.let { customer ->
                            put("customer", JSONObject().put("id", customer.id).put("name", customer.name).put("phone", customer.phone ?: ""))
                        } }
                        put("grossTotalPaise", sale.grossTotalPaise)
                        put("discountType", sale.discountType)
                        put("discountValuePaise", sale.discountValuePaise)
                        put("discountPercent", sale.discountPercent)
                        put("netTotalPaise", sale.netTotalPaise)
                        put("amountReceivedPaise", sale.amountReceivedPaise)
                        put("changeGivenPaise", sale.changeGivenPaise)
                        put("remainingCreditPaise", sale.remainingCreditPaise)
                        put("paymentMethod", sale.paymentMethod)
                        put("timestamp", sale.timestamp)
                        put("originDeviceId", sale.originDeviceId ?: "device-billing")

                        val lArray = JSONArray()
                        for (line in lines) {
                            val quote = syncEventDao.getEventByEntityId("sale-quote-${line.id}")?.payloadJson ?: ""
                            val lObj = JSONObject().apply {
                                put("id", line.id)
                                put("lineType", line.lineType)
                                put("productId", line.productId ?: "")
                                put("productName", line.productName)
                                put("quantityBase", line.quantityBase)
                                put("isPack", line.isPack)
                                put("unitSellingPricePaise", line.unitSellingPricePaise)
                                put("costQuote", quote)
                                put("lineGrossPaise", line.lineGrossPaise)
                                put("lineDiscountAllocatedPaise", line.lineDiscountAllocatedPaise)
                                put("lineNetPaise", line.lineNetPaise)
                                put("serviceId", line.serviceId ?: "")
                                put("serviceName", line.serviceName ?: "")
                                put("serviceOption", line.serviceOption ?: "")
                                put("unitLabel", line.unitLabel ?: "")
                            }
                            lArray.put(lObj)
                        }
                        put("lines", lArray)
                    }
                    billsArray.put(sObj)
                }

                val billsUrl = URL("https://$ownerHost:$port/api/sync-bills")
                val billsConn = (billsUrl.openConnection() as javax.net.ssl.HttpsURLConnection).apply {
                    SyncTls.configure(this, preferences.ownerCertificatePin.first())
                    connectTimeout = 6000
                    readTimeout = 10000
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Authorization", "Bearer $token")
                }
                try {
                val bodyBytes = billsArray.toString().toByteArray(Charsets.UTF_8)
                billsConn.outputStream.use { it.write(bodyBytes) }

                if (billsConn.responseCode == 200) {
                    val respStr = billsConn.inputStream.bufferedReader().use { it.readText() }
                    val respObj = JSONObject(respStr)
                    val protocolVersion = respObj.optInt("protocolVersion", 1)
                    if (protocolVersion >= 2 && respObj.has("results")) {
                        val resultsArray = respObj.getJSONArray("results")
                        val returnedIds = mutableSetOf<String>()
                        for (i in 0 until resultsArray.length()) {
                            val r = resultsArray.getJSONObject(i)
                            val id = r.getString("id")
                            val status = r.getString("status")
                            val reason = r.optString("reason", "")
                            returnedIds.add(id)
                            when (status) {
                                "ACCEPTED", "DUPLICATE" -> {
                                    saleDao.updateSaleSyncStatus(id, "SYNCED")
                                }
                                "REJECTED" -> {
                                    saleDao.updateSaleSyncStatus(id, "SYNC_REJECTED")
                                    syncEventDao.insertEvent(
                                        SyncEventEntity(
                                            id = UUID.randomUUID().toString(),
                                            eventType = "SYNC_BILL_REJECTED",
                                            entityId = id,
                                            payloadJson = reason,
                                            originDeviceId = preferences.deviceRole.first(),
                                            status = "REJECTED",
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                        }
                        val pendingIds = pendingSales.map { it.id }.toSet()
                        require(returnedIds == pendingIds) { "Invalid acknowledgement; pending bills retained" }
                    } else {
                        val ackIds = respObj.getJSONArray("acknowledgedIds")
                        require((0 until ackIds.length()).map { ackIds.getString(it) }.toSet() == pendingSales.map { it.id }.toSet() && ackIds.length() == pendingSales.size) { "Invalid acknowledgement; pending bills retained" }
                        for (i in 0 until ackIds.length()) {
                            val ackId = ackIds.getString(i)
                            saleDao.updateSaleSyncStatus(ackId, "SYNCED")
                        }
                    }
                } else {
                    _connectionState.value = SyncConnectionState.Error("Sync delivery failed (Code ${billsConn.responseCode})")
                    return@withContext false
                }
                } finally {
                    billsConn.disconnect()
                }
            }

            if (pendingSales.isNotEmpty()) {
                return@withContext syncWithMainPhone(ownerHost, port, refreshOnly = true)
            }
            _connectionState.value = SyncConnectionState.Connected(ownerHost)
            true
        } catch (e: Exception) {
            _connectionState.value = SyncConnectionState.Error("Cannot reach Main Phone: ${e.message ?: "Connection timed out"}")
            false
        }
    }

    private suspend fun ingestBill(billObj: JSONObject): IngestResult {
        val saleId = billObj.getString("id")

        // Sanity checks: non-empty lines and no future timestamp > 1 day
        require(billObj.has("lines") && billObj.getJSONArray("lines").length() > 0) { "Empty lines array" }
        val timestamp = billObj.getLong("timestamp")
        require(timestamp <= System.currentTimeMillis() + 86_400_000L) { "Bill timestamp is in the future" }

        // Deduplicate: check if already committed
        val existingSale = saleDao.getSaleById(saleId)
        if (existingSale != null) {
            val original = syncEventDao.getEventByEntityId(saleId)
            require(original?.eventType == "SYNC_BILL_INGESTION" && canon(JSONObject(original.payloadJson)) == canon(billObj)) {
                "Conflicting replay of bill identity"
            }
            return IngestResult.Duplicate(saleId)
        }

        val receiptNumber = billObj.getString("receiptNumber")
        val customerId = billObj.optString("customerId").ifBlank { null }
        val customerName = billObj.optString("customerName").ifBlank { null }
        val grossTotal = billObj.getLong("grossTotalPaise")
        val discountType = billObj.getString("discountType")
        val discountValue = billObj.getLong("discountValuePaise")
        val discountPercent = billObj.optDouble("discountPercent", 0.0)
        val netTotal = billObj.getLong("netTotalPaise")
        val amountReceived = billObj.getLong("amountReceivedPaise")
        val changeGiven = billObj.optLong("changeGivenPaise", 0L)
        val remainingCredit = billObj.optLong("remainingCreditPaise", 0L)
        val paymentMethod = billObj.getString("paymentMethod")
        val originDevice = billObj.optString("originDeviceId", "device-billing")

        require(grossTotal >= 0 && discountValue in 0..grossTotal && netTotal == grossTotal - discountValue && amountReceived >= 0 && remainingCredit == (netTotal - amountReceived).coerceAtLeast(0)) { "Invalid bill totals" }
        if (customerId != null && customerDao.getCustomerById(customerId) == null) {
            val customer = billObj.optJSONObject("customer") ?: error("Missing customer details")
            require(customer.getString("id") == customerId && customer.getString("name").isNotBlank()) { "Invalid customer identity" }
            customerDao.insertCustomer(com.alieducational.app.data.local.entity.CustomerEntity(id=customerId, name=customer.getString("name"), phone=customer.optString("phone").ifBlank { null }))
        }
        if (remainingCredit > 0) require(customerId != null) { "Credit needs a customer" }
        val sale = SaleEntity(
            id = saleId,
            receiptNumber = receiptNumber,
            customerId = customerId,
            customerName = customerName,
            grossTotalPaise = grossTotal,
            discountType = discountType,
            discountValuePaise = discountValue,
            discountPercent = discountPercent,
            netTotalPaise = netTotal,
            amountReceivedPaise = amountReceived,
            changeGivenPaise = changeGiven,
            remainingCreditPaise = remainingCredit,
            paymentMethod = paymentMethod,
            isSummaryEntry = false,
            unresolvedStockDetails = false,
            originDeviceId = originDevice,
            syncStatus = "SYNCED",
            timestamp = timestamp
        )
        saleDao.insertSale(sale)

        // Insert lines and adjust stock
        val linesArray = billObj.getJSONArray("lines")
        val saleLines = mutableListOf<SaleLineEntity>()
        for (j in 0 until linesArray.length()) {
            val lineObj = linesArray.getJSONObject(j)
            val lineType = lineObj.optString("lineType", "PRODUCT")
            val productId = lineObj.optString("productId").ifBlank { null }
            val productName = lineObj.getString("productName")
            val qtyBase = lineObj.getInt("quantityBase")
            val isPack = lineObj.optBoolean("isPack", false)
            val unitSellingPrice = lineObj.getLong("unitSellingPricePaise")
            val lineGross = lineObj.getLong("lineGrossPaise")
            val lineDiscount = lineObj.optLong("lineDiscountAllocatedPaise", 0L)
            val lineNet = lineObj.getLong("lineNetPaise")

            require(lineType in setOf("PRODUCT", "SERVICE") && (lineType != "PRODUCT" || productId != null)) { "Invalid line type or product" }
            require(qtyBase > 0 && unitSellingPrice >= 0 && lineGross >= 0 && lineDiscount in 0..lineGross && lineNet == lineGross - lineDiscount) { "Invalid sale line" }
            val costSnapshot = if (lineType == "PRODUCT" && productId != null) {
                CostQuote.resolve(lineObj.optString("costQuote"), productId)
            } else null

            val lineProfit = if (costSnapshot != null) {
                lineNet - (qtyBase * costSnapshot)
            } else null

            val line = SaleLineEntity(
                id = lineObj.optString("id").ifBlank { UUID.randomUUID().toString() },
                saleId = saleId,
                productId = productId,
                productName = productName,
                quantityBase = qtyBase,
                isPack = isPack,
                unitSellingPricePaise = unitSellingPrice,
                unitCostSnapshotPaise = costSnapshot,
                lineGrossPaise = lineGross,
                lineDiscountAllocatedPaise = lineDiscount,
                lineNetPaise = lineNet,
                lineProfitPaise = lineProfit,
                lineType = lineType,
                serviceId = lineObj.optString("serviceId").ifBlank { null },
                serviceName = lineObj.optString("serviceName").ifBlank { null },
                serviceOption = lineObj.optString("serviceOption").ifBlank { null },
                unitLabel = lineObj.optString("unitLabel").ifBlank { null }
            )
            require(saleDao.getLineById(line.id) == null) { "Sale line identity already used" }
            saleLines.add(line)

            // Deduct stock for product lines
            if (lineType == "PRODUCT" && productId != null) {
                require(productDao.getProductById(productId) != null) { "Unknown product" }
                productDao.adjustSellableStock(productId, -qtyBase)
                stockMovementDao.insertMovement(
                    StockMovementEntity(
                        productId = productId,
                        type = "SALE",
                        quantityChangeBase = -qtyBase,
                        unitCostPaise = costSnapshot ?: 0L,
                        reason = "Synced Bill $receiptNumber",
                        referenceId = saleId
                    )
                )
            }
        }
        require(saleLines.sumOf { it.lineGrossPaise } == grossTotal && saleLines.sumOf { it.lineNetPaise } == netTotal) { "Bill and line totals differ" }
        require(saleLines.map { it.id }.distinct().size == saleLines.size) { "Duplicate line identity" }
        saleDao.insertSaleLines(saleLines)
        if (saleLines.any { it.lineType == "PRODUCT" && (it.unitCostSnapshotPaise == null || (it.productId?.let { id -> productDao.getProductById(id)?.currentSellableStock } ?: 0) < 0) }) {
            saleDao.updateSale(sale.copy(unresolvedStockDetails = true))
        }

        // Customer debt adjustment if credit
        if (customerId != null && remainingCredit > 0) {
            customerDao.adjustBalance(customerId, remainingCredit)
        }

        // Durable sync event entry (A03 / A09)
        syncEventDao.insertEvent(
            SyncEventEntity(
                id = UUID.randomUUID().toString(),
                eventType = "SYNC_BILL_INGESTION",
                entityId = saleId,
                payloadJson = billObj.toString(),
                originDeviceId = originDevice,
                status = "ACKNOWLEDGED",
                timestamp = System.currentTimeMillis()
            )
        )
        return IngestResult.Accepted(saleId)
    }
}

sealed class IngestResult {
    abstract val id: String
    data class Accepted(override val id: String) : IngestResult()
    data class Duplicate(override val id: String) : IngestResult()
    data class Rejected(override val id: String, val reason: String) : IngestResult()
}

internal fun canon(json: String): String {
    val trimmed = json.trim()
    if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return trimmed
    val inner = trimmed.substring(1, trimmed.length - 1)
    val map = sortedMapOf<String, String>()
    var i = 0
    val n = inner.length

    fun skipWs() {
        while (i < n && inner[i].isWhitespace()) i++
    }

    while (i < n) {
        skipWs()
        if (i >= n) break
        if (inner[i] == ',') { i++; continue }
        if (inner[i] != '"') break
        i++
        val keyStart = i
        while (i < n && inner[i] != '"') {
            if (inner[i] == '\\') i++
            i++
        }
        val key = inner.substring(keyStart, i)
        if (i < n) i++
        skipWs()
        if (i < n && inner[i] == ':') i++
        skipWs()
        if (i >= n) break

        val valueStart = i
        when (inner[i]) {
            '"' -> {
                i++
                while (i < n && inner[i] != '"') {
                    if (inner[i] == '\\') i++
                    i++
                }
                if (i < n) i++
            }
            '{' -> {
                var depth = 1
                i++
                var inStr = false
                while (i < n && depth > 0) {
                    val c = inner[i]
                    if (c == '\\' && inStr) { i += 2; continue }
                    if (c == '"') inStr = !inStr
                    else if (!inStr) {
                        if (c == '{') depth++
                        else if (c == '}') depth--
                    }
                    i++
                }
            }
            '[' -> {
                var depth = 1
                i++
                var inStr = false
                while (i < n && depth > 0) {
                    val c = inner[i]
                    if (c == '\\' && inStr) { i += 2; continue }
                    if (c == '"') inStr = !inStr
                    else if (!inStr) {
                        if (c == '[') depth++
                        else if (c == ']') depth--
                    }
                    i++
                }
            }
            else -> {
                while (i < n && inner[i] != ',' && inner[i] != '}') i++
            }
        }
        val value = inner.substring(valueStart, i).trim()
        if (key != "customer") {
            map[key] = value
        }
    }

    return map.entries.joinToString(separator = ",", prefix = "{", postfix = "}") { (k, v) ->
        "\"${k}\":$v"
    }
}

internal fun canon(o: JSONObject): String = canon(o.toString())

internal fun formatIngestResponseJson(results: List<IngestResult>): String {
    val ackIds = results.filter { it is IngestResult.Accepted || it is IngestResult.Duplicate }.map { "\"${it.id}\"" }
    val resList = results.map { r ->
        when (r) {
            is IngestResult.Accepted -> "{\"id\":\"${r.id}\",\"status\":\"ACCEPTED\"}"
            is IngestResult.Duplicate -> "{\"id\":\"${r.id}\",\"status\":\"DUPLICATE\"}"
            is IngestResult.Rejected -> {
                val escapedReason = r.reason.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
                "{\"id\":\"${r.id}\",\"status\":\"REJECTED\",\"reason\":\"$escapedReason\"}"
            }
        }
    }
    return "{\"status\":\"SUCCESS\",\"protocolVersion\":2,\"acknowledgedIds\":[${ackIds.joinToString(",")}],\"results\":[${resList.joinToString(",")}]}"
}

internal fun formatIngestResponse(results: List<IngestResult>): JSONObject =
    JSONObject(formatIngestResponseJson(results))
