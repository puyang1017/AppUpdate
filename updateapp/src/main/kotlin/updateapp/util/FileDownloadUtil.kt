package util

import extension.log
import extension.exYes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_OK
import java.net.URL

/**
 * desc: 文件下载 当 FileDownloader 对某些apk下载失败时（比如：放在阿里云，码云上apk） 使用该工具类下载
 */
internal object FileDownloadUtil {

    /**
     * 下载文件
     * @param url 文件地址
     * @param fileSavePath 文件存储地址
     * @param fileName 文件存储名称
     * @param onStart 开始下载回调
     * @param onProgress 下载中回调
     * @param onComplete 下载完成回调
     * @param onError 下载失败回调
     */
    fun download(
        url: String,
        fileSavePath: String,
        fileName: String?,
        onStart: () -> Unit = {},
        onProgress: (current: Long, total: Long) -> Unit = { _, _ -> },
        onComplete: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            log("----使用HttpURLConnection下载----")
            onStart.invoke()
            var connection: HttpURLConnection? = null
            var outputStream: FileOutputStream? = null

            kotlin.runCatching {
                connection = URL(url).openConnection() as HttpURLConnection
                outputStream = FileOutputStream(File(fileSavePath, fileName))

                connection?.apply {
                    requestMethod = "GET"
                    setRequestProperty("Charset", "utf-8")
                    setRequestProperty("Accept-Encoding", "identity")
                    setRequestProperty(
                        "User-Agent",
                        " Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.120 Safari/537.36"
                    )
                    connect()
                }

                val responseCode = connection!!.responseCode
                if (responseCode == HTTP_OK) {
                    val total = connection!!.contentLength
                    var progress = -1
                    connection!!.inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyToWithProgress(output!!) {
                                val pro = (it * 100.0 / total).toInt()
                                (progress != pro).exYes {
                                    GlobalScope.launch(Dispatchers.Main) {
                                        onProgress(it, total.toLong())
                                    }
                                }
                                progress = pro
                            }
                        }
                    }
                }
            }.onSuccess {
                connection?.disconnect()
                outputStream?.close()
                log("HttpURLConnection下载完成")
                GlobalScope.launch(Dispatchers.Main) {
                    onComplete.invoke()
                }
            }.onFailure {
                connection?.disconnect()
                outputStream?.close()
                log("HttpURLConnection下载失败：${it.message}")
                GlobalScope.launch(Dispatchers.Main) {
                    onError.invoke(it)
                }
            }
        }
    }
}

fun InputStream.copyToWithProgress(
    out: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    currentByte: (Long) -> Unit = {}
): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        bytes = read(buffer)
        currentByte.invoke(bytesCopied)
    }
    return bytesCopied
}