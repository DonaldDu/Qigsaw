package com.iqiyi.android.qigsaw.core

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.*
import android.content.pm.ProviderInfo
import android.net.Uri

class FeatureProviderSwitch(private val context: Context) {
    private val providers = context.featureProviders()

    /**
     * called in Application.getResources
     * */
    fun startFeatureProviders() {
        if (providers.isEmpty()) return
        val iterator = providers.iterator()
        while (iterator.hasNext()) {
            val provider = iterator.next()
            if (context.startFeatureProvider(provider)) iterator.remove()
        }
    }

    private fun Context.startFeatureProvider(provider: ProviderInfo): Boolean {
        val componentName = try {
            val p = Class.forName(provider.name)
            ComponentName(this, p.name)
        } catch (e: Exception) {
            null
        }
        return if (componentName != null) {
            packageManager.setComponentEnabledSetting(componentName, COMPONENT_ENABLED_STATE_ENABLED, DONT_KILL_APP)
            val uri = Uri.parse("content://${provider.authority}")
            try {
                contentResolver.insert(uri, null)
            } catch (e: Exception) {
            }
            packageManager.setComponentEnabledSetting(componentName, COMPONENT_ENABLED_STATE_DISABLED, DONT_KILL_APP)
            println("startFeatureProvider: ${provider.name}, ok")
            true
        } else {
            println("startFeatureProvider: ${provider.name}, uninstalled")
            false
        }
    }

    private fun Context.enableProvider(providerClassName: String, enable: Boolean) {
        val componentName = ComponentName(this, providerClassName)
        val newState = if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        packageManager.setComponentEnabledSetting(componentName, newState, DONT_KILL_APP)
    }

    /**
     * after preloadInstalledSplits
     * */
    private fun Context.featureProviders(): MutableList<ProviderInfo> {
        @Suppress("DEPRECATION")
        val DISABLED_COMPONENTS = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            MATCH_DISABLED_COMPONENTS
        } else GET_DISABLED_COMPONENTS
        val info = packageManager.getPackageInfo(applicationInfo.packageName, GET_PROVIDERS or DISABLED_COMPONENTS)
        val ps = info.providers?.filter {
            !it.enabled && it.authority != null
        } ?: emptyList()
        return ps.toMutableList()
    }
}




