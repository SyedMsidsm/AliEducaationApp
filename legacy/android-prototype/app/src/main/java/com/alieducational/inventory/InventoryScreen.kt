package com.alieducational.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alieducational.inventory.data.ProductRecord
import org.json.JSONObject
import java.util.UUID
import java.text.DateFormat
import java.util.Date

@Composable fun InventoryScreen(model:FoundationModel) {
    fun t(en:String,ur:String)=if(model.urdu) ur else en
    val form=JSONObject(model.inventoryForm)
    fun field(key:String,default:String="")=form.optString(key,default)
    fun set(key:String,value:String) {model.updateInventoryForm(JSONObject(model.inventoryForm).put(key,value).toString())}
    var search by remember {mutableStateOf("")}
    var lowOnly by remember {mutableStateOf(false)}
    var showArchived by remember {mutableStateOf(false)}
    var browsing by remember {mutableStateOf(false)}
    var localError by remember {mutableStateOf("")}
    val mode=field("mode")
    Text(t("Stock","اسٹاک"),style=MaterialTheme.typography.headlineSmall)
    Text(t("TEST data only • Imports and purchases are still being built.","صرف ٹیسٹ ڈیٹا • درآمد اور خریداری ابھی تیار ہو رہی ہیں۔"))
    if(mode.isEmpty() || browsing) {
        if(mode.isNotEmpty()) Button(onClick={browsing=false}) {Text(t("Resume unfinished entry","نامکمل اندراج جاری رکھیں"))}
        Button(enabled=mode.isEmpty(),onClick={model.updateInventoryForm(JSONObject().put("mode","create").put("id",UUID.randomUUID().toString()).put("command",UUID.randomUUID().toString()).toString())}) {Text(t("Add product","شے شامل کریں"))}
        OutlinedTextField(search,{search=it},label={Text(t("Search name, SKU, category or brand","نام، کوڈ، زمرہ یا برانڈ تلاش کریں"))},modifier=Modifier.fillMaxWidth())
        Row {Checkbox(lowOnly,{lowOnly=it});Text(t("Low stock only","صرف کم اسٹاک"))}
        Row {Checkbox(showArchived,{showArchived=it});Text(t("Show archived products","محفوظ شدہ پرانی اشیا دکھائیں"))}
        val rows=model.stock.filter {it.product.archived==showArchived && (!lowOnly || it.sellable<=it.product.lowStock) && listOf(it.product.name,it.product.sku ?: "",it.product.category,it.product.brand,it.product.variant).any {v->v.contains(search,true)}}
        if(rows.isEmpty()) Text(t("No matching products. Add a product to begin.","کوئی شے نہیں ملی۔ شروع کرنے کے لیے شے شامل کریں۔"))
        rows.forEach {row->
            Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(12.dp)) {
                    Text(row.product.name+" "+row.product.variant,style=MaterialTheme.typography.titleMedium)
                    Text(row.product.sku ?: "")
                    Text(if(row.sellable>=0) t("${row.sellable/row.product.packSize} packs + ${row.sellable%row.product.packSize} pieces (${row.sellable} pieces)","${row.sellable/row.product.packSize} پیک + ${row.sellable%row.product.packSize} عدد (کل ${row.sellable})") else t("Shortage: ${-row.sellable} pieces","کمی: ${-row.sellable} عدد"))
                    Text(t("Damaged: ${row.damaged} • Pack size: ${row.product.packSize}","خراب: ${row.damaged} • پیک: ${row.product.packSize}"))
                    Text(t("Piece price: ","فی عدد قیمت: ")+"\u2066"+Money.text(row.product.sellingPaise)+"\u2069")
                    Text(t("Pack price: ","پیک کی قیمت: ")+(row.product.packSellingPaise?.let{"\u2066"+Money.text(it)+"\u2069"} ?: t("Not set","مقرر نہیں")))
                    Text(t("Profit unavailable until cost and sale records are complete.","لاگت اور فروخت مکمل ہونے تک منافع دستیاب نہیں۔"))
                    Row {
                        TextButton(enabled=!row.product.archived && mode.isEmpty(),onClick={model.updateInventoryForm(JSONObject().put("mode","move").put("id",row.product.id).put("command",UUID.randomUUID().toString()).toString());browsing=false}) {Text(t("Adjust / remove","درست کریں / نکالیں"))}
                        TextButton(onClick={model.loadHistory(row.product.id)}) {Text(t("History","تاریخ"))}
                    }
                    TextButton(enabled=mode.isEmpty(),onClick={
                        val p=row.product
                        model.updateInventoryForm(JSONObject().put("mode","edit").put("id",p.id).put("command",UUID.randomUUID().toString())
                            .put("name",p.name).put("sku",p.sku ?: "").put("variant",p.variant).put("category",p.category).put("brand",p.brand)
                            .put("pack",p.packSize.toString()).put("price",Money.input(p.sellingPaise)).put("packPrice",p.packSellingPaise?.let{Money.input(it)} ?: "").put("low",p.lowStock.toString()).put("archived",p.archived.toString()).toString())
                        browsing=false
                    }) {Text(t("Edit product","شے کی ترمیم"))}
                }
            }
        }
        model.movements.forEach {movement ->
            Text(DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(movement.createdAt))+" • "+movement.reason+" • "+movement.sellableDelta+" / "+movement.damagedDelta+" • "+movement.note)
        }
    } else {
        @Composable fun entry(key:String,en:String,ur:String,default:String="") {
            OutlinedTextField(field(key,default),{set(key,it)},label={Text(t(en,ur))},modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!model.inventoryBusy)
        }
        TextButton(onClick={browsing=true}) {Text(t("Back to stock (keep draft)","اسٹاک پر واپس (مسودہ محفوظ)"))}
        if(mode=="create" || mode=="edit") {
            entry("name","Product name","شے کا نام")
            entry("sku","SKU (optional; unique)","کوڈ (اختیاری؛ منفرد)")
            entry("variant","Variant (optional)","قسم (اختیاری)")
            entry("category","Category","زمرہ")
            entry("brand","Brand","برانڈ")
            entry("pack","Pieces per pack","پیک میں عدد","1")
            if(mode=="create") {
                entry("opening","Opening pieces","ابتدائی عدد","0")
                entry("cost","Total opening cost ₹ (blank = unknown)","کل ابتدائی لاگت ₹ (نامعلوم ہو تو خالی)")
            } else {
                Text(t("Editing pack size keeps the same total pieces and preserves earlier pack conversions.","پیک کی تبدیلی سے کل عدد نہیں بدلتے اور سابقہ تبدیلیاں محفوظ رہتی ہیں۔"))
                Row {Checkbox(field("archived")=="true",{set("archived",it.toString())});Text(t("Archive product (keep stock/history)","شے کو پرانی فہرست میں رکھیں (اسٹاک اور تاریخ برقرار)"))}
            }
            entry("price","Piece selling price ₹","فی عدد فروخت ₹","0")
            entry("packPrice","Pack selling price ₹ (optional)","پیک کی فروخت قیمت ₹ (اختیاری)")
            Text(t("Pack price is independent of the piece price. Leave blank if not set.","پیک کی قیمت فی عدد قیمت سے الگ ہے۔ مقرر نہ ہو تو خالی چھوڑیں۔"))
            entry("low","Low-stock threshold","کم اسٹاک کی حد","5")
            Button(enabled=!model.inventoryBusy,onClick={
                try {
                    val product=ProductRecord().apply {
                        id=field("id");name=field("name");sku=field("sku");variant=field("variant");category=field("category");brand=field("brand")
                        packSize=field("pack","1").toInt();sellingPaise=Money.nonnegative(field("price","0"));lowStock=field("low","5").toLong()
                        archived=field("archived")=="true"
                        packSellingPaise=field("packPrice").takeIf{it.isNotBlank()}?.let{Money.nonnegative(it)}
                    }
                    val opening=field("opening","0").toLong()
                    val cost=field("cost").takeIf{it.isNotBlank()}?.let{Money.nonnegative(it)}
                    val command=field("command")
                    localError=""
                    model.inventoryAction {if(mode=="edit") it.edit(product,command) else it.create(product,opening,cost,command)}
                } catch(error:Exception) {localError=t("Check quantities and amounts. Use whole numbers for pieces.","مقدار اور رقم درست کریں۔ عدد کے لیے صحیح عدد لکھیں۔")}
            }) {Text(if(mode=="edit") t("Save product changes","شے کی تبدیلیاں محفوظ کریں") else t("Save product and opening stock","شے اور ابتدائی اسٹاک محفوظ کریں"))}
        } else {
            val product=model.stock.firstOrNull{it.product.id==field("id")}
            Text(product?.product?.name ?: "")
            entry("quantity","Quantity","مقدار","1")
            if(field("reason")!="COUNT_CORRECTION") Row {Checkbox(field("packs")=="true",{set("packs",it.toString())});Text(t("Quantity is in packs","مقدار پیک میں ہے"))}
            TextButton(onClick={set("reason","COUNT_CORRECTION")}) {Text(t("Set counted total (pieces)","گنی ہوئی کل تعداد مقرر کریں"))}
            if(field("reason")=="COUNT_CORRECTION") Text(t("Quantity above is the new sellable total in pieces. Saving records the difference.","اوپر مقدار قابل فروخت اشیا کا نیا کل ہے۔ فرق تاریخ میں محفوظ ہوگا۔"))
            val reasons=listOf("SHOP_USE" to t("Shop use","دکان میں استعمال"),"GIVEAWAY" to t("Giveaway","مفت دیا"),"DAMAGE" to t("Move to damaged","خراب اسٹاک میں منتقل"),"SUPPLIER_RETURN" to t("Supplier return (stock only)","سپلائر کو واپسی (صرف اسٹاک)"),"CORRECTION_ADD" to t("Correction: add","درستی: شامل"),"CORRECTION_REMOVE" to t("Correction: remove","درستی: کم"))
            reasons.forEach{(value,label)->Row {RadioButton(field("reason","SHOP_USE")==value,{set("reason",value)});Text(label)}}
            entry("note","Reason / note (required)","وجہ / نوٹ (ضروری)")
            Text(t("This is not a sale or payment. Supplier returns do not change money owed.","یہ فروخت یا ادائیگی نہیں۔ سپلائر کو واپسی سے رقم کا بقایا نہیں بدلتا۔"))
            val units=field("quantity","1").toLongOrNull()?.let {runCatching{Math.multiplyExact(it,if(field("packs")=="true")product?.product?.packSize?.toLong() ?: 1L else 1L)}.getOrNull()}
            val shortage=field("reason","SHOP_USE") !in listOf("CORRECTION_ADD","COUNT_CORRECTION") && units!=null && units>(product?.sellable ?: 0)
            if(shortage) {
                Text(t("Insufficient stock. Proceeding creates a visible shortage; cost/profit needs review.","اسٹاک ناکافی ہے۔ جاری رکھنے سے کمی ظاہر ہوگی؛ لاگت اور منافع کی جانچ درکار ہے۔"))
                Row {Checkbox(field("allow")=="true",{set("allow",it.toString())});Text(t("I confirm this shortage","میں اس کمی کی تصدیق کرتا ہوں"))}
            }
            Button(enabled=!model.inventoryBusy && (!shortage || field("allow")=="true"),onClick={
                try {
                    val id=field("id");val q=field("quantity","1").toLong();val packs=field("packs")=="true";val reason=field("reason","SHOP_USE");val note=field("note");val allow=field("allow")=="true";val command=field("command")
                    localError=""
                    model.inventoryAction{if(reason=="COUNT_CORRECTION")it.setCount(id,q,note,command) else it.move(id,q,packs,reason,note,allow,command)}
                }catch(error:Exception){localError=t("Enter a valid whole quantity.","صحیح عدد درج کریں۔")}
            }){Text(t("Save stock movement","اسٹاک کی تبدیلی محفوظ کریں"))}
        }
        Text(t("Unfinished entries are retained. They do not change stock until saved.","نامکمل اندراج محفوظ رہتا ہے۔ محفوظ کرنے تک اسٹاک نہیں بدلتا۔"))
    }
    if(localError.isNotEmpty())Text(localError)
    if(model.inventoryMessage.isNotEmpty())Text(if(model.urdu && model.inventoryMessage=="Saved") "محفوظ ہو گیا" else model.inventoryMessage)
}
