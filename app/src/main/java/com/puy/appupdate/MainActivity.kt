package com.puy.appupdate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import constacne.DownLoadBy
import constacne.UiType
import listener.OnBtnClickListener
import model.UiConfig
import model.UpdateConfig
import update.UpdateAppUtils
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.button).setOnClickListener {
            update()
        }

    }

    fun update(){
        UpdateAppUtils.getInstance().deleteInstalledApk()
        UpdateAppUtils
            .getInstance()
            .apkUrl("https://appdownload.qiyou.cn/upload/20220516222139/app_version/qyOtherChannelApp_aligned_0516_1840_signed_QEEYOU.apk")
            .updatePackageSize("55MB")
            .updateTitle("2222")
            .updateContent("23sdfsdjkvjks\ndvjksvksnlkvn")
            .updateConfig(UpdateConfig().apply {
                downloadBy = DownLoadBy.APP
                isShowNotification = true
                alwaysShowDownLoadDialog = true
                serverVersionName = "2.0.0"
                apkSavePath = externalCacheDir?.absolutePath!!
            })
            .uiConfig(UiConfig(uiType = UiType.PLENTIFUL, closeFileDownload = false))

            // 设置 取消 按钮点击事件
            .setCancelBtnClickListener(object : OnBtnClickListener {
                override fun onClick(): Boolean {
                    Toast.makeText(this@MainActivity, "cancel btn click", Toast.LENGTH_SHORT).show()
                    return false // 事件是否消费，是否需要传递下去。false-会执行原有点击逻辑，true-只执行本次设置的点击逻辑
                }
            })

            // 设置 立即更新 按钮点击事件
            .setUpdateBtnClickListener(object : OnBtnClickListener {
                override fun onClick(): Boolean {
                    Toast.makeText(this@MainActivity, "update btn click", Toast.LENGTH_SHORT).show()
                    return false // 事件是否消费，是否需要传递下去。false-会执行原有点击逻辑，true-只执行本次设置的点击逻辑
                }
            })
            .update()
    }
}
