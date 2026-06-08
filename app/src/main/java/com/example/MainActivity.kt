package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.api.GradingResult
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    GraderAppScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraderAppScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current

    // UI inputs / states from ViewModel
    val keyUri by viewModel.keyImageUri.collectAsState()
    val studentUri by viewModel.studentImageUri.collectAsState()
    val keyLighting by viewModel.keyLighting.collectAsState()
    val studentLighting by viewModel.studentLighting.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()

    // Key masking & local configuration ui flags
    var showApiKey by remember { mutableStateOf(false) }
    var showConfigPanel by remember { mutableStateOf(false) }

    // Standard photo picker launchers
    val keyPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.selectKeyImage(uri, context)
            }
        }
    )

    val studentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.selectStudentImage(uri, context)
            }
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Title TopBar (Bento Header Style)
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AI",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = "Auto Grader",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Koreksi Lembar Jawaban Cerdas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            ),
            actions = {
                IconButton(
                    onClick = { showConfigPanel = !showConfigPanel },
                    modifier = Modifier.testTag("action_settings")
                ) {
                    Icon(
                        imageVector = if (showConfigPanel) Icons.Default.Close else Icons.Default.Settings,
                        contentDescription = "Pengaturan",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Settings / API Config Panel (Expandable)
            item {
                AnimatedVisibility(
                    visible = showConfigPanel || apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY",
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .testTag("settings_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info Key",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Konfigurasi API Gemini AI",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Aplikasi ini memanggil langsung REST API Gemini AI secara mandiri untuk keamanan total. Masukkan API Key Anda di bawah ini, atau letakkan kunci di menu AI Studio Secrets.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Password/API Key input
                            OutlinedTextField(
                                value = if (apiKey == "MY_GEMINI_API_KEY") "" else apiKey,
                                onValueChange = { viewModel.updateApiKey(it) },
                                label = { Text("Gemini API Key") },
                                placeholder = { Text("Masukkan API Key Anda...") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("api_key_input"),
                                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { showApiKey = !showApiKey }) {
                                        Icon(
                                            imageVector = if (showApiKey) Icons.Default.Clear else Icons.Default.Star,
                                            contentDescription = if (showApiKey) "Sembunyikan" else "Tampilkan"
                                        )
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Model Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Model Vision AI",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (selectedModel == "gemini-3.1-pro-preview") "Pro Model: Akurasi OCR & Uraian Tinggi" else "Flash Model: Proses Sangat Cepat",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                var modelDropdownExpanded by remember { mutableStateOf(false) }

                                Box {
                                    FilledTonalButton(
                                        onClick = { modelDropdownExpanded = true },
                                        modifier = Modifier.testTag("model_selector_btn")
                                    ) {
                                        Text(text = if (selectedModel == "gemini-3.1-pro-preview") "Gemini 3.1 Pro" else "Gemini 3.5 Flash")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Pilih"
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = modelDropdownExpanded,
                                        onDismissRequest = { modelDropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Gemini 3.1 Pro (Sangat Akurat - Disarankan)") },
                                            onClick = {
                                                viewModel.updateSelectedModel("gemini-3.1-pro-preview")
                                                modelDropdownExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Gemini 3.5 Flash (Cepat & Ringan)") },
                                            onClick = {
                                                viewModel.updateSelectedModel("gemini-3.5-flash")
                                                modelDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Twin Selection Cards
            item {
                Text(
                    text = "Langkah 1: Pilih / Foto Dokumen Pendukung",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 0.1.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card A: Kunci Jawaban
                    Column(modifier = Modifier.weight(1f)) {
                        ImagePickerCard(
                            title = "Kunci Jawaban",
                            description = "Kunci jawaban cetak atau komputer",
                            imageUri = keyUri,
                            lightingAnalysis = keyLighting,
                            onPickImage = {
                                keyPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onClearImage = { viewModel.selectKeyImage(null, context) },
                            testTag = "key_picker_card"
                        )
                    }

                    // Card B: Jawaban Siswa
                    Column(modifier = Modifier.weight(1f)) {
                        ImagePickerCard(
                            title = "Lembar Murid",
                            description = "Jawaban siswa tulisan tangan",
                            imageUri = studentUri,
                            lightingAnalysis = studentLighting,
                            onPickImage = {
                                studentPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onClearImage = { viewModel.selectStudentImage(null, context) },
                            testTag = "student_picker_card"
                        )
                    }
                }
            }

            // Bento Status Bar
            item {
                val isActive = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = if (isActive) {
                                val modelReadable = if (selectedModel == "gemini-3.1-pro-preview") "Gemini 3.1 Pro" else "Gemini 3.5 Flash"
                                "$modelReadable Aktif & Terhubung"
                            } else {
                                "Mode Offline Aktif (Koreksi Tanpa API Key)"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // 3. Action Buttons Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.reset() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("reset_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        enabled = keyUri != null || studentUri != null || uiState !is GradingUiState.Idle
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Reset")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset", fontWeight = FontWeight.Bold)
                    }

                    val isEnabled = keyUri != null && studentUri != null && uiState !is GradingUiState.Loading
                    Button(
                        onClick = { viewModel.executeGrading(context) },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(52.dp)
                            .testTag("grade_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        enabled = isEnabled
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Koreksi")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Koreksi Nilai", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 4. Loading State Card
            item {
                AnimatedVisibility(visible = uiState is GradingUiState.Loading) {
                    val loadingStep = (uiState as? GradingUiState.Loading)?.step ?: "Memproses data..."
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("loading_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sedang Mengoreksi Jawaban",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = loadingStep,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 5. Error Message Card
            item {
                AnimatedVisibility(visible = uiState is GradingUiState.Error) {
                    val errorState = uiState as? GradingUiState.Error
                    val errorMsg = errorState?.message ?: "Terjadi kegagalan."
                    val rawResp = errorState?.rawResponse

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("error_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Pemberitahuan Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Proses Gagal",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.executeOfflineGrading(context) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Offline")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Koreksi dengan Mode Offline", fontWeight = FontWeight.Bold)
                            }

                            if (rawResp != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Respon Mentah AI:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Text(
                                        text = rawResp,
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 10,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Grading Results Section (Display when Success)
            if (uiState is GradingUiState.Success) {
                val results = (uiState as GradingUiState.Success).result

                item {
                    Text(
                        text = "Langkah 2: Hasil Evaluasi Lembar Jawaban",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                // Student Identity and Summary Score Card
                item {
                    GradingScoreSummaryCard(results = results, context = context)
                }

                // Correction Detail header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Lembar Evaluasi Soal Detil",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                text = "${results.detailKoreksi.size} Soal",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Table Rows representation of all question details parsed
                items(results.detailKoreksi) { detail ->
                    QuestionCorrectionDetailRow(detail = detail)
                }
            } else {
                // If IDLE, show a quick elegant tutorial card on how to snap pictures
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "💡 Panduan Pengambilan Foto Optimal",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "1. Ambil foto dari sudut tegak lurus (bird's-eye view) tepat di atas kertas untuk menghindari distorsi tulisan.\n" +
                                        "2. Pastikan tulisan jawaban siswa (baik pilihan ganda, isian singkat, ataupun esai) terbaca cukup jelas oleh mata sebelum difoto.\n" +
                                        "3. Detektor pencahayaan otomotis di bawah kotak foto akan memberitahu Anda secara instan jika pencahayaan ruangan kurang terang atau memiliki pantulan silau.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
        
        // Safety Warning block for stored API keys
        Text(
            text = "Security Warning: Aplikasi purwarupa ini menyimpan API key secara lokal pada ram dan build configuration. Hindari membagikan berkas APK secara terbuka.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}

@Composable
fun ImagePickerCard(
    title: String,
    description: String,
    imageUri: Uri?,
    lightingAnalysis: LightingAnalysis?,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            1.dp,
            if (imageUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Central clickable click-to-pick area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { onPickImage() },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = imageUri),
                            contentDescription = "Preview $title",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Dynamic bottom overlay banner with card name
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = title.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.White
                            )
                        }
                        // Clear button floating top right
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .size(28.dp)
                                .clickable { onClearImage() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus Gambar",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Pilih Gambar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = title.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Footer Lighting Detection report
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (lightingAnalysis != null) {
                            when (lightingAnalysis.quality) {
                                LightingQuality.GOOD -> Color(0xFFE8F5E9)
                                LightingQuality.POOR_DARK -> Color(0xFFFFF3E0)
                                LightingQuality.POOR_BRIGHT -> Color(0xFFFFFDE7)
                            }
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (lightingAnalysis != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = when (lightingAnalysis.quality) {
                                LightingQuality.GOOD -> Icons.Default.Check
                                else -> Icons.Default.Warning
                            },
                            contentDescription = "Pencahayaan",
                            tint = when (lightingAnalysis.quality) {
                                LightingQuality.GOOD -> Color(0xFF2E7D32)
                                LightingQuality.POOR_DARK -> Color(0xFFE65100)
                                LightingQuality.POOR_BRIGHT -> Color(0xFFFBC02D)
                            },
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (lightingAnalysis.quality) {
                                LightingQuality.GOOD -> "Cahaya OK"
                                LightingQuality.POOR_DARK -> "Gelap (${lightingAnalysis.score})"
                                LightingQuality.POOR_BRIGHT -> "Silau (${lightingAnalysis.score})"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when (lightingAnalysis.quality) {
                                LightingQuality.GOOD -> Color(0xFF2E7D32)
                                LightingQuality.POOR_DARK -> Color(0xFFE65100)
                                LightingQuality.POOR_BRIGHT -> Color(0xFFFBC02D)
                            }
                        )
                    }
                } else {
                    Text(
                        text = "Analisis pencahayaan...",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun GradingScoreSummaryCard(
    results: GradingResult,
    context: Context,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("result_summary_card"),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column {
            // Rounded Inner Top Header Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HASIL KOREKSI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = results.namaMurid.ifBlank { "Tidak Terbaca" },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Rounded score stamp in Bento Grid theme style
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = results.skorTotal.toString(),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Skor Total".uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Divider stroke
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )

            // Inner body region with summaries and actions
            Column(modifier = Modifier.padding(20.dp)) {
                // Inline status badges row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Benar dot
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Text(
                            text = "${results.jumlahBenar} Benar",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Salah dot
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF44336))
                        )
                        Text(
                            text = "${results.jumlahSalah} Salah",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // WhatsApp action button
                Button(
                    onClick = { shareToWhatsApp(context, results) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("whatsapp_share_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "WhatsApp", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bagikan ke WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QuestionCorrectionDetailRow(
    detail: com.example.api.CorrectionDetail,
    modifier: Modifier = Modifier
) {
    val cardBgColor = if (detail.statusBenar) Color(0xFFF7F2FA) else Color(0xFFFFEBEE)
    val badgeBgColor = if (detail.statusBenar) MaterialTheme.colorScheme.primary else Color(0xFFC62828)
    val statusTextColor = if (detail.statusBenar) Color(0xFF2E7D32) else Color(0xFFC62828)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("correction_item_${detail.nomorSoal}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = BorderStroke(
            1.dp,
            if (detail.statusBenar) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color(0xFFFFCDD2)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded bento index badge
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(badgeBgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = detail.nomorSoal.padStart(2, '0'),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Information details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Soal Nomor ${detail.nomorSoal}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Text Badge
                    Text(
                        text = if (detail.statusBenar) "√ BENAR" else "× SALAH",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        color = statusTextColor,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Jawaban Murid:",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = detail.jawabanMurid.ifBlank { "Kosong" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (detail.statusBenar) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kunci Jawaban:",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = detail.jawabanSeharusnya.ifBlank { "Kosong" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}

fun shareToWhatsApp(context: Context, result: GradingResult) {
    val sb = java.lang.StringBuilder()
    sb.append("📋 *HASIL KOREKSI UJIAN - AI AUTO GRADER*\n")
    sb.append("===================================\n")
    sb.append("👤 *Nama Siswa:* ${result.namaMurid}\n")
    sb.append("💯 *Skor Akhir:* *${result.skorTotal}* / 100\n")
    sb.append("✅ *Jawaban Benar:* ${result.jumlahBenar}\n")
    sb.append("❌ *Jawaban Salah:* ${result.jumlahSalah}\n\n")
    sb.append("*Detail Analisis Evaluasi per Soal:*\n")

    result.detailKoreksi.forEach { item ->
        val emoji = if (item.statusBenar) "✅" else "❌"
        val statusText = if (item.statusBenar) "Benar" else "Salah"
        sb.append("$emoji Nomor ${item.nomorSoal}: $statusText (Jawaban Murid: ${item.jawabanMurid} | Kunci: ${item.jawabanSeharusnya})\n")
    }

    sb.append("\n_Diperiksa otomatis secara cerdas menggunakan teknologi AI Auto Grader_")

    val textMessage = sb.toString()

    // 1. First choice target WhatsApp package specifically
    val waIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, textMessage)
        setPackage("com.whatsapp")
    }

    try {
        context.startActivity(waIntent)
    } catch (e: Exception) {
        // 2. Clear fallback using standard system picker chooser
        val generalIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, textMessage)
        }
        context.startActivity(Intent.createChooser(generalIntent, "Bagikan Hasil"))
    }
}
