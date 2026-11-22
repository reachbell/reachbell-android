package com.reachbell.types

internal interface SubscriberStore {
    val token: String?
    var externalId: String?

    /**
     * Set by [clear] and cleared by [save]: the next registration without an explicit externalId sends
     * `"externalId": null` so a logout whose unidentify call failed is repaired server-side.
     */
    val externalIdClearPending: Boolean

    /** Saves a successful registration and drops [externalIdClearPending]. */
    fun save(token: String, externalId: String?)

    /** Forgets the subscription and sets [externalIdClearPending]. */
    fun clear()
}
