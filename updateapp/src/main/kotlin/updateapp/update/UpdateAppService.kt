package update

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.content.Context.RECEIVER_EXPORTED

/**
 * desc: UpdateAppService
 */
internal class UpdateAppService : Service() {

    private val updateAppReceiver = UpdateAppReceiver()

    override fun onCreate() {
        super.onCreate()
        // 动态注册receiver 适配8.0 updateAppReceiver 静态注册没收不到广播

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(updateAppReceiver, IntentFilter(packageName + UpdateAppReceiver.ACTION_UPDATE), RECEIVER_EXPORTED)
            registerReceiver(updateAppReceiver, IntentFilter(packageName + UpdateAppReceiver.ACTION_RE_DOWNLOAD), RECEIVER_EXPORTED)
        }else {
            registerReceiver(updateAppReceiver, IntentFilter(packageName + UpdateAppReceiver.ACTION_UPDATE))
            registerReceiver(updateAppReceiver, IntentFilter(packageName + UpdateAppReceiver.ACTION_RE_DOWNLOAD))
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(updateAppReceiver) // 注销广播
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
