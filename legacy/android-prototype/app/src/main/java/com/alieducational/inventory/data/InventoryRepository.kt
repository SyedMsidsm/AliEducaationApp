package com.alieducational.inventory.data

import java.util.Locale

class InventoryRepository(private val db:ShopDatabase) {
    fun edit(product:ProductRecord,commandId:String) {
        require(product.packSellingPaise==null || product.packSellingPaise>=0) {"Pack price cannot be negative"}
        require(commandId.isNotBlank() && product.id.isNotBlank())
        require(product.name.isNotBlank() && product.name.length<=150) {"Enter a product name (1–150 characters)"}
        require(product.packSize in 1..100000 && product.sellingPaise>=0 && product.lowStock in 0..100000000) {"Invalid pack size, price or threshold"}
        product.name=product.name.trim()
        product.sku=product.sku?.trim()?.uppercase(Locale.ROOT)?.takeIf{it.isNotEmpty()}
        db.runInTransaction {
            val previous=db.inventory().movement(commandId)
            if(previous!=null) {
                check(previous.productId==product.id && previous.reason=="PRODUCT_EDIT") {"Operation ID already used"}
                return@runInTransaction
            }
            val old=checkNotNull(db.inventory().product(product.id)) {"Product not found"}
            fun snapshot(p:ProductRecord)=org.json.JSONObject().put("name",p.name).put("sku",p.sku ?: "")
                .put("variant",p.variant).put("category",p.category).put("brand",p.brand)
                .put("packSize",p.packSize).put("sellingPaise",p.sellingPaise).put("packSellingPaise",p.packSellingPaise ?: org.json.JSONObject.NULL)
                .put("lowStock",p.lowStock).put("archived",p.archived)
            val note=org.json.JSONObject().put("before",snapshot(old)).put("after",snapshot(product)).toString()
            check(db.inventory().updateProduct(product)==1)
            db.inventory().insertMovement(StockMovement().apply {
                id=commandId;productId=product.id;reason="PRODUCT_EDIT";packSizeSnapshot=old.packSize
                createdAt=System.currentTimeMillis();this.note=note
            })
        }
    }
    fun setCount(productId:String,count:Long,note:String,commandId:String) {
        require(commandId.isNotBlank() && note.isNotBlank()) {"Enter a reason or note"}
        require(count in 0..100000000) {"Count must be a nonnegative whole number"}
        db.runInTransaction {
            val previous=db.inventory().movement(commandId)
            if(previous!=null) {
                check(previous.productId==productId && previous.reason=="COUNT_CORRECTION") {"Operation ID already used"}
                return@runInTransaction
            }
            val product=checkNotNull(db.inventory().product(productId)) {"Product not found"}
            check(!product.archived) {"Product is archived"}
            val before=db.inventory().sellable(productId)
            db.inventory().insertMovement(StockMovement().apply {
                id=commandId;this.productId=productId;reason="COUNT_CORRECTION"
                sellableDelta=Math.subtractExact(count,before);packSizeSnapshot=product.packSize
                createdAt=System.currentTimeMillis();this.note="Counted $count pieces; previously $before. "+note.trim()
            })
        }
    }
    fun create(product:ProductRecord, opening:Long, totalCostPaise:Long?, commandId:String) {
        require(product.packSellingPaise==null || product.packSellingPaise>=0) {"Pack price cannot be negative"}
        require(product.id.isNotBlank() && commandId.isNotBlank())
        require(product.name.trim().isNotEmpty() && product.name.length<=150) {"Enter a product name (1–150 characters)"}
        require(product.packSize in 1..100000 && opening in 0..100000000 && product.lowStock in 0..100000000) {"Invalid pack size or quantity"}
        require(product.sellingPaise>=0 && (totalCostPaise==null || totalCostPaise>=0)) {"Amounts cannot be negative"}
        require(opening>0 || totalCostPaise==null || totalCostPaise==0L) {"Opening cost requires opening stock"}
        product.name=product.name.trim()
        product.sku=product.sku?.trim()?.uppercase(Locale.ROOT)?.takeIf {it.isNotEmpty()}
        db.runInTransaction {
            val existing=db.inventory().movement(commandId)
            if(existing!=null) {
                check(existing.productId==product.id && existing.reason=="OPENING") {"Operation ID already used"}
                return@runInTransaction
            }
            db.inventory().insertProduct(product)
            db.inventory().insertMovement(StockMovement().apply {
                id=commandId;productId=product.id;reason="OPENING";sellableDelta=opening
                packSizeSnapshot=product.packSize;createdAt=System.currentTimeMillis();openingCostPaise=totalCostPaise
                note="Opening stock"
            })
        }
    }
    fun move(productId:String, quantity:Long, packs:Boolean, reason:String, note:String, allowShortage:Boolean, commandId:String) {
        require(commandId.isNotBlank()) {"Missing operation ID"}
        require(quantity in 1..100000000) {"Enter a positive whole quantity"}
        require(reason in setOf("SHOP_USE","GIVEAWAY","DAMAGE","SUPPLIER_RETURN","CORRECTION_ADD","CORRECTION_REMOVE")) {"Invalid stock reason"}
        require(note.isNotBlank()) {"Enter a reason or note"}
        db.runInTransaction {
            val previous=db.inventory().movement(commandId)
            if(previous!=null) {
                check(previous.productId==productId && previous.reason==reason) {"Operation ID already used"}
                return@runInTransaction
            }
            val product=checkNotNull(db.inventory().product(productId)) {"Product not found"}
            check(!product.archived) {"Product is archived"}
            val units=Math.multiplyExact(quantity,if(packs)product.packSize.toLong() else 1L)
            require(units<=100000000) {"Quantity too large"}
            val add=reason=="CORRECTION_ADD"
            val current=db.inventory().sellable(productId)
            if(!add && units>current && !allowShortage) throw IllegalArgumentException("Insufficient stock. Review and confirm the shortage.")
            db.inventory().insertMovement(StockMovement().apply {
                id=commandId;this.productId=productId;this.reason=reason
                sellableDelta=if(add) units else -units;damagedDelta=if(reason=="DAMAGE") units else 0
                packSizeSnapshot=product.packSize;createdAt=System.currentTimeMillis();this.note=note.trim()
            })
        }
    }
}
