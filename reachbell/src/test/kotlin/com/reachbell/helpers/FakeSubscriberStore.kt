package com.reachbell.helpers

import com.reachbell.types.SubscriberStore

internal class FakeSubscriberStore(
    override var token: String? = null,
    override var externalId: String? = null,
) : SubscriberStore {

    override var externalIdClearPending: Boolean = false

    override fun save(token: String, externalId: String?) {
        this.token = token
        this.externalId = externalId
        externalIdClearPending = false
    }

    override fun clear() {
        token = null
        externalId = null
        externalIdClearPending = true
    }
}
