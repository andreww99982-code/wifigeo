package com.wifispoofer.hooks

import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import com.wifispoofer.XposedEntry
import com.wifispoofer.config.ConfigManager
import com.wifispoofer.model.FakeNetwork
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Хуки WifiManager: getScanResults, getWifiState, isWifiEnabled,
 * startScan, getConnectionInfo.
 */
object WifiManagerHook {

    private const val WM = "android.net.wifi.WifiManager"

    fun hook(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookGetScanResults(lpparam)
        hookGetWifiState(lpparam)
        hookIsWifiEnabled(lpparam)
        hookStartScan(lpparam)
        hookGetConnectionInfo(lpparam)
    }

    private fun hookGetScanResults(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(WM, lpparam.classLoader, "getScanResults",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val networks = loadCachedNetworks()
                            if (networks.isEmpty()) {
                                XposedEntry.log("getScanResults: no cached networks, passing through")
                                return
                            }
                            val fake = buildScanResults(networks)
                            param.result = fake
                            XposedEntry.log("getScanResults: injected ${fake.size} fake networks")
                        } catch (e: Exception) {
                            XposedEntry.log("getScanResults hook error: ${e.message}")
                        }
                    }
                })
            XposedEntry.log("✓ Hooked getScanResults()")
        } catch (e: Exception) {
            XposedEntry.log("✗ Failed to hook getScanResults: ${e.message}")
        }
    }

    private fun hookGetWifiState(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(WM, lpparam.classLoader, "getWifiState",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (loadCachedNetworks().isNotEmpty()) {
                            param.result = WifiManager.WIFI_STATE_ENABLED
                        }
                    }
                })
            XposedEntry.log("✓ Hooked getWifiState()")
        } catch (e: Exception) {
            XposedEntry.log("✗ Failed to hook getWifiState: ${e.message}")
        }
    }

    private fun hookIsWifiEnabled(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(WM, lpparam.classLoader, "isWifiEnabled",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (loadCachedNetworks().isNotEmpty()) {
                            param.result = true
                        }
                    }
                })
            XposedEntry.log("✓ Hooked isWifiEnabled()")
        } catch (e: Exception) {
            XposedEntry.log("✗ Failed to hook isWifiEnabled: ${e.message}")
        }
    }

    private fun hookStartScan(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(WM, lpparam.classLoader, "startScan",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (loadCachedNetworks().isNotEmpty()) {
                            param.result = true
                        }
                    }
                })
            XposedEntry.log("✓ Hooked startScan()")
        } catch (e: Exception) {
            XposedEntry.log("⚠ startScan hook skipped: ${e.message}")
        }
    }

    private fun hookGetConnectionInfo(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(WM, lpparam.classLoader, "getConnectionInfo",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val networks = loadCachedNetworks()
                            if (networks.isEmpty()) return

                            val connected = networks[getConnectedIndex().coerceIn(0, networks.size - 1)]
                            val wifiInfo = param.result ?: return

                            XposedHelpers.setObjectField(wifiInfo, "mBSSID", connected.bssid)
                            try {
                                XposedHelpers.setObjectField(wifiInfo, "mWifiSsid",
                                    createWifiSsid(connected.ssid, lpparam.classLoader))
                            } catch (_: Throwable) {}
                            try { XposedHelpers.setIntField(wifiInfo, "mFrequency", connected.frequency) } catch (_: Throwable) {}
                            try { XposedHelpers.setIntField(wifiInfo, "mRssi", connected.level) } catch (_: Throwable) {}
                            try {
                                val stateClass = XposedHelpers.findClass(
                                    "android.net.wifi.SupplicantState", lpparam.classLoader)
                                val completed = XposedHelpers.getStaticObjectField(stateClass, "COMPLETED")
                                XposedHelpers.setObjectField(wifiInfo, "mSupplicantState", completed)
                            } catch (_: Throwable) {}

                            param.result = wifiInfo
                        } catch (e: Exception) {
                            XposedEntry.log("getConnectionInfo hook error: ${e.message}")
                        }
                    }
                })
            XposedEntry.log("✓ Hooked getConnectionInfo()")
        } catch (e: Exception) {
            XposedEntry.log("✗ Failed to hook getConnectionInfo: ${e.message}")
        }
    }

    // ===== Builders =====

    private fun buildScanResults(networks: List<FakeNetwork>): List<ScanResult> {
        return networks.mapNotNull { network ->
            try {
                val scanResult = ScanResult::class.java.getDeclaredConstructor().apply {
                    isAccessible = true
                }.newInstance()

                XposedHelpers.setObjectField(scanResult, "SSID", network.ssid)
                XposedHelpers.setObjectField(scanResult, "BSSID", network.bssid)
                XposedHelpers.setIntField(scanResult, "frequency", network.frequency)
                XposedHelpers.setIntField(scanResult, "level", network.level)
                XposedHelpers.setObjectField(scanResult, "capabilities", network.capabilities)
                XposedHelpers.setLongField(scanResult, "timestamp", System.currentTimeMillis() * 1000)
                try {
                    XposedHelpers.setIntField(scanResult, "channelWidth", network.channelWidth)
                } catch (_: Throwable) {}

                scanResult
            } catch (e: Throwable) {
                XposedEntry.log("Failed to create ScanResult for ${network.ssid}: ${e.message}")
                null
            }
        }
    }

    private fun createWifiSsid(ssid: String, classLoader: ClassLoader): Any? {
        return try {
            val cls = XposedHelpers.findClass("android.net.wifi.WifiSsid", classLoader)
            try {
                XposedHelpers.callStaticMethod(cls, "fromUtf8Text", ssid as CharSequence)
            } catch (_: Throwable) {
                XposedHelpers.callStaticMethod(cls, "fromBytes", ssid.toByteArray(Charsets.UTF_8))
            }
        } catch (e: Throwable) {
            XposedEntry.log("Failed to create WifiSsid: ${e.message}")
            null
        }
    }

    // ===== Data Loading =====

    private fun loadCachedNetworks(): List<FakeNetwork> = try {
        XposedEntry.prefs.reload()
        ConfigManager.loadNetworksFromPrefs(XposedEntry.prefs)
    } catch (e: Exception) {
        XposedEntry.log("Failed to load networks: ${e.message}")
        emptyList()
    }

    private fun getConnectedIndex(): Int = try {
        XposedEntry.prefs.reload()
        ConfigManager.getConnectedIndexFromPrefs(XposedEntry.prefs)
    } catch (_: Exception) { 0 }
}
