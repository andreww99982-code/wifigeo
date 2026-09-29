package com.wifispoofer.config

import android.content.Context
import android.content.SharedPreferences
import com.wifispoofer.model.FakeNetwork
import org.json.JSONArray

/**
 * Управляет конфигурацией модуля.
 * Prefs хранятся в MODE_PRIVATE; хуки читают их через XSharedPreferences
 * (LSPosed, meta-data xposedsharedprefs=true).
 */
object ConfigManager {

    const val PREFS_NAME = "wifispoofer_prefs"
    private const val KEY_NETWORKS = "cached_networks"
    private const val KEY_SCOPE = "target_scope"
    private const val KEY_SOURCE = "location_source"
    private const val KEY_MANUAL_LAT = "manual_lat"
    private const val KEY_MANUAL_LON = "manual_lon"
    private const val KEY_CONNECTED_INDEX = "connected_index"

    fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ===== Wi-Fi Networks =====

    fun saveNetworks(context: Context, networks: List<FakeNetwork>) {
        getPrefs(context).edit()
            .putString(KEY_NETWORKS, FakeNetwork.listToJson(networks))
            .apply()
    }

    fun loadNetworks(context: Context): List<FakeNetwork> {
        val json = getPrefs(context).getString(KEY_NETWORKS, "") ?: ""
        return FakeNetwork.listFromJson(json)
    }

    fun clearNetworks(context: Context) {
        getPrefs(context).edit().remove(KEY_NETWORKS).apply()
    }

    // ===== Scope =====

    fun saveScope(context: Context, packages: Set<String>) {
        val arr = JSONArray(packages.toList())
        getPrefs(context).edit().putString(KEY_SCOPE, arr.toString()).apply()
    }

    fun loadScope(context: Context): Set<String> =
        loadScopeFromPrefs(getPrefs(context))

    // ===== Location Source =====

    fun saveLocationSource(context: Context, source: String) {
        getPrefs(context).edit().putString(KEY_SOURCE, source).apply()
    }

    fun getLocationSource(context: Context): String =
        getPrefs(context).getString(KEY_SOURCE, "ip") ?: "ip"

    fun saveManualCoords(context: Context, lat: Double, lon: Double) {
        getPrefs(context).edit()
            .putString(KEY_MANUAL_LAT, lat.toString())
            .putString(KEY_MANUAL_LON, lon.toString())
            .apply()
    }

    fun getManualCoords(context: Context): Pair<Double, Double>? {
        val prefs = getPrefs(context)
        val lat = prefs.getString(KEY_MANUAL_LAT, null)?.toDoubleOrNull()
        val lon = prefs.getString(KEY_MANUAL_LON, null)?.toDoubleOrNull()
        return if (lat != null && lon != null) lat to lon else null
    }

    // ===== Connected Network Index =====

    fun saveConnectedIndex(context: Context, index: Int) {
        getPrefs(context).edit().putInt(KEY_CONNECTED_INDEX, index).apply()
    }

    fun getConnectedIndex(context: Context): Int =
        getPrefs(context).getInt(KEY_CONNECTED_INDEX, 0)

    // ===== Чтение из Xposed (XSharedPreferences) =====

    fun loadNetworksFromPrefs(prefs: SharedPreferences): List<FakeNetwork> {
        val json = prefs.getString(KEY_NETWORKS, "") ?: ""
        return FakeNetwork.listFromJson(json)
    }

    fun loadScopeFromPrefs(prefs: SharedPreferences): Set<String> {
        val json = prefs.getString(KEY_SCOPE, "[]") ?: "[]"
        val arr = JSONArray(json)
        return (0 until arr.length()).map { arr.getString(it) }.toSet()
    }

    fun getConnectedIndexFromPrefs(prefs: SharedPreferences): Int =
        prefs.getInt(KEY_CONNECTED_INDEX, 0)
}
