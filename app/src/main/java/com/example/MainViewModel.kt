package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.GradingResult
import com.example.api.InlineData
import com.example.api.Part
import com.example.api.RetrofitClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import android.util.Base64
import java.io.File
import com.example.api.OfflineGradingEngine

sealed interface GradingUiState {
    object Idle : GradingUiState
    data class Loading(val step: String) : GradingUiState
    data class Success(val result: GradingResult) : GradingUiState
    data class Error(val message: String, val rawResponse: String? = null) : GradingUiState
}

enum class LightingQuality { GOOD, POOR_DARK, POOR_BRIGHT }

data class LightingAnalysis(
    val quality: LightingQuality,
    val score: Int,
    val message: String
)

class MainViewModel : ViewModel() {

    // Image URI States
    private val _keyImageUri = MutableStateFlow<Uri?>(null)
    val keyImageUri: StateFlow<Uri?> = _keyImageUri.asStateFlow()

    private val _studentImageUri = MutableStateFlow<Uri?>(null)
    val studentImageUri: StateFlow<Uri?> = _studentImageUri.asStateFlow()

    // Lighting Analysis States
    private val _keyLighting = MutableStateFlow<LightingAnalysis?>(null)
    val keyLighting: StateFlow<LightingAnalysis?> = _keyLighting.asStateFlow()

    private val _studentLighting = MutableStateFlow<LightingAnalysis?>(null)
    val studentLighting: StateFlow<LightingAnalysis?> = _studentLighting.asStateFlow()

    // Grading UI State
    private val _uiState = MutableStateFlow<GradingUiState>(GradingUiState.Idle)
    val uiState: StateFlow<GradingUiState> = _uiState.asStateFlow()

    // Key input state (prefilled from BuildConfig.GEMINI_API_KEY)
    private val _apiKey = MutableStateFlow(BuildConfig.GEMINI_API_KEY)
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    // Selected Model option
    private val _selectedModel = MutableStateFlow("gemini-3.1-pro-preview")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    fun updateApiKey(newKey: String) {
        _apiKey.value = newKey
    }

    fun updateSelectedModel(model: String) {
        _selectedModel.value = model
    }

    fun selectKeyImage(uri: Uri?, context: Context) {
        _keyImageUri.value = uri
        if (uri != null) {
            analyzeImageLighting(uri, isKey = true, context = context)
        } else {
            _keyLighting.value = null
        }
    }

    fun selectStudentImage(uri: Uri?, context: Context) {
        _studentImageUri.value = uri
        if (uri != null) {
            analyzeImageLighting(uri, isKey = false, context = context)
        } else {
            _studentLighting.value = null
        }
    }

    fun reset() {
        _keyImageUri.value = null
        _studentImageUri.value = null
        _keyLighting.value = null
        _studentLighting.value = null
        _uiState.value = GradingUiState.Idle
    }

    private fun analyzeImageLighting(uri: Uri, isKey: Boolean, context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            val bitmap = uriToBitmap(context, uri) ?: return@launch
            val analysis = doLightingAnalysis(bitmap)
            withContext(Dispatchers.Main) {
                if (isKey) {
                    _keyLighting.value = analysis
                } else {
                    _studentLighting.value = analysis
                }
            }
        }
    }

    private fun doLightingAnalysis(bitmap: Bitmap): LightingAnalysis {
        val width = bitmap.width
        val height = bitmap.height
        var totalLuminance = 0.0
        var sampleCount = 0

        // Subsample pixels for quick responsive on-device computation without blocking
        val stepX = (width / 40).coerceAtLeast(1)
        val stepY = (height / 40).coerceAtLeast(1)

        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                // Relative luminance formula per ITU-R BT.601
                val luminance = 0.299 * r + 0.587 * g + 0.114 * b
                totalLuminance += luminance
                sampleCount++
            }
        }

        val avgLuminance = if (sampleCount > 0) totalLuminance / sampleCount else 127.0
        return when {
            avgLuminance < 65.0 -> LightingAnalysis(
                quality = LightingQuality.POOR_DARK,
                score = avgLuminance.toInt(),
                message = "Pencahayaan Redup (Skor: ${avgLuminance.toInt()}). Foto kemungkinan terlalu gelap. Disarankan memfoto ulang di ruangan terang."
            )
            avgLuminance > 220.0 -> LightingAnalysis(
                quality = LightingQuality.POOR_BRIGHT,
                score = avgLuminance.toInt(),
                message = "Pencahayaan Silau (Skor: ${avgLuminance.toInt()}). Foto terlalu terang atau memiliki silau tinggi. Coba kurangi paparan cahaya langsung."
            )
            else -> LightingAnalysis(
                quality = LightingQuality.GOOD,
                score = avgLuminance.toInt(),
                message = "Pencahayaan Optimal (Skor: ${avgLuminance.toInt()}). Foto siap diproses secara akurat!"
            )
        }
    }

    fun executeGrading(context: Context) {
        val keyUri = _keyImageUri.value
        val studentUri = _studentImageUri.value
        val currentKey = _apiKey.value.trim()

        if (keyUri == null || studentUri == null) {
            _uiState.value = GradingUiState.Error("Silakan pilih kedua gambar terlebih dahulu.")
            return
        }

        // Check if API Key is not set or default placeholder - fallback gracefully to robust local Offline Mode as requested!
        val isOfflineMode = currentKey.isEmpty() || currentKey == "MY_GEMINI_API_KEY"
        if (isOfflineMode) {
            executeOfflineGrading(context)
            return
        }

        _uiState.value = GradingUiState.Loading("Membaca dan memproses gambar...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Load Bitmaps
                val rawKeyBitmap = uriToBitmap(context, keyUri)
                val rawStudentBitmap = uriToBitmap(context, studentUri)

                if (rawKeyBitmap == null || rawStudentBitmap == null) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = GradingUiState.Error("Gagal membaca file gambar. Periksa izin atau coba foto file lain.")
                    }
                    return@launch
                }

                // 2. Downscale Bitmaps to optimize upload speeds and avoid OOM
                withContext(Dispatchers.Main) {
                    _uiState.value = GradingUiState.Loading("Mengoptimalkan ukuran berkas & konversi Base64...")
                }
                val resizedKey = rawKeyBitmap.resizeIfNeeded(1200)
                val resizedStudent = rawStudentBitmap.resizeIfNeeded(1200)

                val keyBase64 = resizedKey.toBase64(80)
                val studentBase64 = resizedStudent.toBase64(80)

                // 3. Assemble Gemini Request
                withContext(Dispatchers.Main) {
                    _uiState.value = GradingUiState.Loading("Mengirim ke Gemini AI untuk analisis lembar jawaban...")
                }

                val systemInstruction = "Kamu adalah sistem autograder asisten guru sekolah yang sangat teliti dan detail."

                val promptText = """
                Kamu adalah asisten guru yang ahli. Saya melampirkan dua gambar: Gambar pertama adalah Kunci Jawaban, Gambar kedua adalah Lembar Jawaban Murid (tulisan tangan). Tugasmu adalah mengoreksi jawaban murid.
                
                Aturan koreksi:
                - Untuk Pilihan Ganda (A, B, C, D): Cocokkan dengan pasti.
                - Untuk Isian Singkat / Uraian: Pahami tulisan tangan murid. Berikan toleransi untuk kesalahan ejaan (typo) minor selama maknanya sama dengan kunci jawaban.
                - Perhatikan penomoran: Terkadang nomor di lembar murid (misal 26-35) merujuk pada bagian B (1-10) di kunci jawaban. Petakan dengan cerdas.
                
                Output yang diminta:
                Berikan hasil berupa JSON yang rapi berisi: 'nama_murid', 'skor_total' (skala 100), 'jumlah_benar', 'jumlah_salah', dan 'detail_koreksi' (daftar nomor soal, status benar/salah, jawaban murid, dan jawaban seharusnya).
                
                Format JSON yang harus dipatuhi tepat seperti ini:
                {
                  "nama_murid": "John Doe",
                  "skor_total": 85.0,
                  "jumlah_benar": 17,
                  "jumlah_salah": 3,
                  "detail_koreksi": [
                     {
                       "nomor_soal": "1",
                       "status_benar": true,
                       "jawaban_murid": "A",
                       "jawaban_seharusnya": "A"
                     }
                  ]
                }
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(text = promptText),
                                Part(inlineData = InlineData(mimeType = "image/jpeg", data = keyBase64)),
                                Part(inlineData = InlineData(mimeType = "image/jpeg", data = studentBase64))
                            )
                        )
                    ),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.2f
                    ),
                    systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
                )

                // 4. Request from API
                val response = RetrofitClient.service.generateContent(
                    model = _selectedModel.value,
                    apiKey = currentKey,
                    request = request
                )

                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (responseText.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = GradingUiState.Error("Gemini mengembalikan respon kosong. Periksa gambar dan coba lagi.")
                    }
                    return@launch
                }

                // 5. Parse JSON response
                withContext(Dispatchers.Main) {
                    _uiState.value = GradingUiState.Loading("Memformat data hasil evaluasi...")
                }

                val cleanJson = extractJson(responseText)

                val moshi = Moshi.Builder()
                    .addLast(KotlinJsonAdapterFactory())
                    .build()
                val adapter = moshi.adapter(GradingResult::class.java)
                val gradingResult = adapter.fromJson(cleanJson)

                withContext(Dispatchers.Main) {
                    if (gradingResult != null) {
                        _uiState.value = GradingUiState.Success(gradingResult)
                    } else {
                        _uiState.value = GradingUiState.Error(
                            "Berhasil menerima respon, namun format JSON tidak sesuai struktur evaluasi.",
                            rawResponse = responseText
                        )
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value = GradingUiState.Error("Terjadi kesalahan: ${e.localizedMessage ?: e.toString()}")
                }
            }
        }
    }

    fun executeOfflineGrading(context: Context) {
        val keyUri = _keyImageUri.value
        val studentUri = _studentImageUri.value
        if (keyUri == null || studentUri == null) {
            _uiState.value = GradingUiState.Error("Silakan pilih kedua gambar (Kunci Jawaban & Lembar Murid) terlebih dahulu.")
            return
        }

        _uiState.value = GradingUiState.Loading("Menginisialisasi Mesin OCR Offline...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Load Bitmaps
                val keyBitmap = uriToBitmap(context, keyUri)
                val studentBitmap = uriToBitmap(context, studentUri)

                if (keyBitmap == null || studentBitmap == null) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = GradingUiState.Error("Gagal membaca berkas gambar secara lokal.")
                    }
                    return@launch
                }

                // 2. Perform OCR on Kunci Jawaban
                withContext(Dispatchers.Main) {
                    _uiState.value = GradingUiState.Loading("OCR Offline: Membaca Lembar Kunci Jawaban...")
                }
                val keyText = OfflineGradingEngine.extractTextFromBitmap(context, keyBitmap)

                // 3. Perform OCR on Lembar Murid
                withContext(Dispatchers.Main) {
                    _uiState.value = GradingUiState.Loading("OCR Offline: Membaca Tulisan Tangan Siswa...")
                }
                val studentText = OfflineGradingEngine.extractTextFromBitmap(context, studentBitmap)

                // 4. Run through local models/Heuristics
                withContext(Dispatchers.Main) {
                    _uiState.value = GradingUiState.Loading("Mengevaluasi Lembar Jawaban dengan AI Lokal...")
                }

                val localModelFile = getLocalModelFile(context)
                val result: GradingResult = if (localModelFile != null) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = GradingUiState.Loading("Menjalankan MediaPipe LLM Inference (On-Device)...")
                    }
                    val prompt = """
                        Kamu adalah sistem autograder offline super cerdas. Cocokkan Lembar Murid dengan Kunci Jawaban.
                        Kunci Jawaban:
                        $keyText
                        
                        Lembar Murid:
                        $studentText
                        
                        Kembalikan hasil grader berformat JSON seperti skema:
                        {
                          "nama_murid": "Ahmad Fauzi",
                          "skor_total": 88.0,
                          "jumlah_benar": 22,
                          "jumlah_salah": 3,
                          "detail_koreksi": [
                             {
                               "nomor_soal": "1",
                               "status_benar": true,
                               "jawaban_murid": "A",
                               "jawaban_seharusnya": "A"
                             }
                          ]
                        }
                    """.trimIndent()
                    val llmResponse = OfflineGradingEngine.gradeWithMediaPipeLlm(context, localModelFile, prompt)
                    val cleanJson = extractJson(llmResponse)
                    val moshi = Moshi.Builder()
                        .addLast(KotlinJsonAdapterFactory())
                        .build()
                    val adapter = moshi.adapter(GradingResult::class.java)
                    adapter.fromJson(cleanJson) ?: OfflineGradingEngine.gradeOfflineWithOcr(keyText, studentText)
                } else {
                    // Fallback to high-accuracy offline smart rule engine (ML Kit Text + Speel Tolerant heuristic matcher)
                    OfflineGradingEngine.gradeOfflineWithOcr(keyText, studentText)
                }

                withContext(Dispatchers.Main) {
                    _uiState.value = GradingUiState.Success(result)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value = GradingUiState.Error("Kesalahan Koreksi Offline: ${e.localizedMessage ?: e.toString()}")
                }
            }
        }
    }

    private fun getLocalModelFile(context: Context): File? {
        val possiblePaths = listOf(
            File(context.filesDir, "gemma-2b-it-cpu-int4.bin"),
            File(context.cacheDir, "gemma-2b-it-cpu-int4.bin"),
            File(context.getExternalFilesDir(null), "gemma-2b-it-cpu-int4.bin"),
            File("/data/local/tmp/gemma-2b-it-cpu-int4.bin")
        )
        return possiblePaths.firstOrNull { it.exists() }
    }

    // --- Helpers ---

    private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    private fun Bitmap.resizeIfNeeded(maxDimension: Int): Bitmap {
        val width = this.width
        val height = this.height
        if (width <= maxDimension && height <= maxDimension) return this

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
    }

    private fun Bitmap.toBase64(quality: Int): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun extractJson(text: String): String {
        val regex = "```json\\s*([\\s\\S]*?)\\s*```".toRegex()
        val matchResult = regex.find(text)
        return if (matchResult != null) {
            matchResult.groups[1]?.value?.trim() ?: text.trim()
        } else {
            val genericRegex = "```\\s*([\\s\\S]*?)\\s*```".toRegex()
            val genericMatch = genericRegex.find(text)
            if (genericMatch != null) {
                genericMatch.groups[1]?.value?.trim() ?: text.trim()
            } else {
                text.trim()
            }
        }
    }
}
