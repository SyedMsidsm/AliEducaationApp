package com.alieducational.app.ui.billing

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.EnumMap

class BarcodeAnalyzer(
    private val onBarcodeDetected: (barcode: String, format: String) -> Unit,
    private val onFrameEmpty: () -> Unit = {}
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(
                DecodeHintType.POSSIBLE_FORMATS,
                listOf(
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.ITF,
                    BarcodeFormat.QR_CODE
                )
            )
            put(DecodeHintType.TRY_HARDER, java.lang.Boolean.TRUE)
        }
        setHints(hints)
    }

    private var lastScannedCode: String? = null
    private var lastScannedTimestamp: Long = 0L
    private var consecutiveEmptyFrames = 0

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val plane = mediaImage.planes[0]
            val buffer: ByteBuffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            val width = imageProxy.width
            val height = imageProxy.height

            val yData = ByteArray(width * height)
            if (rowStride == width && pixelStride == 1) {
                buffer.get(yData)
            } else {
                for (row in 0 until height) {
                    buffer.position(row * rowStride)
                    if (pixelStride == 1) {
                        buffer.get(yData, row * width, width)
                    } else {
                        for (col in 0 until width) {
                            yData[row * width + col] = buffer.get(row * rowStride + col * pixelStride)
                        }
                    }
                }
            }

            val rotation = imageProxy.imageInfo.rotationDegrees

            val source = when (rotation) {
                90 -> {
                    val rotatedData = ByteArray(width * height)
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            rotatedData[x * height + (height - y - 1)] = yData[x + y * width]
                        }
                    }
                    PlanarYUVLuminanceSource(
                        rotatedData, height, width, 0, 0, height, width, false
                    )
                }
                270 -> {
                    val rotatedData = ByteArray(width * height)
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            rotatedData[(width - x - 1) * height + y] = yData[x + y * width]
                        }
                    }
                    PlanarYUVLuminanceSource(
                        rotatedData, height, width, 0, 0, height, width, false
                    )
                }
                180 -> {
                    val rotatedData = ByteArray(width * height)
                    for (i in 0 until width * height) {
                        rotatedData[i] = yData[width * height - 1 - i]
                    }
                    PlanarYUVLuminanceSource(
                        rotatedData, width, height, 0, 0, width, height, false
                    )
                }
                else -> {
                    PlanarYUVLuminanceSource(
                        yData, width, height, 0, 0, width, height, false
                    )
                }
            }

            var detected = false
            try {
                val bitmap = BinaryBitmap(HybridBinarizer(source))
                val result = reader.decodeWithState(bitmap)
                val rawCode = result.text?.trim()
                if (!rawCode.isNullOrEmpty()) {
                    detected = true
                    handleDetectedBarcode(rawCode, result.barcodeFormat.name)
                }
            } catch (e: Exception) {
                // Fallback attempt with unrotated buffer if rotation failed
                if (rotation != 0) {
                    try {
                        val fallbackSource = PlanarYUVLuminanceSource(
                            yData, width, height, 0, 0, width, height, false
                        )
                        val fallbackBitmap = BinaryBitmap(HybridBinarizer(fallbackSource))
                        val fallbackResult = reader.decodeWithState(fallbackBitmap)
                        val rawCode = fallbackResult.text?.trim()
                        if (!rawCode.isNullOrEmpty()) {
                            detected = true
                            handleDetectedBarcode(rawCode, fallbackResult.barcodeFormat.name)
                        }
                    } catch (e2: Exception) {
                        // ignore
                    }
                }
            } finally {
                reader.reset()
            }

            if (!detected) {
                consecutiveEmptyFrames++
                if (consecutiveEmptyFrames > 15) {
                    lastScannedCode = null
                    onFrameEmpty()
                }
            }
        }
        imageProxy.close()
    }

    private fun handleDetectedBarcode(rawCode: String, formatName: String) {
        consecutiveEmptyFrames = 0
        val now = System.currentTimeMillis()
        if (rawCode != lastScannedCode || now - lastScannedTimestamp > 2500) {
            lastScannedCode = rawCode
            lastScannedTimestamp = now
            onBarcodeDetected(rawCode, formatName)
        }
    }

    fun resetLastScanned() {
        lastScannedCode = null
        lastScannedTimestamp = 0L
    }
}
