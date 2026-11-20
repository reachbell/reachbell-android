package com.reachbell.helpers

import com.reachbell.types.SubscriberStore

internal class FakeSubscriberStore(
    override var token: String? = null,
    override var externalId: String? = null,
) : SubscriberStore {

    override fun save(token: String, externalId: String?) {
        this.token = token
        this.externalId = externalId
    }

    override fun clear() {
        token = null
        externalId = null
    }
}
