package com.pamoron.electroperico.ui.ocr

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pamoron.electroperico.R
import com.pamoron.electroperico.domain.ocr.ChargerTextCandidates
import com.pamoron.electroperico.domain.ocr.ChargerTextParser
import com.pamoron.electroperico.ui.format.Formatters
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.stringResource

/**
 * Lector OCR local de tarifas. La cámara solo se activa tras una acción
 * explícita y los datos detectados nunca se escriben sin confirmación.
 */
@Composable
fun OcrScannerScreen(
    onApply: (ChargerTextCandidates) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var candidates by remember { mutableStateOf<ChargerTextCandidates?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    DisposableEffect(Unit) {
        onDispose { cameraProvider?.unbindAll() }
    }

    if (!granted) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.titulo_lector_camara), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.permiso_camara_denegado))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text(stringResource(R.string.accion_leer_con_camara))
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.accion_cancelar)) }
        }
        return
    }

    if (candidates == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { viewContext ->
                    PreviewView(viewContext).also { previewView ->
                        bindCamera(
                            previewView = previewView,
                            lifecycleOwner = lifecycleOwner,
                            onProviderReady = { cameraProvider = it },
                            onCandidates = { candidates = it },
                        )
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.ayuda_lector_camara),
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) { Text(stringResource(R.string.accion_cancelar)) }
        }
    } else {
        ConfirmOcrDialog(
            candidates = checkNotNull(candidates),
            onApply = { onApply(checkNotNull(candidates)) },
            onDismiss = { candidates = null },
        )
    }
}

private fun bindCamera(
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    onProviderReady: (ProcessCameraProvider) -> Unit,
    onCandidates: (ChargerTextCandidates) -> Unit,
) {
    val context = previewView.context
    val executor = Executors.newSingleThreadExecutor()
    val found = AtomicBoolean(false)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val providerFuture = ProcessCameraProvider.getInstance(context)
    providerFuture.addListener({
        val provider = providerFuture.get()
        onProviderReady(provider)
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(executor) { proxy ->
            val image = proxy.image
            if (image == null || found.get()) {
                proxy.close()
            } else {
                recognizer.process(InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees))
                    .addOnSuccessListener { text ->
                        val values = ChargerTextParser.parse(text.text)
                        if (
                            (values.pricePerKWh != null || values.chargerPowerKw != null) &&
                            found.compareAndSet(false, true)
                        ) {
                            ContextCompat.getMainExecutor(context).execute { onCandidates(values) }
                            provider.unbindAll()
                            recognizer.close()
                            executor.shutdown()
                        }
                    }
                    .addOnCompleteListener { proxy.close() }
            }
        }
        provider.unbindAll()
        provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analysis,
        )
    }, ContextCompat.getMainExecutor(context))
}

/** Diálogo obligatorio que permite aceptar o descartar las propuestas OCR. */
@Composable
private fun ConfirmOcrDialog(
    candidates: ChargerTextCandidates,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.titulo_confirmar_lectura)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.texto_confirmar_lectura))
                candidates.pricePerKWh?.let {
                    Text(stringResource(R.string.lectura_precio, Formatters.toEditableText(it)))
                }
                candidates.chargerPowerKw?.let {
                    Text(stringResource(R.string.lectura_potencia, Formatters.toEditableText(it)))
                }
                candidates.pricePerMinuteEur?.let {
                    Text(stringResource(R.string.lectura_coste_minuto, Formatters.toEditableText(it)))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onApply) { Text(stringResource(R.string.accion_aplicar_lectura)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.accion_cancelar)) }
        },
    )
}
