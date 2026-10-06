package com.example.aastore

import org.json.JSONArray
import java.net.URL

data class AppEntry(
    val name: String,
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val description: String,
    val apkUrl: String,
    val sha256: String
)

object CatalogRepo {
    // Cambia esto por la URL de tu catalog.json (p. ej. GitHub raw)
    const val CATALOG_URL = "https://raw.githubusercontent.com/intrasportnutricion/appcar-catalog/main/catalog.json"

    fun fetch(url: String = CATALOG_URL): List<AppEntry> {
        val json = URL(url).openStream().bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            AppEntry(
                o.getString("name"), o.getString("packageName"),
                o.getLong("versionCode"), o.getString("versionName"),
                o.optString("description"), o.getString("apkUrl"), o.getString("sha256")
            )
        }
    }
}
