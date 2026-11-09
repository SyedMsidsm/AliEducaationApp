package com.alieducational.app.data.service.import

import java.security.MessageDigest

data class ColumnSuggestion(
    val columnIndex: Int,
    val confidence: Int // 100 = exact, 80 = synonym
)

object AutoMapper {
    private val synonymMap = mapOf(
        "name" to listOf("name", "item name", "product name", "item", "product", "title", "particulars", "description", "item_name", "product_name"),
        "category" to listOf("category", "group", "type", "dept", "department", "cat", "product_category", "item_category"),
        "brand" to listOf("brand", "make", "mfr", "manufacturer", "company"),
        "sku" to listOf("sku", "item code", "product code", "code", "article", "art no", "item_code", "product_code"),
        "barcode" to listOf("barcode", "upc", "ean", "ean13", "ean-13", "code128", "gtin", "bar code"),
        "openingQty" to listOf("qty", "quantity", "stock", "opening stock", "opening qty", "pcs", "pieces", "count", "units", "qty in pcs", "stock_qty", "current_stock"),
        "purchaseCost" to listOf("cost", "purchase cost", "c.p.", "cp", "buy price", "purchase price", "cost price", "purchase rate", "cost_price", "purchase_cost"),
        "sellingPrice" to listOf("selling price", "sell price", "s.p.", "sp", "mrp", "rate", "sale price", "retail price", "price", "selling_price"),
        "wholesalePrice" to listOf("wholesale", "wholesale price", "w.s.", "ws", "dealer price", "trade price", "wholesale_price"),
        "packSize" to listOf("pack size", "pack", "units/pack", "pcs/pack", "pack_size", "units per pack", "pkg"),
        "lowStock" to listOf("low stock", "reorder level", "min stock", "alert", "threshold", "low_stock", "min_stock"),
        "boxes" to listOf("boxes", "box count", "cartons", "cases", "total boxes")
    )

    fun suggest(headers: List<String>): Map<String, ColumnSuggestion> {
        val result = mutableMapOf<String, ColumnSuggestion>()
        val usedIndices = mutableSetOf<Int>()

        // Pass 1: Exact normalized match (100% confidence)
        synonymMap.forEach { (field, synonyms) ->
            val primary = synonyms.first()
            headers.forEachIndexed { idx, header ->
                if (idx !in usedIndices) {
                    val norm = normalize(header)
                    if (norm == primary) {
                        result[field] = ColumnSuggestion(idx, 100)
                        usedIndices.add(idx)
                        return@forEach
                    }
                }
            }
        }

        // Pass 2: Exact match on any synonym (90% confidence)
        synonymMap.forEach { (field, synonyms) ->
            if (field !in result) {
                headers.forEachIndexed { idx, header ->
                    if (idx !in usedIndices) {
                        val norm = normalize(header)
                        if (synonyms.any { it == norm }) {
                            result[field] = ColumnSuggestion(idx, 90)
                            usedIndices.add(idx)
                            return@forEach
                        }
                    }
                }
            }
        }

        // Pass 3: Substring / contains match (80% confidence)
        synonymMap.forEach { (field, synonyms) ->
            if (field !in result) {
                headers.forEachIndexed { idx, header ->
                    if (idx !in usedIndices) {
                        val norm = normalize(header)
                        if (synonyms.any { syn -> norm.contains(syn) || syn.contains(norm) }) {
                            result[field] = ColumnSuggestion(idx, 80)
                            usedIndices.add(idx)
                            return@forEach
                        }
                    }
                }
            }
        }

        return result
    }

    fun hashHeaders(headers: List<String>): String {
        val normalized = headers.map { normalize(it) }.joinToString("|")
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(normalized.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }

    private fun normalize(raw: String): String {
        return raw.trim()
            .lowercase()
            .replace("_", " ")
            .replace("-", " ")
            .replace(".", "")
            .replace(Regex("\\s+"), " ")
    }
}
