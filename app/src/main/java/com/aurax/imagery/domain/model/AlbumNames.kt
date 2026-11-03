package com.aurax.imagery.domain.model

/** Folder names for albums the user creates, kept safe for MediaStore paths. */
object AlbumNames {
    private val forbidden = Regex("[\\\\/:*?\"<>|]")
    const val MAX_LENGTH = 40

    /** Removes characters a path cannot hold, collapses spaces, trims dots and spaces; empty if nothing is left. */
    fun clean(raw: String): String =
        raw.replace(forbidden, " ").filter { !it.isISOControl() }.replace(Regex("\\s+"), " ").trim().trim('.').trim().take(MAX_LENGTH).trim()

    /** MediaStore RELATIVE_PATH for a new album: photos under Pictures, videos under Movies. */
    fun relativePath(name: String, isVideo: Boolean): String = (if (isVideo) "Movies/" else "Pictures/") + name
}
