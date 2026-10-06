package com.example.aastore

import org.json.JSONArray
import java.net.URL

data class AppEntry(
    val name: String,
    val packageName: String = "",
    val versionCode: Long = 0,
    val versionName: String = "",
    val description: String = "",
    val apkUrl: String = "",
    val sha256: String = "",
    val openUrl: String = ""
)

object CatalogRepo {
    const val CATALOG_URL = "https://raw.githubusercontent.com/intrasportnutricion/appcar-catalog/main/catalog.json"

    fun fetch(url: String = CATALOG_URL): List<AppEntry> {
        val json = URL(url).openStream().bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            AppEntry(
                name = o.getString("name"),
                packageName = o.optString("packageName"),
                versionCode = o.optLong("versionCode", 0),
                versionName = o.optString("versionName"),
                description = o.optString("description"),
                apkUrl = o.optString("apkUrl"),
                sha256 = o.optString("sha256"),
                openUrl = o.optString("openUrl")
            )
        }
    }
}
