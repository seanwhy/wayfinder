// Copyright 2026 The Wayfinder Authors
// SPDX-License-Identifier: Apache-2.0

package com.droid.wayfinder.data

import androidx.annotation.StringRes
import com.droid.wayfinder.R

enum class ViewMode { Details, List, Grid, Columns }

enum class SortKey { Name, Modified, Type, Size, Tags, Permissions }

enum class ThemeMode(val label: String, @StringRes val labelRes: Int) {
    System("System", R.string.theme_system),
    Light("Light", R.string.theme_light),
    Dark("Dark", R.string.theme_dark),
}

/** Google Drive account discovered on the device. */
data class DriveAccount(
    val email: String,
    val title: String = "Drive",
    val mountPath: String? = null,
    val isMounted: Boolean = mountPath != null,
)

enum class PhoneTransport {
    QUICK_ACCESS,
    USB_MTP,
    STORAGE_VOLUME,
    BLUETOOTH_UNMOUNTED,
}

/** Paired smartphone (e.g. Pixel 9) discovered on the device. */
data class PairedPhone(
    val name: String = "Pixel 9",
    val subtitle: String = "Internal Storage",
    val path: String = FsPath.PHONE_ROOT,
    val isConnected: Boolean = true,
    val authority: String? = null,
    val isMounted: Boolean = path != FsPath.PHONE_ROOT,
    val transport: PhoneTransport = PhoneTransport.QUICK_ACCESS,
)

/** Finder-style color labels; [argb] is the dot color. */
enum class ColorTag(val label: String, val argb: Long, @StringRes val labelRes: Int) {
    Urgent("Urgent", 0xFFD93025, R.string.tag_urgent),
    Review("Review", 0xFFE8710A, R.string.tag_review),
    WIP("WIP", 0xFFF9AB00, R.string.tag_wip),
    Approved("Approved", 0xFF1E8E3E, R.string.tag_approved),
    Spec("Spec", 0xFF1A73E8, R.string.tag_spec),
    Build("Build", 0xFF9334E6, R.string.tag_build),
    Archive("Archive", 0xFF80868B, R.string.tag_archive);

    companion object {
        fun fromName(n: String): ColorTag? = entries.firstOrNull { it.name.equals(n, ignoreCase = true) }
    }
}

enum class FileKind(val label: String, @StringRes val labelRes: Int) {
    Folder("Folder", R.string.kind_folder),
    Image("Image", R.string.kind_image),
    Video("Video", R.string.kind_video),
    Audio("Audio", R.string.kind_audio),
    Pdf("PDF document", R.string.kind_pdf),
    Document("Document", R.string.kind_document),
    Sheet("Spreadsheet", R.string.kind_sheet),
    Slides("Presentation", R.string.kind_slides),
    Archive("Archive", R.string.kind_archive),
    Code("Source code", R.string.kind_code),
    Apk("Android app", R.string.kind_apk),
    Text("Text file", R.string.kind_text),
    Other("File", R.string.kind_other);

    companion object {
        private val map: Map<String, FileKind> = buildMap {
            fun add(kind: FileKind, vararg exts: String) = exts.forEach { put(it, kind) }
            add(Image, "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "avif", "svg")
            add(Video, "mp4", "mkv", "mov", "avi", "webm", "3gp", "m4v", "ts")
            add(Audio, "mp3", "wav", "flac", "ogg", "m4a", "aac", "opus", "amr")
            add(Pdf, "pdf")
            add(Document, "doc", "docx", "odt", "rtf", "pages")
            add(Sheet, "xls", "xlsx", "ods", "csv", "numbers")
            add(Slides, "ppt", "pptx", "odp", "key")
            add(Archive, "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz")
            add(Code, "kt", "java", "py", "js", "html", "css", "json", "xml", "c", "cpp", "h", "rs", "go", "sh", "gradle", "kts", "yml", "yaml", "md")
            add(Apk, "apk", "aab", "xapk")
            add(Text, "txt", "log", "ini", "conf")
        }

        fun fromName(name: String): FileKind {
            val ext = name.substringAfterLast('.', "").lowercase()
            return map[ext] ?: Other
        }
    }
}

/** A single item in a folder listing (local file, SMB entry, SMB share/server, ...). */
data class FileEntry(
    val path: String,
    val name: String,
    val isDir: Boolean,
    val size: Long = 0L,
    val modified: Long = 0L,
    val isHidden: Boolean = name.startsWith("."),
    val isServer: Boolean = false,
    val isShare: Boolean = false,
    val subtitle: String? = null,
    /** POSIX st_mode for local files (0 = unknown). */
    val mode: Int = 0,
    /** True for symbolic links (local files): recursive operations never descend into linked folders. */
    val isLink: Boolean = false,
) {
    val kind: FileKind get() = if (isDir) FileKind.Folder else FileKind.fromName(name)
    val extension: String get() = if (isDir) "" else name.substringAfterLast('.', "")
    val typeLabel: String
        get() = when {
            isServer -> "Server"
            isShare -> "Network share"
            isDir -> "Folder"
            extension.isNotEmpty() && kind == FileKind.Other -> "${extension.uppercase()} file"
            else -> kind.label
        }
}
