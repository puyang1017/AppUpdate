package update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.liulishuo.filedownloader.BaseDownloadTask
import com.liulishuo.filedownloader.FileDownloadLargeFileListener
import com.liulishuo.filedownloader.FileDownloader
import extension.log
import extension.exNo
import extension.exYes
import util.FileDownloadUtil
import util.GlobalContextProvider
import util.SPUtil
import util.SignMd5Util
import util.Utils
import java.io.File

internal object DownloadAppUtils {

    const val KEY_OF_SP_APK_PATH = "KEY_OF_SP_APK_PATH"

    /**
     * apk 下载后本地文件路径
     */
    var downloadUpdateApkFilePath: String = ""

    /**
     * 更新信息
     */
    private val updateInfo by lazy { UpdateAppUtils.updateInfo }

    /**
     * context
     */
    private val context by lazy { GlobalContextProvider.getGlobalContext() }

    /**
     * 是否在下载完成
     */
    var isDownloaded = false

    /**
     * 是否在下载中
     */
    var isDownloading = false

    /**
     *下载进度回调
     */
    var onProgress: (Int) -> Unit = {}

    /**
     * 下载出错回调
     */
    var onError: () -> Unit = {}

    /**
     * 出错，点击重试回调
     */
    var onReDownload: () -> Unit = {}

    /**
     * 通过浏览器下载APK包
     */
    fun downloadForWebView(url: String) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 出错后，点击重试
     */
    fun reDownload(closeThreeWayDownload:Boolean?) {
        onReDownload.invoke()
        download(closeThreeWayDownload)
    }

    /**
     * App下载APK包，下载完成后安装
     */
    fun download(closeThreeWayDownload:Boolean?) {
        (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED).exNo {
            log("没有SD卡")
            onError.invoke()
            return
        }

        var filePath = ""
        (updateInfo.config.apkSavePath.isNotEmpty()).exYes {
            filePath = updateInfo.config.apkSavePath
        }.exNo {
            val packageName = context.packageName
            filePath = context.externalCacheDir?.path + "/"
        }

        // apk 保存名称
        val apkName = if (updateInfo.config.apkSaveName.isNotEmpty()) {
            updateInfo.config.apkSaveName
        } else {
            Utils.getAppName(context)
        }

        val apkLocalPath = "$filePath/$apkName.apk"

        downloadUpdateApkFilePath = apkLocalPath

        SPUtil.putBase(KEY_OF_SP_APK_PATH, downloadUpdateApkFilePath)
        if (closeThreeWayDownload == true){//是否关闭三方下载库
            downloadByHttpUrlConnection(filePath, apkName)
        }else{
            FileDownloader.setup(context)

            val downloadTask = FileDownloader.getImpl().create(updateInfo.apkUrl)
                .setPath(apkLocalPath)

            downloadTask
                .addHeader("Accept-Encoding","identity")
                .addHeader("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.120 Safari/537.36")
                .setListener(object : FileDownloadLargeFileListener() {

                    override fun pending(task: BaseDownloadTask, soFarBytes: Long, totalBytes: Long) {
                        log("----使用FileDownloader下载-------")
                        log("pending:soFarBytes($soFarBytes),totalBytes($totalBytes)")
                        downloadStart()
                        if(totalBytes < 0){
                            downloadTask.pause()
                        }
                    }

                    override fun progress(task: BaseDownloadTask, soFarBytes: Long, totalBytes: Long) {
                        downloading(soFarBytes, totalBytes)
                        if(totalBytes < 0){
                            downloadTask.pause()
                        }
                    }

                    override fun paused(task: BaseDownloadTask, soFarBytes: Long, totalBytes: Long) {
                        log("获取文件总长度失败出错，尝试HTTPURLConnection下载")
                        Utils.deleteFile(downloadUpdateApkFilePath)
                        Utils.deleteFile("$downloadUpdateApkFilePath.temp")
                        downloadByHttpUrlConnection(filePath, apkName)
                    }

                    override fun completed(task: BaseDownloadTask) {
                        downloadComplete()
                    }

                    override fun error(task: BaseDownloadTask, e: Throwable) {
                        // FileDownloader 下载失败后，再调用 FileDownloadUtil 下载一次
                        // FileDownloader 对码云或者阿里云上的apk文件会下载失败
                        // downloadError(e)
                        log("下载出错，尝试HTTPURLConnection下载")
                        Utils.deleteFile(downloadUpdateApkFilePath)
                        Utils.deleteFile("$downloadUpdateApkFilePath.temp")
                        downloadByHttpUrlConnection(filePath, apkName)
                    }

                    override fun warn(task: BaseDownloadTask) {
                    }
                }).start()
        }
    }

    /**
     * 使用 HttpUrlConnection 下载
     */
    private fun downloadByHttpUrlConnection(filePath: String, apkName: String?) {
        FileDownloadUtil.download(
            updateInfo.apkUrl,
            filePath,
            "$apkName.apk",
            onStart = { downloadStart() },
            onProgress = { current, total -> downloading(current, total) },
            onComplete = { downloadComplete() },
            onError = { downloadError(it) }
        )
    }

    /**
     * 开始下载逻辑
     */
    private fun downloadStart() {
        isDownloading = true
        isDownloaded = false
        UpdateAppUtils.downloadListener?.onStart()
        UpdateAppReceiver.send(context, 0)
    }

    /**
     * 下载中逻辑
     */
    private fun downloading(soFarBytes: Long, totalBytes: Long) {
//        log("soFarBytes:$soFarBytes--totalBytes:$totalBytes")
        isDownloading = true
        isDownloaded = false
        var progress = (soFarBytes * 100.0 / totalBytes).toInt()
        if (progress < 0) progress = 0
        log("progress:$progress")
        UpdateAppReceiver.send(context, progress)
        this@DownloadAppUtils.onProgress.invoke(progress)
        UpdateAppUtils.downloadListener?.onDownload(progress)
    }

    /**
     * 下载完成处理逻辑
     */
    private fun downloadComplete() {
        isDownloaded = true
        isDownloading = false
        log("completed")
        this@DownloadAppUtils.onProgress.invoke(100)
        UpdateAppUtils.downloadListener?.onFinish()
        // 校验md5
        (updateInfo.config.needCheckMd5).exYes {
            checkMd5(context)
        }.exNo {
            UpdateAppReceiver.send(context, 100)
        }
    }

    /**
     * 下载失败处理逻辑
     */
    private fun downloadError(e: Throwable) {
        isDownloading = false
        isDownloaded = false
        log("error:${e.message}")
        Utils.deleteFile(downloadUpdateApkFilePath)
        this@DownloadAppUtils.onError.invoke()
        UpdateAppUtils.downloadListener?.onError(e)
        UpdateAppReceiver.send(context, -1000)
    }

    /**
     * 校验Md5
     *  先获取本应用的MD5值，获取未安装应用的MD5.进行对比
     */
    private fun checkMd5(context: Context) {
        // 当前应用md5
        val localMd5 = SignMd5Util.getAppSignatureMD5()

        // 下载的apk 签名md5
        val apkMd5 = SignMd5Util.getSignMD5FromApk(File(downloadUpdateApkFilePath))
        log("当前应用签名md5：$localMd5")
        log("下载apk签名md5：$apkMd5")

        // 校验结果回调
        UpdateAppUtils.md5CheckResultListener?.onResult(localMd5.equals(apkMd5, true))

        (localMd5.equals(apkMd5, true)).exYes {
            log("md5校验成功")
            UpdateAppReceiver.send(context, 100)
        }.exNo {
            log("md5校验失败")
        }
    }
}