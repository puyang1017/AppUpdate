package update

import extension.log
import extension.exNo
import extension.exYes
import listener.OnBtnClickListener
import listener.Md5CheckResultListener
import listener.OnInitUiListener
import listener.UpdateDownloadListener
import model.UiConfig
import model.UpdateConfig
import model.UpdateInfo
import ui.UpdateAppActivity
import util.GlobalContextProvider
import util.SPUtil
import util.Utils
import android.widget.Toast
import com.puy.updateapp.R

object UpdateAppUtils {

    init {
        GlobalContextProvider.getGlobalContext()
    }

    // 更新信息对象
    internal val updateInfo = UpdateInfo()

    // 下载监听
    internal var downloadListener: UpdateDownloadListener? = null

    // md5校验结果回调
    internal var md5CheckResultListener: Md5CheckResultListener? = null

    // 初始化更新弹窗UI回调
    internal var onInitUiListener: OnInitUiListener? = null

    // "暂不更新"按钮点击事件
    internal var onCancelBtnClickListener: OnBtnClickListener? = null

    // "立即更新"按钮点击事件
    internal var onUpdateBtnClickListener: OnBtnClickListener? = null

    /**
     * 设置apk下载地址
     */
    fun apkUrl(apkUrl: String): UpdateAppUtils {
        updateInfo.apkUrl = apkUrl
        return this
    }

    /**
     * 设置包大小
     */
    fun updatePackageSize(size: String): UpdateAppUtils {
        updateInfo.packageSize = size
        return this
    }

    /**
     * 设置更新标题
     */
    fun updateTitle(title: CharSequence): UpdateAppUtils {
        updateInfo.updateTitle = title
        return this
    }

    /**
     * 设置更新按钮
     */
    fun updateButton(title: CharSequence): UpdateAppUtils {
        updateInfo.updateButton = title
        return this
    }

    /**
     * 设置更新内容
     */
    fun updateContent(content: CharSequence): UpdateAppUtils {
        updateInfo.updateContent = content
        return this
    }

    /**
     * 设置更新配置
     */
    fun updateConfig(config: UpdateConfig): UpdateAppUtils {
        updateInfo.config = config
        return this
    }

    /**
     * 设置UI配置
     */
    fun uiConfig(uiConfig: UiConfig): UpdateAppUtils {
        updateInfo.uiConfig = uiConfig
        return this
    }

    /**
     * 设置下载监听
     */
    fun setUpdateDownloadListener(listener: UpdateDownloadListener?): UpdateAppUtils {
        this.downloadListener = listener
        return this
    }

    /**
     * 设置md5校验结果监听
     */
    fun setMd5CheckResultListener(listener: Md5CheckResultListener?): UpdateAppUtils {
        this.md5CheckResultListener = listener
        return this
    }

    /**
     * 设置初始化UI监听
     */
    fun setOnInitUiListener(listener: OnInitUiListener?): UpdateAppUtils {
        this.onInitUiListener = listener
        return this
    }

    /**
     * 设置 “暂不更新” 按钮点击事件
     */
    fun setCancelBtnClickListener(listener: OnBtnClickListener?): UpdateAppUtils {
        this.onCancelBtnClickListener = listener
        return this
    }

    /**
     * 设置 “立即更新” 按钮点击事件
     */
    fun setUpdateBtnClickListener(listener: OnBtnClickListener?): UpdateAppUtils {
        this.onUpdateBtnClickListener = listener
        return this
    }

    /**
     * 检查更新
     */
    fun update() {
        if (DownloadAppUtils.isDownloading) {
            Toast.makeText( GlobalContextProvider.getGlobalContext(), GlobalContextProvider.getGlobalContext().getString(R.string.toast_download_apk), Toast.LENGTH_SHORT).show()
            return
        }
        val keyName = GlobalContextProvider.getGlobalContext().packageName + updateInfo.config.serverVersionName
        // 设置每次显示，设置本次显示及强制更新 每次都显示弹窗
        (updateInfo.config.alwaysShow || updateInfo.config.thisTimeShow || updateInfo.config.force).exYes {
            UpdateAppActivity.launch()
        }.exNo {
            val hasShow = SPUtil.getBoolean(keyName, false)
            (hasShow).exNo { UpdateAppActivity.launch() }
        }
        SPUtil.putBase(keyName, true)
    }

    /**
     * 删除已安装 apk
     */
    fun deleteInstalledApk() {
        if (DownloadAppUtils.isDownloading) {
            return
        }
        val apkPath = SPUtil.getString(DownloadAppUtils.KEY_OF_SP_APK_PATH, "")
        log("deleteInstalledApk:$apkPath")
        val appVersionCode = Utils.getAPPVersionCode()
        val apkVersionCode = apkPath?.let { Utils.getApkVersionCode(it) }
        log("appVersionCode:$appVersionCode")
        log("apkVersionCode:$apkVersionCode")
        (apkPath.isNullOrEmpty()).exNo {
            Utils.deleteFile(apkPath)
        }
    }

    /**
     * 获取单例对象
     */
    @JvmStatic
    fun getInstance() = this
}