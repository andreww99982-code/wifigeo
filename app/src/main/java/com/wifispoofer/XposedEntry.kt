package com.wifispoofer

import com.wifispoofer.config.ConfigManager
import com.wifispoofer.hooks.WifiInfoHook
import com.wifispoofer.hooks.WifiManagerHook
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/** Точка входа Xposed-модуля (LSPosed). */
class XposedEntry : IXposedHookLoadPackage {

    companion object {
        const val TAG = "WifiSpoofer"
        const val PACKAGE = "com.wifispoofer"

        lateinit var prefs: XSharedPreferences
            private set

        fun log(msg: String) {
            XposedBridge.log("[$TAG] $msg")
        }
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == PACKAGE) return

        try {
            prefs = XSharedPreferences(PACKAGE, ConfigManager.PREFS_NAME)
            if (!prefs.file.canRead()) {
                log("⚠ Cannot read prefs file: ${prefs.file.absolutePath}")
                prefs.reload()
            }
        } catch (e: Exception) {
            log("❌ Failed to load preferences: ${e.message}")
            return
        }

        val scope = loadScope()
        if (scope.isNotEmpty() && lpparam.packageName !in scope) return

        log("🎯 Hooking package: ${lpparam.packageName}")

        try {
            WifiManagerHook.hook(lpparam)
            WifiInfoHook.hook(lpparam)
            log("✅ All hooks applied for ${lpparam.packageName}")
        } catch (e: Exception) {
            log("❌ Hook failed for ${lpparam.packageName}: ${e.message}")
        }
    }

    private fun loadScope(): Set<String> = try {
        prefs.reload()
        ConfigManager.loadScopeFromPrefs(prefs)
    } catch (e: Exception) {
        emptySet()
    }
}
