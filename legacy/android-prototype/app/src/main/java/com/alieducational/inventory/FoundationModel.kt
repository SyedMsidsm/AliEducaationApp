package com.alieducational.inventory

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import com.alieducational.inventory.data.*
import java.util.concurrent.Executors

class FoundationModel(application: Application) : AndroidViewModel(application) {
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val store = EncryptedStore(application)
    private val boot = application.getSharedPreferences("appearance", 0)
    private val session = SessionPolicy()
    private var generation = 0
    private var draftRevision = 0L
    var unlocked by mutableStateOf(false); private set
    var opening by mutableStateOf(false); private set
    var foreground by mutableStateOf(false); private set
    var urdu by mutableStateOf(boot.getBoolean("urdu", false)); private set
    var dark by mutableStateOf(boot.getBoolean("dark", true)); private set
    var settings by mutableStateOf(SettingsRecord()); private set
    var draft by mutableStateOf(""); private set
    var draftStatus by mutableStateOf(""); private set
    var message by mutableStateOf(""); private set
    var authMethod by mutableStateOf(""); private set
    var savingSettings by mutableStateOf(false); private set
    var stock by mutableStateOf<List<StockRow>>(emptyList()); private set
    var movements by mutableStateOf<List<StockMovement>>(emptyList()); private set
    var inventoryBusy by mutableStateOf(false); private set
    var inventoryMessage by mutableStateOf(""); private set
    var inventoryForm by mutableStateOf("{}"); private set

    fun updateInventoryForm(value:String) {
        if(!unlocked || inventoryBusy) return
        inventoryForm=value
        val ticket=generation
        worker.execute {
            try { store.db.foundation().saveDraft(DraftRecord().apply {id="inventory-form";text=value;updatedAt=System.currentTimeMillis()}) }
            catch(error:Exception) { main.post {if(ticket==generation) inventoryMessage="Draft not saved. Please retry."} }
        }
    }
    fun inventoryAction(action:(InventoryRepository)->Unit) {
        if(!unlocked || inventoryBusy) return
        inventoryBusy=true;inventoryMessage=""
        val ticket=generation
        worker.execute {
            try {
                store.db.runInTransaction {
                    action(InventoryRepository(store.db))
                    store.db.foundation().saveDraft(DraftRecord().apply {id="inventory-form";text="{}"})
                }
                val rows=store.db.inventory().stock()
                main.post {if(ticket==generation) {stock=rows;inventoryForm="{}";inventoryBusy=false;inventoryMessage="Saved";movements=emptyList()} }
            } catch(error:Exception) {
                main.post {if(ticket==generation) {inventoryBusy=false;inventoryMessage=if(error is android.database.sqlite.SQLiteConstraintException || error.javaClass.simpleName.contains("Constraint")) "Duplicate SKU or operation. Nothing was changed." else error.message ?: "Not saved. Please retry."} }
            }
        }
    }
    fun loadHistory(id:String) {
        if(!unlocked)return
        val ticket=generation
        worker.execute {
            try {val rows=store.db.inventory().history(id);main.post {if(ticket==generation)movements=rows}}
            catch(error:Exception){main.post{if(ticket==generation)inventoryMessage="Could not load history"}}
        }
    }

    private val clock = object : Runnable {
        override fun run() {
            if (session.expired(SystemClock.elapsedRealtime())) lock()
            main.postDelayed(this, 500)
        }
    }
    init { main.post(clock) }
    fun resume() { if(session.expired(SystemClock.elapsedRealtime())) lock(); foreground=true }
    fun obscure() { foreground=false }
    fun touch() {
        if (session.expired(SystemClock.elapsedRealtime())) { lock(); return }
        if(foreground) session.touch(SystemClock.elapsedRealtime())
    }
    fun reportAuthentication(value: String) { message=value }
    fun selectLockedLanguage(value: Boolean) {
        urdu=value
        boot.edit().putBoolean("urdu",value).apply() // Appearance only; no shop data.
    }
    fun authenticated(method: String) {
        if(opening || unlocked) return
        opening=true
        message=""
        val ticket=++generation
        worker.execute {
            try {
                store.open()
                val saved=store.db.foundation().settings()
                val note=store.db.foundation().draft("foundation-note")
                val inventoryDraft=store.db.foundation().draft("inventory-form")?.text ?: "{}"
                val stockRows=store.db.inventory().stock()
                main.post {
                    if(ticket==generation) {
                        settings=saved
                        stock=stockRows;inventoryForm=inventoryDraft
                        if(saved.setupComplete) { urdu=saved.language=="ur"; dark=saved.dark }
                        draft=note?.text ?: ""
                        draftStatus=if(note==null) "" else "Saved"
                        session.timeoutMs=saved.lockSeconds*1000L
                        session.unlock(SystemClock.elapsedRealtime())
                        opening=false; unlocked=true; authMethod=method
                    }
                }
            } catch (error: Exception) {
                store.close()
                main.post {
                    if(ticket==generation) {
                        opening=false; unlocked=false
                        message=if(urdu) "ڈیٹا نہیں کھل سکا۔ دوبارہ تصدیق کریں۔ ڈیٹا حذف نہ کریں۔" else
                            "Could not open protected data. Authenticate again. Do not clear app data. ("+error.javaClass.simpleName+")"
                    }
                }
            }
        }
    }
    fun lock() {
        generation++
        session.lock()
        unlocked=false; opening=false; draft=""; draftStatus=""; authMethod=""; savingSettings=false
        stock=emptyList();movements=emptyList();inventoryForm="{}";inventoryBusy=false;inventoryMessage=""
        worker.execute { store.close() }
    }
    fun saveSettings(shop: String, languageUrdu: Boolean, isDark: Boolean, seconds: Int) {
        if(!unlocked || savingSettings) return
        if(shop.isBlank() || shop.length>100 || seconds !in listOf(30,60,300)) {
            message=if(urdu) "دکان کا نام درست درج کریں۔" else "Enter a shop name of 1–100 characters."
            return
        }
        savingSettings=true
        val updated=SettingsRecord().apply { shopName=shop.trim();language=if(languageUrdu) "ur" else "en";dark=isDark;lockSeconds=seconds;setupComplete=true }
        val ticket=generation
        worker.execute {
            try {
                store.db.runInTransaction { store.db.foundation().saveSettings(updated) }
                main.post {
                    if(ticket==generation) {
                        settings=updated;urdu=languageUrdu;dark=isDark;session.timeoutMs=seconds*1000L
                        boot.edit().putBoolean("urdu",urdu).putBoolean("dark",dark).apply()
                        savingSettings=false;message=if(urdu) "محفوظ ہو گیا" else "Saved"
                    }
                }
            } catch(error: Exception) {
                main.post { if(ticket==generation) { savingSettings=false;message=if(urdu) "محفوظ نہیں ہوا۔ دوبارہ کوشش کریں۔" else "Not saved. Please retry." } }
            }
        }
    }
    fun editDraft(text: String) {
        if(!unlocked || text.length>2000) return
        draft=text;draftStatus="Saving"
        val revision=++draftRevision
        val ticket=generation
        val record=DraftRecord().apply { this.text=text;updatedAt=System.currentTimeMillis() }
        worker.execute {
            try {
                store.db.foundation().saveDraft(record)
                main.post { if(ticket==generation && revision==draftRevision) draftStatus="Saved" }
            } catch(error: Exception) {
                main.post { if(ticket==generation && revision==draftRevision) draftStatus="Not saved" }
            }
        }
    }
    override fun onCleared() {
        main.removeCallbacks(clock)
        worker.execute { store.close() }
        worker.shutdown()
    }
}
