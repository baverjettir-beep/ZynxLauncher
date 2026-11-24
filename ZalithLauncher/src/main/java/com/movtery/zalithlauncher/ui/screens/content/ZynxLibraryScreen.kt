/*
 * Zynx Launcher - ZynxModMenu / ZynxResourcePack / ZynxShaders / ZynxModPack
 * Based on Zalith Launcher 2 (GPL-3.0)
 */

package com.movtery.zalithlauncher.ui.screens.content

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.zynx.ZynxHit
import com.movtery.zalithlauncher.zynx.ZynxKind
import com.movtery.zalithlauncher.zynx.ZynxModrinth
import com.movtery.zalithlauncher.zynx.ZynxReport
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ZynxLibraryScreen(
    key: NormalNavKey.ZynxLibrary,
    backScreenViewModel: ScreenBackStackViewModel
) {
    BaseScreen(
        screenKey = key,
        currentKey = backScreenViewModel.mainScreen.currentKey,
        useClassEquality = true
    ) { _ ->
        ZynxLibraryContent(
            kindIndex = key.kind,
            onBack = { backScreenViewModel.mainScreen.backStack.removeLastOrNull() }
        )
    }
}

@Composable
private fun ZynxLibraryContent(
    kindIndex: Int,
    onBack: () -> Unit
) {
    val version by VersionsManager.currentVersion.collectAsState()
    val packPage = kindIndex == 3
    val kind = ZynxKind.fromIndex(if (packPage) 0 else kindIndex)
    val titleRes = when (kindIndex) {
        1 -> R.string.zl_title_rp
        2 -> R.string.zl_title_shaders
        3 -> R.string.zl_title_pack
        else -> R.string.zl_title_mods
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = null)
            }
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        val current = version
        if (current == null) {
            Text(
                text = stringResource(R.string.zl_no_version),
                style = MaterialTheme.typography.bodyMedium
            )
            return@Column
        }

        val mc = ZynxModrinth.minecraftVersionOf(current)
        val loader = ZynxModrinth.loaderOf(current)
        val loaders = ZynxModrinth.loadersOf(loader)

        Text(
            text = stringResource(
                R.string.zl_version_line,
                current.getVersionInfo()?.getInfoString() ?: current.getVersionName()
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (packPage) {
            ModPackPanel(version = current, loader = loader)
        } else {
            LibraryList(version = current, kind = kind, mc = mc, loaders = loaders)
        }
    }
}

@Composable
private fun ModPackPanel(
    version: Version,
    loader: ModLoader?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val slugs = remember(loader) { ZynxModrinth.modPackSlugs(loader) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.zl_pack_desc),
            style = MaterialTheme.typography.bodyMedium
        )
        if (slugs.isEmpty()) {
            Text(
                text = stringResource(R.string.zl_no_loader),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Text(
                text = slugs.joinToString(", "),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        status = ""
                        try {
                            val report: ZynxReport = ZynxModrinth.installModPack(version) { slug ->
                                status = context.getString(R.string.zl_pack_working, slug)
                            }
                            status = context.getString(
                                R.string.zl_pack_done,
                                report.installed.size,
                                report.skipped.size
                            )
                        } catch (e: Exception) {
                            status = context.getString(R.string.zl_failed, e.message ?: "")
                        } finally {
                            busy = false
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.zl_pack_install))
            }
            if (busy) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            if (status.isNotBlank()) {
                Text(text = status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LibraryList(
    version: Version,
    kind: ZynxKind,
    mc: String?,
    loaders: List<String>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    val folder = remember(version, kind) { ZynxModrinth.folderOf(version, kind) }
    val files: List<File> = remember(folder, refresh) {
        folder.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".part") }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    var showBrowse by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<ZynxHit>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var installing by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    val noLoader = kind == ZynxKind.MOD && loaders.isEmpty()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val ok = mutableListOf<String>()
        val bad = mutableListOf<String>()
        uris.forEach { uri ->
            val (success, name) = copyPicked(context, uri, folder, kind)
            if (success) ok.add(name) else bad.add(name)
        }
        message = buildString {
            if (ok.isNotEmpty()) append(context.getString(R.string.zl_file_added, ok.joinToString(", ")))
            if (bad.isNotEmpty()) {
                if (isNotEmpty()) append("\n")
                append(context.getString(R.string.zl_file_bad, kind.extension))
            }
        }
        refresh++
    }

    val doSearch: () -> Unit = {
        scope.launch {
            loading = true
            message = ""
            try {
                hits = ZynxModrinth.search(kind, query.trim(), mc, loaders)
                if (hits.isEmpty()) message = context.getString(R.string.zl_no_results)
            } catch (e: Exception) {
                message = context.getString(R.string.zl_failed, e.message ?: "")
            } finally {
                loading = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { picker.launch(arrayOf("*/*")) }) {
                    Text(stringResource(R.string.zl_add_file))
                }
                OutlinedButton(
                    enabled = !noLoader,
                    onClick = {
                        showBrowse = !showBrowse
                        if (showBrowse && hits.isEmpty()) doSearch()
                    }
                ) {
                    Text(
                        stringResource(
                            if (showBrowse) R.string.zl_hide_browse else R.string.zl_browse
                        )
                    )
                }
            }
        }

        if (noLoader) {
            item {
                Text(
                    text = stringResource(R.string.zl_no_loader),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (message.isNotBlank()) {
            item {
                Text(text = message, style = MaterialTheme.typography.bodySmall)
            }
        }

        if (showBrowse) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.zl_search_hint)) }
                    )
                    Button(enabled = !loading, onClick = { doSearch() }) {
                        Text(stringResource(R.string.zl_search))
                    }
                }
            }
            if (loading) {
                item { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
            }
            items(hits) { hit ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = hit.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = hit.description,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${hit.author} · ${hit.downloads}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (installing == hit.projectId) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Button(
                                enabled = installing == null,
                                onClick = {
                                    scope.launch {
                                        installing = hit.projectId
                                        message = ""
                                        try {
                                            val report = ZynxReport()
                                            ZynxModrinth.install(
                                                project = hit.projectId,
                                                kind = kind,
                                                mcVersion = mc,
                                                loaders = loaders,
                                                targetDir = folder,
                                                report = report
                                            )
                                            message = if (report.installed.isEmpty()) {
                                                context.getString(R.string.zl_no_compat, hit.title)
                                            } else {
                                                context.getString(
                                                    R.string.zl_installed_ok,
                                                    report.installed.joinToString(", ")
                                                )
                                            }
                                            refresh++
                                        } catch (e: Exception) {
                                            message = context.getString(R.string.zl_failed, e.message ?: "")
                                        } finally {
                                            installing = null
                                        }
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.zl_install))
                            }
                        }
                    }
                }
            }
            item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }
        }

        item {
            Text(
                text = stringResource(R.string.zl_installed_header, files.size),
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (files.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.zl_empty),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        items(files) { file ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = file.name,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
                IconButton(onClick = {
                    file.delete()
                    refresh++
                }) {
                    Icon(painterResource(R.drawable.ic_delete_outlined), contentDescription = null)
                }
            }
        }
    }
}

/**
 * Copies a user-picked file into the version folder.
 * @return (success, file name)
 */
private fun copyPicked(
    context: Context,
    uri: Uri,
    folder: File,
    kind: ZynxKind
): Pair<Boolean, String> {
    var name = uri.lastPathSegment ?: "file"
    try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) name = it.getString(0)
        }
    } catch (_: Exception) {
    }
    name = File(name).name
    if (!name.endsWith("." + kind.extension, ignoreCase = true)) return false to name
    return try {
        val dest = File(folder, name)
        val input = context.contentResolver.openInputStream(uri) ?: return false to name
        input.use { src -> dest.outputStream().use { out -> src.copyTo(out) } }
        true to name
    } catch (e: Exception) {
        false to name
    }
}
