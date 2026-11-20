package com.reachbell.types

internal interface SubscriberStore {
    val token: String?
    var externalId: String?
    fun save(token: String, externalId: String?)
    fun clear()
}
