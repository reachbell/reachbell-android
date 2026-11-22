package com.reachbell.helpers

import android.content.Context
import android.content.SharedPreferences
import com.reachbell.constants.StorageKeys
import com.reachbell.types.SubscriberStore

internal class PrefsSubscriberStore(context: Context) : SubscriberStore {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(StorageKeys.PREFS_NAME, Context.MODE_PRIVATE)
    }

    override val token: String?
        get() = prefs.getString(StorageKeys.SUBSCRIBER_TOKEN, null)

    override var externalId: String?
        get() = prefs.getString(StorageKeys.EXTERNAL_ID, null)
        set(value) {
            prefs.edit().apply {
                if (value.isNullOrBlank()) remove(StorageKeys.EXTERNAL_ID) else putString(StorageKeys.EXTERNAL_ID, value)
            }.apply()
        }

    override val externalIdClearPending: Boolean
        get() = prefs.getBoolean(StorageKeys.EXTERNAL_ID_CLEAR_PENDING, false)

    override fun save(token: String, externalId: String?) {
        prefs.edit().apply {
            putString(StorageKeys.SUBSCRIBER_TOKEN, token)
            remove(StorageKeys.EXTERNAL_ID_CLEAR_PENDING)
            if (externalId.isNullOrBlank()) remove(StorageKeys.EXTERNAL_ID) else putString(StorageKeys.EXTERNAL_ID, externalId)
        }.apply()
    }

    override fun clear() {
        prefs.edit()
            .remove(StorageKeys.SUBSCRIBER_TOKEN)
            .remove(StorageKeys.EXTERNAL_ID)
            .putBoolean(StorageKeys.EXTERNAL_ID_CLEAR_PENDING, true)
            .apply()
    }
}
