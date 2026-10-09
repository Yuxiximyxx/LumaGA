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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.bugenzhao.mnga.ui.nav.Navigator
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
@OptIn(ExperimentalMaterial3Api::class)
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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("扫码登录") },
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
                            Button(onClick = {
                                result = null
                                confirming = false
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
                                confirming = true
                                result = "检测到 NGA 登录二维码，正在确认…"
                                confirmNgaLogin(
                                    qrkey,
                                    onSuccess = {
                                        confirming = false
                                        result = "登录确认成功！网页端应该已经登录。"
                                    },
                                    onError = { e ->
                                        confirming = false
                                        result = "确认失败：${e.message}"
                                    }
                                )
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
                        color = MaterialTheme.colorScheme.onSurface
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
 * Confirms the NGA web login by loading the qrlogin_ui URL with the app's
 * auth cookies. Runs off the main thread.
 */
private fun confirmNgaLogin(
    qrkey: String,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit,
) {
    val url = "https://bbs.nga.cn/nuke.php?__lib=login&__act=qrlogin_ui&qrkey=$qrkey"
    GlobalScope.launch(Dispatchers.IO) {
        try {
            val cookies = CookieManager.getInstance().getCookie("https://bbs.nga.cn") ?: ""
            Log.d(TAG, "Confirming login with cookies present=${cookies.isNotEmpty()}")
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Cookie", cookies)
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"
                )
                connectTimeout = 15000
                readTimeout = 15000
            }
            val code = conn.responseCode
            Log.d(TAG, "qrlogin_ui response code=$code")
            // Read the response to ensure the request completes
            try {
                conn.inputStream.bufferedReader().readText()
            } catch (_: Exception) {
            }
            conn.disconnect()
            withContext(Dispatchers.Main) {
                if (code in 200..399) onSuccess() else onError(Exception("HTTP $code"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "confirm failed", e)
            withContext(Dispatchers.Main) { onError(e) }
        }
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
