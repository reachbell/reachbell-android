package com.reachbell.helpers

import com.reachbell.constants.Defaults
import com.reachbell.types.DeviceInfo
import org.json.JSONArray
import org.json.JSONObject

internal object RequestBodies {

    fun subscribe(
        token: String,
        externalId: String?,
        previousToken: String? = null,
        device: DeviceInfo = DeviceInfo(),
    ): JSONObject =
        JSONObject()
            .put("token", token)
            .put("tokenType", Defaults.TOKEN_TYPE)
            .put("platform", Defaults.PLATFORM)
            .put("os", Defaults.OS_NAME)
            .apply {
                if (!externalId.isNullOrBlank()) put("externalId", externalId)
                if (!previousToken.isNullOrBlank() && previousToken != token) put("previousToken", previousToken)
                if (!device.osVersion.isNullOrBlank()) put("osVersion", device.osVersion)
                if (!device.deviceType.isNullOrBlank()) put("device", device.deviceType)
            }

    fun identify(token: String, externalId: String): JSONObject =
        JSONObject()
            .put("token", token)
            .put("externalId", externalId)

    fun unidentify(token: String): JSONObject =
        JSONObject().put("token", token)

    fun tags(token: String, tags: List<String>): JSONObject =
        JSONObject()
            .put("token", token)
            .put("tags", JSONArray(tags))
}
