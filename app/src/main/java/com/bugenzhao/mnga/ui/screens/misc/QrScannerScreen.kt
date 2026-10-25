package com.bugenzhao.mnga.ui.screens.misc

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.webkit.CookieManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.bugenzhao.mnga.App
import com.bugenzhao.mnga.ui.nav.Navigator
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

private const val TAG = "QrScanner"

/**
 * Scans QR codes. If the QR is an NGA web-login code
 * (nuke.php?__lib=login&__act=qrlogin_ui&qrkey=...), confirms the login
 * with the current app session so the web page logs in.
 */
@Composable
fun QrScannerScreen(navigator: Navigator) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var result by remember { mutableStateOf<String?>(null) }
    var confirming by remember { mutableStateOf(false) }
    // Set when the last confirm failed on auth; offers re-login + auto retry.
    var authFailed by remember { mutableStateOf(false) }
    var pendingQrkey by remember { mutableStateOf<String?>(null) }

    fun doConfirm(qrkey: String) {
        authFailed = false
        confirming = true
        result = "检测到 NGA 登录二维码，正在确认…"
        confirmNgaLogin(
            qrkey,
            onSuccess = {
                confirming = false
                authFailed = false
                pendingQrkey = null
                result = "登录确认成功！网页端应该已经登录。"
            },
            onError = { e ->
                confirming = false
                val msg = e.message ?: "未知错误"
                authFailed = msg.contains("登录态") || msg.contains("重新登录") || msg.contains("还没登录")
                if (authFailed) pendingQrkey = qrkey
                result = "确认失败：$msg"
            }
        )
    }

    // After the login sheet closes, retry a pending confirm with fresh auth.
    val isSigning by App.authStorage.isSigning.collectAsState()
    var wasSigning by remember { mutableStateOf(false) }
    LaunchedEffect(isSigning) {
        if (wasSigning && !isSigning) {
            val qk = pendingQrkey
            if (qk != null) {
                pendingQrkey = null
                Log.d(TAG, "Login sheet closed, retrying confirm")
                doConfirm(qk)
            }
        }
        wasSigning = isSigning
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "扫码登录",
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                !hasPermission -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(64.dp))
                        Text("需要相机权限才能扫码")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }) {
                            Text("授权相机")
                        }
                    }
                }
                result != null -> {
                    // Show result / confirmation status
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(64.dp))
                        if (confirming) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("正在确认登录…")
                        } else {
                            Text(result!!, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(16.dp))
                            if (authFailed) {
                                Button(onClick = {
                                    // Opens the app login sheet; confirm auto-retries when it closes.
                                    App.authStorage.setIsSigning(true)
                                }) {
                                    Text("重新登录并重试")
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            Button(onClick = {
                                result = null
                                confirming = false
                                authFailed = false
                                pendingQrkey = null
                            }) {
                                Text("继续扫码")
                            }
                        }
                    }
                }
                else -> {
                    CameraPreview(
                        onQrDetected = { content ->
                            Log.d(TAG, "QR detected: $content")
                            val qrkey = parseNgaQrKey(content)
                            if (qrkey != null) {
                                doConfirm(qrkey)
                            } else {
                                result = "二维码内容：$content\n（不是 NGA 登录二维码）"
                            }
                        }
                    )
                    // Hint overlay
                    Text(
                        "将 NGA 网页端的登录二维码放入框内",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp),
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/** Extracts the qrkey from an NGA login QR URL, or null if not a login QR. */
private fun parseNgaQrKey(content: String): String? {
    if (!content.contains("qrlogin_ui")) return null
    // Format: https://bbs.nga.cn/nuke.php?__lib=login&__act=qrlogin_ui&qrkey=xxx
    val regex = "[?&]qrkey=([^&]+)".toRegex()
    return regex.find(content)?.groupValues?.get(1)
}

/**
 * Confirms the NGA web login.
 *
 * The scanned QR points at `qrlogin_ui`, which is only the confirmation
 * *page*. The real confirmation is the page's `loginform`:
 * POST /nuke.php?__lib=login&__act=qrlogin_allow&raw=3 with the `qrkey`
 * field, sent with the app's auth cookies so the server knows which
 * account is approving. The web page polling
 * (__act=login&v2=1&qrkey&hiddenkey) then succeeds and logs in.
 *
 * Runs off the main thread.
 */
private fun confirmNgaLogin(
    qrkey: String,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit,
) {
    GlobalScope.launch(Dispatchers.IO) {
        try {
            // Auth cookies come from the app's own login state (source of truth),
            // not the WebView cookie jar (which may be stale). AuthInfo.token
            // is the ngaPassportCid captured at login.
            val auth = App.authStorage.authInfo.value
            if (auth.uid.isBlank() || auth.token.isBlank()) {
                withContext(Dispatchers.Main) { onError(Exception("App 内还没登录，无法确认")) }
                return@launch
            }
            val webCookies = CookieManager.getInstance().getCookie("https://bbs.nga.cn") ?: ""
            val cookies = mergeCookies(
                webCookies,
                "ngaPassportUid=${auth.uid}; ngaPassportCid=${auth.token}",
            )
            Log.d(TAG, "Confirming as uid=${auth.uid}")

            // 1. Load the confirm page to extract the loginform (action + hidden fields).
            val uiUrl = "https://bbs.nga.cn/nuke.php?__lib=login&__act=qrlogin_ui&qrkey=$qrkey"
            val html = httpGet(uiUrl, cookies)
            Log.d(TAG, "qrlogin_ui loaded, ${html.length} chars")
            if (html.contains("你没有在这台设备上登录")) {
                withContext(Dispatchers.Main) { onError(Exception("登录态已失效，请在 App 内重新登录")) }
                return@launch
            }

            val action = Regex("""<form[^>]*\bid\s*=\s*['"]loginform['"][^>]*\baction\s*=\s*['"]([^'"]+)""")
                .find(html)?.groupValues?.get(1)
                ?: "/nuke.php?__lib=login&__act=qrlogin_allow&raw=3"
            val fields = Regex("""<input[^>]+>""").findAll(html).mapNotNull { m ->
                val tag = m.value
                val name = Regex("""\bname\s*=\s*['"]([^'"]+)""")
                    .find(tag)?.groupValues?.get(1) ?: return@mapNotNull null
                val value = Regex("""\bvalue\s*=\s*['"]([^'"]*)""")
                    .find(tag)?.groupValues?.get(1) ?: ""
                name to value
            }.toMap().toMutableMap()
            fields["qrkey"] = qrkey
            val postUrl = if (action.startsWith("http")) action else "https://bbs.nga.cn$action"
            Log.d(TAG, "POST $postUrl fields=${fields.keys}")

            // 2. Submit the confirmation, like the page's loginSubmit() does.
            val resp = httpPost(postUrl, cookies, fields)
            Log.d(TAG, "qrlogin_allow response: ${resp.take(600)}")

            // 3. NGA answers via script_muti_get_var_store={data:[...]} or {error:[...]}.
            val err = Regex("""error['"]?\s*:\s*\[\s*['"]([^'"]+)""")
                .find(resp)?.groupValues?.get(1)
            withContext(Dispatchers.Main) {
                if (err != null) onError(Exception(err)) else onSuccess()
            }
        } catch (e: Exception) {
            Log.e(TAG, "confirm failed", e)
            withContext(Dispatchers.Main) { onError(e) }
        }
    }
}

/** Merges Cookie headers; later headers override earlier ones by name. */
private fun mergeCookies(vararg headers: String): String {
    val map = LinkedHashMap<String, String>()
    headers.forEach { h ->
        h.split(";").forEach { part ->
            val kv = part.trim().split("=", limit = 2)
            if (kv.size == 2 && kv[0].isNotBlank()) map[kv[0]] = kv[1]
        }
    }
    return map.entries.joinToString("; ") { "${it.key}=${it.value}" }
}

private const val UA =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

/** GET with cookies; decodes GBK like NGA pages do. */
private fun httpGet(url: String, cookies: String): String {
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        setRequestProperty("Cookie", cookies)
        setRequestProperty("User-Agent", UA)
        connectTimeout = 15000
        readTimeout = 15000
    }
    return try {
        val code = conn.responseCode
        Log.d(TAG, "GET $url -> $code")
        val charset = conn.contentType?.let { ct ->
            Regex("""charset=([\w-]+)""").find(ct)?.groupValues?.get(1)
        } ?: "GBK"
        val stream = if (code in 200..399) conn.inputStream else conn.errorStream
        stream.bufferedReader(java.nio.charset.Charset.forName(charset)).readText()
    } finally {
        conn.disconnect()
    }
}

/** POST form fields with cookies; decodes GBK like NGA pages do. */
private fun httpPost(url: String, cookies: String, fields: Map<String, String>): String {
    val body = fields.entries.joinToString("&") { (k, v) ->
        "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
    }
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Cookie", cookies)
        setRequestProperty("User-Agent", UA)
        setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        setRequestProperty("Referer", "https://bbs.nga.cn/nuke.php?__lib=login&__act=qrlogin_ui")
        connectTimeout = 15000
        readTimeout = 15000
    }
    return try {
        conn.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
        val code = conn.responseCode
        Log.d(TAG, "POST $url -> $code")
        val charset = conn.contentType?.let { ct ->
            Regex("""charset=([\w-]+)""").find(ct)?.groupValues?.get(1)
        } ?: "GBK"
        val stream = if (code in 200..399) conn.inputStream else conn.errorStream
        stream.bufferedReader(java.nio.charset.Charset.forName(charset)).readText()
    } finally {
        conn.disconnect()
    }
}

@Composable
private fun CameraPreview(onQrDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    var detected by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            scanner.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { ia ->
                        ia.setAnalyzer(executor) { imageProxy ->
                            if (detected) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        val value = barcodes.firstOrNull()?.rawValue
                                        if (value != null && !detected) {
                                            detected = true
                                            onQrDetected(value)
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "bind failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}
