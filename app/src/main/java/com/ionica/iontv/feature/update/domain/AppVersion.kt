package com.ionica.iontv.feature.update.domain

private val VERSION = Regex("""\d+(\.\d+)*""")

fun isNewerVersion(candidate: String, installed: String): Boolean {
    val latest = candidate.versionParts() ?: return false
    val current = installed.versionParts() ?: return false
    for (index in 0 until maxOf(latest.size, current.size)) {
        val difference = latest.getOrElse(index) { 0 } - current.getOrElse(index) { 0 }
        if (difference != 0) return difference > 0
    }
    return false
}

private fun String.versionParts(): List<Int>? {
    val version = trim()
    if (!VERSION.matches(version)) return null
    return version.split('.').map { it.toIntOrNull() ?: return null }
}
