package com.alieducational.app.domain.model

data class BulkResult(
    val done: Int,
    val skipped: List<Pair<String, String>>
)

enum class RemovalAction {
    ARCHIVED,
    DELETED
}

data class RemovalResult(
    val id: String,
    val name: String,
    val action: RemovalAction,
    val message: String
)
