package com.belleval.enmerkar.type.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.belleval.enmerkar.type.BuildConfig
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.belleval.enmerkar.type.R
import com.belleval.enmerkar.type.diagnostic.DiagnosticLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticLogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lines by DiagnosticLog.lines.collectAsState()

    LaunchedEffect(Unit) {
        DiagnosticLog.i("UI", "DiagnosticLogScreen opened")
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diagnostic_log_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.diagnostic_back),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.diagnostic_log_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalButton(
                        onClick = {
                            val text = header(context) + DiagnosticLog.dumpSnapshotText()
            val cm =
                                context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Uruk IME diagnostic", text))
                            Toast
                                .makeText(
                                    context,
                                    context.getString(R.string.diagnostic_copied),
                                    Toast.LENGTH_SHORT,
                                ).show()
                        },
                    ) {
                        Icon(Icons.Default.ContentCopy, null, Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.diagnostic_copy))
                    }
                    OutlinedButton(
                        onClick = {
                            val text = header(context) + DiagnosticLog.dumpSnapshotText()
                            val send =
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.diagnostic_share_subject))
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                            context.startActivity(
                                Intent.createChooser(send, context.getString(R.string.diagnostic_share)),
                            )
                        },
                    ) {
                        Icon(Icons.Default.Share, null, Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.diagnostic_share))
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        DiagnosticLog.clear()
                        Toast
                            .makeText(
                                context,
                                context.getString(R.string.diagnostic_cleared),
                                Toast.LENGTH_SHORT,
                            ).show()
                    },
                ) {
                    Icon(Icons.Default.DeleteSweep, null, Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.diagnostic_clear))
                }
            }
            item { HorizontalDivider() }
            itemsIndexed(lines) { _, line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun header(context: Context): String =
    buildString {
        append("Uruk IME diagnostic export\n")
        append("package=").append(context.packageName).append("\n")
        append("version=").append(BuildConfig.VERSION_NAME).append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
        append("---\n")
    }
