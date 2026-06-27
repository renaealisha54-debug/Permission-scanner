package com.permissionscanner.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.permissionscanner.app.scanner.Finding

private val CategoryColors = mapOf(
    "permission"  to Color(0xFF7C4DFF),
    "network"     to Color(0xFF0288D1),
    "camera"      to Color(0xFF00897B),
    "storage"     to Color(0xFFF57C00),
    "location"    to Color(0xFF43A047),
    "contacts"    to Color(0xFFE91E63),
    "phone"       to Color(0xFF8D6E63),
    "sms"         to Color(0xFFD81B60),
    "microphone"  to Color(0xFF6D4C41),
    "execute"     to Color(0xFFD32F2F),
    "crypto"      to Color(0xFF1565C0),
    "reflection"  to Color(0xFF6A1B9A),
)

@Composable
fun ScannerScreen(vm: ScannerViewModel) {
    val state by vm.state.collectAsState()
    var pathInput by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { vm.scanFromUri(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permission Scanner", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { filePicker.launch("*/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pick ZIP / APK")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            OutlinedTextField(
                value = pathInput,
                onValueChange = { pathInput = it },
                label = { Text("Or paste file path") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    keyboard?.hide()
                    if (pathInput.isNotBlank()) vm.scanFromPath(pathInput)
                }),
                trailingIcon = {
                    TextButton(onClick = {
                        keyboard?.hide()
                        if (pathInput.isNotBlank()) vm.scanFromPath(pathInput)
                    }) { Text("Scan") }
                }
            )

            AnimatedContent(targetState = state, label = "scan_state") { s ->
                when (s) {
                    is ScanState.Idle -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No scan yet. Pick a file or enter a path.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    is ScanState.Scanning -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is ScanState.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()) {
                            Text("Error: ${s.message}", color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { vm.reset() }) { Text("Reset") }
                        }
                    }
                    is ScanState.Done -> {
                        FindingsView(findings = s.findings, onReset = { vm.reset(); pathInput = "" })
                    }
                }
            }
        }
    }
}

@Composable
private fun FindingsView(findings: List<Finding>, onReset: () -> Unit) {
    if (findings.isEmpty()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center) {
            Text("No risky patterns found.", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onReset) { Text("Scan another") }
        }
        return
    }

    val grouped = findings.groupBy { it.category }

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("${findings.size} finding(s) in ${findings.map { it.file }.distinct().size} file(s)",
                style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = onReset) { Text("Reset") }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            grouped.forEach { (category, hits) ->
                item {
                    val color = CategoryColors[category] ?: MaterialTheme.colorScheme.primary
                    Text(
                        text = category.uppercase(),
                        color = color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(hits) { finding ->
                    FindingCard(finding)
                }
            }
        }
    }
}

@Composable
private fun FindingCard(finding: Finding) {
    val color = CategoryColors[finding.category] ?: MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = finding.file,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                finding.matches.forEach { match ->
                    Box(
                        modifier = Modifier
                            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(match, fontSize = 11.sp, color = color, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            Text(
                text = "Lines: ${finding.lineNumbers.take(10).joinToString(", ")}${if (finding.lineNumbers.size > 10) "…" else ""}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
