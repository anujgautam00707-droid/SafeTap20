package com.safetap.app.data.sos.services

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.safetap.app.domain.sos.services.BatteryProvider

class DefaultBatteryProvider(
    private val context: Context
) : BatteryProvider {

    override fun getBatteryPercentage(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val capacity = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                if (capacity in 0..100) return capacity
            }

            val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, iFilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

            if (level >= 0 && scale > 0) {
                ((level / scale.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else {
                100
            }
        } catch (_: Exception) {
            100
        }
    }
}
