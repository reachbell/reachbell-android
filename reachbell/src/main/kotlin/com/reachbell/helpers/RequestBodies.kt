package com.reachbell.helpers

import com.reachbell.constants.Defaults
import org.json.JSONArray
import org.json.JSONObject

internal object RequestBodies {

    fun subscribe(token: String, externalId: String?): JSONObject =
        JSONObject()
            .put("token", token)
            .put("tokenType", Defaults.TOKEN_TYPE)
            .put("platform", Defaults.PLATFORM)
            .apply { if (!externalId.isNullOrBlank()) put("externalId", externalId) }

    fun identify(token: String, externalId: String): JSONObject =
        JSONObject()
            .put("token", token)
            .put("externalId", externalId)

    fun tags(token: String, tags: List<String>): JSONObject =
        JSONObject()
            .put("token", token)
            .put("tags", JSONArray(tags))
}
