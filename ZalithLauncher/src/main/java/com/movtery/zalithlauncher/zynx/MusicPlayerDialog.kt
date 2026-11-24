/*
 * Zynx Launcher - music player dialog
 * Based on Zalith Launcher 2 (GPL-3.0)
 */

package com.movtery.zalithlauncher.zynx

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R

@Composable
fun MusicPlayerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val tracks = MusicPlayerManager.tracks
    val current = MusicPlayerManager.currentIndex

    LaunchedEffect(Unit) { MusicPlayerManager.load(context) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNullOrEmpty()) return@rememberLauncherForActivityResult
        uris.forEach {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
        }
        MusicPlayerManager.addTracks(context, uris)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.zynx_music_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (current in tracks.indices) tracks[current].title
                    else stringResource(R.string.zynx_music_nothing_playing),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { MusicPlayerManager.previous(context) }) {
                        Icon(painterResource(R.drawable.ic_zynx_skip_previous), contentDescription = null)
                    }
                    IconButton(onClick = { MusicPlayerManager.toggle(context) }) {
                        Icon(
                            painterResource(
                                if (MusicPlayerManager.isPlaying) R.drawable.ic_zynx_pause
                                else R.drawable.ic_play_arrow_filled
                            ),
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = { MusicPlayerManager.next(context) }) {
                        Icon(painterResource(R.drawable.ic_zynx_skip_next), contentDescription = null)
                    }
                }
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
                if (tracks.isEmpty()) {
                    Text(
                        text = stringResource(R.string.zynx_music_empty),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        itemsIndexed(tracks) { index, track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { MusicPlayerManager.play(context, index) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (index == current) R.drawable.ic_play_arrow_filled
                                        else R.drawable.ic_audio_file_filled
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = track.title,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                IconButton(
                                    onClick = { MusicPlayerManager.removeTrack(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_close),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { picker.launch(arrayOf("audio/*")) }) {
                Text(stringResource(R.string.zynx_music_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.zynx_music_close)) }
        }
    )
}
