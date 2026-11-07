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
        } catch (_: Exception) {}

        return "127.0.0.1"
    }

    // ── Owner Server Functions ─────────────────────────────────────────

    fun startOwnerServer(port: Int = 8080) {
        if (serverJob != null && serverJob?.isActive == true) return

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                val ip = getLocalIpAddress()
                _connectionState.value = SyncConnectionState.Hosting(ip, port)

                while (serverSocket?.isClosed == false) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch {
                        handleClientRequest(clientSocket)
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
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = OutputStreamWriter(socket.getOutputStream())

            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0]
            val path = parts[1]

            // Read headers to get content length and authorization
            var contentLength = 0
            var authHeader: String? = null
            var line: String?
            while (reader.readLine().also { line = it } != null) {
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

            if (providedToken.isNullOrEmpty() || providedToken != requiredToken) {
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
                val bodyChars = CharArray(contentLength)
                var readTotal = 0
                while (readTotal < contentLength) {
                    val r = reader.read(bodyChars, readTotal, contentLength - readTotal)
                    if (r < 0) break
                    readTotal += r
                }
                val body = String(bodyChars, 0, readTotal)
                val billsArray = JSONArray(body)

                val acknowledgedIds = JSONArray()

                // A03: Atomically commit incoming sales, stock, and customer ledgers in one Room transaction
                db.withTransaction {
                    for (i in 0 until billsArray.length()) {
                        val billObj = billsArray.getJSONObject(i)
                        val saleId = billObj.getString("id")

                        // Deduplicate: check if already committed
                        val existingSale = saleDao.getSaleById(saleId)
                        if (existingSale == null) {
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
                            val timestamp = billObj.getLong("timestamp")
                            val originDevice = billObj.optString("originDeviceId", "device-billing")

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

                                // Preserve valid sale-time cost snapshot if present; otherwise resolve from Owner DB (A07)
                                val costSnapshot = if (lineObj.has("unitCostSnapshotPaise") && !lineObj.isNull("unitCostSnapshotPaise")) {
                                    lineObj.getLong("unitCostSnapshotPaise")
                                } else if (lineType == "PRODUCT" && productId != null) {
                                    val p = productDao.getProductById(productId)
                                    p?.movingAvgCostPaise
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
                                saleLines.add(line)

                                // Deduct stock for product lines
                                if (lineType == "PRODUCT" && productId != null) {
                                    productDao.adjustSellableStock(productId, -qtyBase)
                                    val product = productDao.getProductById(productId)
                                    stockMovementDao.insertMovement(
                                        StockMovementEntity(
                                            productId = productId,
                                            type = "SALE",
                                            quantityChangeBase = -qtyBase,
                                            unitCostPaise = product?.movingAvgCostPaise ?: 0L,
                                            reason = "Synced Bill $receiptNumber",
                                            referenceId = saleId
                                        )
                                    )
                                }
                            }
                            saleDao.insertSaleLines(saleLines)

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
                        }

                        acknowledgedIds.put(saleId)
                    }
                }

                val responseRoot = JSONObject()
                responseRoot.put("status", "SUCCESS")
                responseRoot.put("acknowledgedIds", acknowledgedIds)

                val responseBytes = responseRoot.toString().toByteArray(Charsets.UTF_8)
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
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    // ── Billing Phone Client Functions ──────────────────────────────────

    suspend fun syncWithMainPhone(ownerHost: String, port: Int = 8080): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = preferences.syncToken.first()
            _connectionState.value = SyncConnectionState.Syncing("Connecting to $ownerHost:$port...")

            // 1. Fetch latest catalog
            val catalogUrl = URL("http://$ownerHost:$port/api/catalog")
            val catalogConn = (catalogUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (catalogConn.responseCode != 200) {
                _connectionState.value = SyncConnectionState.Error("Failed to reach Main Phone (Code ${catalogConn.responseCode})")
                return@withContext false
            }

            val catalogJsonStr = catalogConn.inputStream.bufferedReader().use { it.readText() }
            val catalogRoot = JSONObject(catalogJsonStr)
            val productsArray = catalogRoot.getJSONArray("products")

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
                    currentSellableStock = pObj.getInt("currentSellableStock")
                )
                productDao.insertProduct(updated)
            }

            // 2. Deliver pending bills to Main Phone
            val pendingSales = saleDao.getPendingSyncSales()
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
                            val lObj = JSONObject().apply {
                                put("id", line.id)
                                put("lineType", line.lineType)
                                put("productId", line.productId ?: "")
                                put("productName", line.productName)
                                put("quantityBase", line.quantityBase)
                                put("isPack", line.isPack)
                                put("unitSellingPricePaise", line.unitSellingPricePaise)
                                put("unitCostSnapshotPaise", line.unitCostSnapshotPaise ?: JSONObject.NULL)
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

                val billsUrl = URL("http://$ownerHost:$port/api/sync-bills")
                val billsConn = (billsUrl.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 10000
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Authorization", "Bearer $token")
                }

                val bodyBytes = billsArray.toString().toByteArray(Charsets.UTF_8)
                billsConn.outputStream.use { it.write(bodyBytes) }

                if (billsConn.responseCode == 200) {
                    val respStr = billsConn.inputStream.bufferedReader().use { it.readText() }
                    val respObj = JSONObject(respStr)
                    val ackIds = respObj.getJSONArray("acknowledgedIds")
                    for (i in 0 until ackIds.length()) {
                        val ackId = ackIds.getString(i)
                        saleDao.updateSaleSyncStatus(ackId, "SYNCED")
                    }
                } else {
                    _connectionState.value = SyncConnectionState.Error("Sync delivery failed (Code ${billsConn.responseCode})")
                    return@withContext false
                }
            }

            _connectionState.value = SyncConnectionState.Connected(ownerHost)
            true
        } catch (e: Exception) {
            _connectionState.value = SyncConnectionState.Error("Cannot reach Main Phone: ${e.message ?: "Connection timed out"}")
            false
        }
    }
}
