// Copyright 2026 The Wayfinder Authors
// SPDX-License-Identifier: Apache-2.0

package com.droid.wayfinder.ui

import android.content.Context
import android.icu.text.MessageFormat
import android.icu.text.NumberFormat
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import java.util.Locale

/**
 * Formats an ICU [MessageFormat] [pattern] (such as `{0, plural, =1 {# item} other {# items}}`)
 * using [locale]-appropriate plural rules and number grouping.
 */
fun formatIcu(pattern: String, vararg args: Any, locale: Locale = Locale.getDefault()): String =
    MessageFormat(pattern, locale).format(args)

/**
 * Formats an integer [value] with [locale]-aware grouping separators (for example `1,024` in `en-US`).
 */
fun formatLocalizedNumber(value: Long, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getIntegerInstance(locale).format(value)

/**
 * Returns the active [Locale] from this [Context]'s configuration, falling back to [Locale.getDefault].
 */
fun Context.activeLocale(): Locale =
    resources.configuration.locales.takeIf { !it.isEmpty }?.get(0) ?: Locale.getDefault()

/**
 * Resolves the Android string resource [id] from this [Context] and evaluates its ICU [MessageFormat]
 * pattern against [activeLocale].
 */
fun Context.icuString(@StringRes id: Int, vararg args: Any): String =
    formatIcu(getString(id), *args, locale = activeLocale())

/**
 * Resolves the Android string resource [id] for the current Compose configuration and evaluates
 * its ICU [MessageFormat] pattern against the active [Locale].
 */
@Composable
@ReadOnlyComposable
fun icuStringResource(@StringRes id: Int, vararg args: Any): String {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales.takeIf { !it.isEmpty }?.get(0) ?: Locale.getDefault()
    return formatIcu(stringResource(id), *args, locale = locale)
}

private val gbFreeRegex = Regex("""^(\d+) GB free$""")

@Composable
@ReadOnlyComposable
fun com.droid.wayfinder.data.FileEntry.localizedTypeLabel(): String = when {
    isServer -> stringResource(com.droid.wayfinder.R.string.type_server)
    isShare -> stringResource(com.droid.wayfinder.R.string.type_network_share)
    isDir -> stringResource(com.droid.wayfinder.R.string.kind_folder)
    extension.isNotEmpty() && kind == com.droid.wayfinder.data.FileKind.Other ->
        stringResource(com.droid.wayfinder.R.string.type_ext_file, extension.uppercase())
    else -> stringResource(kind.labelRes)
}

@Composable
@ReadOnlyComposable
fun localizeRootLabel(label: String): String = when (label) {
    "Internal storage", "Internal Storage" -> stringResource(com.droid.wayfinder.R.string.sidebar_internal_storage)
    "Linux files" -> stringResource(com.droid.wayfinder.R.string.sidebar_linux_files)
    else -> label
}

@Composable
@ReadOnlyComposable
fun localizePhoneName(name: String): String = when (name) {
    "Phone" -> stringResource(com.droid.wayfinder.R.string.sidebar_phone)
    "Connected Phone" -> stringResource(com.droid.wayfinder.R.string.phone_connected_default)
    "USB Phone" -> stringResource(com.droid.wayfinder.R.string.phone_usb_default)
    else -> name
}

@Composable
@ReadOnlyComposable
fun localizeSubtitle(subtitle: String?): String? {
    if (subtitle == null) return null
    gbFreeRegex.matchEntire(subtitle)?.let { m ->
        return stringResource(com.droid.wayfinder.R.string.sidebar_gb_free, m.groupValues[1])
    }
    return when (subtitle) {
        "Internal storage", "Internal Storage" -> stringResource(com.droid.wayfinder.R.string.sidebar_internal_storage)
        "USB file transfer" -> stringResource(com.droid.wayfinder.R.string.phone_subtitle_usb_transfer)
        "USB · Tap to connect storage" -> stringResource(com.droid.wayfinder.R.string.phone_subtitle_usb_tap_connect)
        "Tap to connect phone storage" -> stringResource(com.droid.wayfinder.R.string.phone_subtitle_bt_tap_connect)
        "Cross-Device · Browse or import" -> stringResource(com.droid.wayfinder.R.string.phone_subtitle_cross_device)
        "Cloud / storage provider" -> stringResource(com.droid.wayfinder.R.string.mount_subtitle_cloud_provider)
        "Quick Share" -> stringResource(com.droid.wayfinder.R.string.phone_quick_share)
        else -> subtitle
    }
}

@Composable
@ReadOnlyComposable
fun AppState.localizedRootLabels(): Map<String, String> {
    val driveLabel = stringResource(com.droid.wayfinder.R.string.sidebar_drive)
    val phoneDefault = stringResource(com.droid.wayfinder.R.string.sidebar_phone)
    val linuxLabel = stringResource(com.droid.wayfinder.R.string.sidebar_linux_files)
    val virtualLabels = mapOf(
        com.droid.wayfinder.data.FsPath.SMB_ROOT to stringResource(com.droid.wayfinder.R.string.nav_network),
        com.droid.wayfinder.data.FsPath.TRASH_ROOT to stringResource(com.droid.wayfinder.R.string.nav_trash),
        com.droid.wayfinder.data.FsPath.RECENTS_ROOT to stringResource(com.droid.wayfinder.R.string.nav_recents),
        com.droid.wayfinder.data.FsPath.STARRED_ROOT to stringResource(com.droid.wayfinder.R.string.nav_starred),
        com.droid.wayfinder.data.FsPath.PHONE_ROOT to (pairedPhone?.let { localizePhoneName(it.name) } ?: phoneDefault),
    ) + com.droid.wayfinder.data.ColorTag.entries.associate {
        com.droid.wayfinder.data.FsPath.tagPath(it) to stringResource(it.labelRes)
    }
    return virtualLabels +
        roots.associate { com.droid.wayfinder.data.FsPath.normalize(it.path) to localizeRootLabel(it.label) } +
        mounts.associate { com.droid.wayfinder.data.FsPath.normalize(it.path) to it.label } +
        driveAccounts.mapNotNull { acc ->
            acc.mountPath?.let { com.droid.wayfinder.data.FsPath.normalize(it) to "$driveLabel (${acc.email})" }
        }.toMap() +
        pairedPhone?.takeIf { it.isMounted && it.path != com.droid.wayfinder.data.FsPath.PHONE_ROOT }
            ?.let {
                val pName = localizePhoneName(it.name)
                mapOf(
                    com.droid.wayfinder.data.FsPath.normalize(it.path) to pName,
                    com.droid.wayfinder.data.FsPath.PHONE_ROOT to pName,
                )
            }.orEmpty() +
        linuxRootPath?.let { mapOf(com.droid.wayfinder.data.FsPath.normalize(it) to linuxLabel) }.orEmpty()
}

@Composable
@ReadOnlyComposable
fun AppState.localizedTitleOf(path: String): String {
    val labels = localizedRootLabels()
    return when {
        com.droid.wayfinder.data.FsPath.isVirtual(path) -> labels[path] ?: com.droid.wayfinder.data.FsPath.name(path)
        else -> labels[com.droid.wayfinder.data.FsPath.normalize(path)] ?: com.droid.wayfinder.data.FsPath.name(path)
    }
}

@Composable
@ReadOnlyComposable
fun AppState.localizedBreadcrumbs(path: String): List<Pair<String, String>> =
    com.droid.wayfinder.data.FsPath.breadcrumbs(path, localizedRootLabels())

@Composable
@ReadOnlyComposable
fun AppState.localizedDisplayPath(path: String, separator: String = "/"): String =
    com.droid.wayfinder.data.FsPath.friendlyPath(path, localizedRootLabels(), separator)

fun Context.localizedNameError(name: String): String? = when (val err = com.droid.wayfinder.data.FsPath.nameError(name)) {
    null -> null
    "Name can't be empty" -> getString(com.droid.wayfinder.R.string.error_name_empty)
    "Invalid name" -> getString(com.droid.wayfinder.R.string.error_name_invalid)
    "Names can't contain slashes" -> getString(com.droid.wayfinder.R.string.error_name_slashes)
    "Names can't contain control characters" -> getString(com.droid.wayfinder.R.string.error_name_control_chars)
    "Name is too long" -> getString(com.droid.wayfinder.R.string.error_name_too_long)
    else -> err
}

@Composable
@ReadOnlyComposable
fun localizedNameError(name: String): String? = when (val err = com.droid.wayfinder.data.FsPath.nameError(name)) {
    null -> null
    "Name can't be empty" -> stringResource(com.droid.wayfinder.R.string.error_name_empty)
    "Invalid name" -> stringResource(com.droid.wayfinder.R.string.error_name_invalid)
    "Names can't contain slashes" -> stringResource(com.droid.wayfinder.R.string.error_name_slashes)
    "Names can't contain control characters" -> stringResource(com.droid.wayfinder.R.string.error_name_control_chars)
    "Name is too long" -> stringResource(com.droid.wayfinder.R.string.error_name_too_long)
    else -> err
}

@Composable
@ReadOnlyComposable
fun localizeRenameConflict(conflict: String): String = when (conflict) {
    "Duplicate name" -> stringResource(com.droid.wayfinder.R.string.batch_rename_conflict_duplicate)
    "Already exists" -> stringResource(com.droid.wayfinder.R.string.batch_rename_conflict_exists)
    "Name can't be empty" -> stringResource(com.droid.wayfinder.R.string.error_name_empty)
    "Invalid name" -> stringResource(com.droid.wayfinder.R.string.error_name_invalid)
    "Names can't contain slashes" -> stringResource(com.droid.wayfinder.R.string.error_name_slashes)
    "Names can't contain control characters" -> stringResource(com.droid.wayfinder.R.string.error_name_control_chars)
    "Name is too long" -> stringResource(com.droid.wayfinder.R.string.error_name_too_long)
    else -> conflict
}
