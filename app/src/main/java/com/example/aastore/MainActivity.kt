package com.example.aastore

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { StoreScreen() } } }
    }
}

private fun installedVersion(ctx: Context, pkg: String): Long? = try {
    ctx.packageManager.getPackageInfo(pkg, 0).longVersionCode
} catch (e: Exception) { null }

@Composable
fun StoreScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    val status = remember { mutableStateMapOf<String, String>() }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh) {
        loading = true; error = null
        try { apps = withContext(Dispatchers.IO) { CatalogRepo.fetch() } }
        catch (e: Exception) { error = "No se pudo cargar el catálogo: ${e.message}" }
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("AAStore", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { refresh++ }) { Text("Actualizar") }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(apps) { app ->
                val isLink = app.openUrl.isNotEmpty()
                val installed = if (isLink) null else installedVersion(ctx, app.packageName)
                val label = when {
                    isLink -> "Abrir"
                    installed == null -> "Instalar"
                    installed < app.versionCode -> "Actualizar"
                    else -> "Instalada"
                }
                val key = app.name
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(app.name, style = MaterialTheme.typography.titleMedium)
                        if (app.versionName.isNotEmpty())
                            Text("v${app.versionName}", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(app.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                enabled = label != "Instalada" && status[key] == null,
                                onClick = {
                                    if (isLink) {
                                        ctx.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(app.openUrl))
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    } else scope.launch {
                                        try {
                                            status[key] = "Descargando 0%"
                                            val f = withContext(Dispatchers.IO) {
                                                Installer.download(ctx, app) { p -> status[key] = "Descargando $p%" }
                                            }
                                            Installer.install(ctx, f)
                                        } catch (e: Exception) {
                                            error = "Error con ${app.name}: ${e.message}"
                                        } finally { status.remove(key) }
                                    }
                                }
                            ) { Text(label) }
                            status[key]?.let { Spacer(Modifier.width(12.dp)); Text(it) }
                        }
                    }
                }
            }
        }
    }
}
