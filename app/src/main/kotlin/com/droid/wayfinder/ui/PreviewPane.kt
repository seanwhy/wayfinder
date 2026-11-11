// Copyright 2026 The Wayfinder Authors
// SPDX-License-Identifier: Apache-2.0

package com.droid.wayfinder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import com.droid.wayfinder.R
import com.droid.wayfinder.data.ColorTag
import com.droid.wayfinder.data.FileEntry
import com.droid.wayfinder.data.FileInfo
import com.droid.wayfinder.data.FileKind
import com.droid.wayfinder.data.FsPath
import com.droid.wayfinder.data.formatSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PreviewPane(vm: BrowserViewModel, s: AppState, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val tab = s.active
    val sel = tab.selectedEntries(s.showHidden)
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = cs.surfaceContainerLowest) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.details_pane_title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                IconButton({ vm.togglePreview() }, Modifier.minimumInteractiveComponentSize().sizeIn(minWidth = 40.dp, minHeight = 40.dp)) { Icon(Icons.Outlined.Close, stringResource(R.string.preview_close), Modifier.size(18.dp)) }
            }
            when (sel.size) {
                0 -> {
                    Preview(null, FsPath.name(tab.path).ifEmpty { "/" }, stringResource(R.string.kind_folder))
                    Fact(stringResource(R.string.col_items), tab.entries.size.toString())
                    Fact(stringResource(R.string.preview_location), s.localizedTitleOf(tab.path))
                }
                1 -> {
                    val e = sel[0]
                    Preview(null, e.name, stringResource(e.kind.labelRes), e, vm)
                    Fact(stringResource(R.string.col_type), e.localizedTypeLabel())
                    e.subtitle?.takeIf { it.isNotBlank() }?.let { sub ->
                        localizeSubtitle(sub)?.let { Fact(stringResource(R.string.preview_summary), it) }
                    }
                    if (!e.isDir) Fact(stringResource(R.string.col_size), formatSize(e.size))
                    Fact(stringResource(R.string.preview_modified), formatDateFull(e.modified))
                    Fact(stringResource(R.string.preview_location), s.localizedDisplayPath(FsPath.parent(e.path) ?: e.path, " / "))
                    if (FsPath.isLocalFile(e.path)) PosixFact(e)
                    if (!e.isServer && !e.isShare) MarksEditor(vm, s, e)
                    val btnPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    FlowRow(
                        Modifier.padding(top = 16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton({ vm.openSelection() }, contentPadding = btnPadding) { Text(stringResource(R.string.action_open), maxLines = 1, softWrap = false) }
                        if (FsPath.isOneDrive(e.path)) {
                            FilledTonalButton({ vm.openInOneDriveWeb(e.path) }, contentPadding = btnPadding) {
                                Text(stringResource(R.string.onedrive_action_open_web), maxLines = 1, softWrap = false)
                            }
                        }
                        if (FsPath.isLocalFile(e.path) && !e.isDir) FilledTonalButton({ vm.share() }, contentPadding = btnPadding) { Text(stringResource(R.string.action_share), maxLines = 1, softWrap = false) }
                        if (!e.isServer && !e.isShare && !FsPath.isZip(e.path) && !FsPath.isVirtual(e.path)) {
                            val pinned = e.path in s.pins
                            FilledTonalButton({ vm.togglePin(e.path) }, contentPadding = btnPadding) {
                                Text(stringResource(if (pinned) R.string.preview_unpin else R.string.preview_pin), maxLines = 1, softWrap = false)
                            }
                        }
                        if (!e.isServer && !e.isShare && !FsPath.isZip(e.path)) FilledTonalButton({ vm.requestRename() }, contentPadding = btnPadding) { Text(stringResource(R.string.action_rename), maxLines = 1, softWrap = false) }
                    }
                    if (!e.isDir && FsPath.isLocalFile(e.path) && (e.kind == FileKind.Text || e.kind == FileKind.Code)) TextSnippet(e)
                }
                else -> {
                    Preview(null, icuStringResource(R.string.selected_count_plural, sel.size), stringResource(R.string.preview_multiple))
                    Fact(stringResource(R.string.preview_total_size), formatSize(sel.filter { !it.isDir }.sumOf { it.size }))
                    Fact(stringResource(R.string.preview_folders), sel.count { it.isDir }.toString())
                    Fact(stringResource(R.string.preview_files), sel.count { !it.isDir }.toString())
                }
            }
        }
    }
}

@Composable
private fun Preview(
    imagePath: String?,
    name: String,
    kindLabel: String,
    entry: com.droid.wayfinder.data.FileEntry? = null,
    vm: BrowserViewModel? = null,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.padding(top = 12.dp).fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(16.dp)).background(cs.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        if (entry != null && vm != null && vm.canThumbnail(entry)) {
            // Local and network/cloud (SMB, NAS, WebDAV, FTP, SAF) images and videos.
            EntryThumbnail(vm, entry, sizePx = 720, iconSize = 64.dp, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else if (imagePath != null) {
            val ctx = LocalContext.current
            AsyncImage(
                remember(imagePath) { ImageRequest.Builder(ctx).data(File(imagePath)).size(720).build() },
                name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
            )
        } else {
            val k = entry?.kind ?: FileKind.Folder
            Icon(entry?.icon() ?: k.icon(), null, Modifier.size(64.dp), tint = entry?.tint() ?: k.tint())
        }
    }
    Text(name, Modifier.padding(top = 12.dp, bottom = 8.dp), style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
}

/** POSIX mode summary, e.g. `-rw-rw---- (0660) • uid 1000 / gid 1015`. */
@Composable
private fun PosixFact(e: FileEntry) {
    val info by produceState<com.droid.wayfinder.data.PosixInfo?>(null, e.path, e.modified) {
        value = withContext(Dispatchers.IO) { FileInfo.lstat(e.path) }
    }
    info?.let { p -> Fact(stringResource(R.string.preview_mode), "${FileInfo.posixString(p.mode)} (${FileInfo.octal(p.mode)})\nuid ${p.uid} / gid ${p.gid}", mono = true) }
}

/** Star, pin, and color-tag chips for one item. */
@Composable
private fun MarksEditor(vm: BrowserViewModel, s: AppState, e: FileEntry) {
    val cs = MaterialTheme.colorScheme
    val starred = s.marks.isStarred(e.path)
    val pinned = e.path in s.pins
    val tags = s.marks.tagsOf(e.path)
    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.col_tags), Modifier.widthIn(min = 84.dp).padding(end = 8.dp), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!FsPath.isVirtual(e.path) && !FsPath.isZip(e.path)) {
                val pinLabel = stringResource(if (pinned) R.string.action_unpin_from_sidebar else R.string.action_pin_to_sidebar)
                Box(
                    Modifier.sizeIn(minWidth = 32.dp, minHeight = 32.dp)
                        .semantics {
                            role = Role.Checkbox
                            this.selected = pinned
                        }
                        .clip(CircleShape)
                        .clickable(onClickLabel = pinLabel) { vm.togglePin(e.path) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (pinned) androidx.compose.material.icons.Icons.Filled.PushPin else androidx.compose.material.icons.Icons.Outlined.PushPin,
                        pinLabel,
                        Modifier.size(20.dp),
                        tint = if (pinned) cs.primary else cs.onSurfaceVariant,
                    )
                }
            }
            val starLabel = stringResource(if (starred) R.string.action_remove_from_starred else R.string.action_add_to_starred)
            Box(
                Modifier.sizeIn(minWidth = 32.dp, minHeight = 32.dp)
                    .semantics {
                        role = Role.Checkbox
                        this.selected = starred
                    }
                    .clip(CircleShape)
                    .clickable(onClickLabel = starLabel) { vm.toggleStar(listOf(e.path)) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (starred) Icons.Filled.Star else Icons.Outlined.StarOutline, starLabel,
                    Modifier.size(20.dp), tint = if (starred) StarColor else cs.onSurfaceVariant)
            }
            ColorTag.entries.forEach { t ->
                val on = t in tags
                val tagName = stringResource(t.labelRes)
                val tagCd = stringResource(R.string.preview_tag_cd, tagName)
                val tagClick = stringResource(if (on) R.string.preview_remove_tag else R.string.preview_add_tag, tagName)
                Box(
                    Modifier.sizeIn(minWidth = 32.dp, minHeight = 32.dp)
                        .semantics {
                            role = Role.Checkbox
                            this.selected = on
                            contentDescription = tagCd
                        }
                        .clip(CircleShape)
                        .background(Color(t.argb).copy(alpha = if (on) 1f else 0.25f))
                        .then(if (on) Modifier.border(2.dp, cs.onSurface.copy(alpha = 0.5f), CircleShape) else Modifier)
                        .clickable(onClickLabel = tagClick) { vm.toggleTag(t, listOf(e.path)) },
                    contentAlignment = Alignment.Center,
                ) { if (on) Icon(Icons.Filled.Check, tagName, Modifier.size(14.dp), tint = Color.White) }
            }
        }
    }
    if (tags.isNotEmpty()) Text(
        tags.sortedBy { it.ordinal }.map { stringResource(it.labelRes) }.joinToString(" • "), Modifier.padding(start = 84.dp, top = 4.dp),
        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
    )
}

/** First lines of a text/code file. */
@Composable
private fun TextSnippet(e: FileEntry) {
    val cs = MaterialTheme.colorScheme
    val text by produceState<String?>(null, e.path, e.modified) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                File(e.path).inputStream().use { inp ->
                    val buf = ByteArray(1200)
                    val n = inp.read(buf).coerceAtLeast(0)
                    String(buf, 0, n, Charsets.UTF_8).lines().take(14).joinToString("\n")
                }
            }.getOrNull()
        }
    }
    val t = text?.takeIf { it.isNotBlank() } ?: return
    Text(stringResource(R.string.preview_text_heading), Modifier.padding(top = 16.dp, bottom = 4.dp), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
    Surface(shape = RoundedCornerShape(10.dp), color = cs.surfaceContainerLow) {
        Text(t, Modifier.fillMaxWidth().padding(10.dp), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, maxLines = 14)
    }
}

@Composable
private fun Fact(label: String, value: String, mono: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, Modifier.widthIn(min = 84.dp).padding(end = 8.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontFamily = if (mono) FontFamily.Monospace else null)
    }
}
