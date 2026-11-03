package com.aurax.imagery.domain.edit

/** File format of the saved copy. [Auto] keeps PNG for PNG sources and uses JPEG for everything else. */
enum class ExportFormat(val label: String, val mime: String, val extension: String) {
    Auto("Same as original", "", ""),
    Jpeg("JPEG", "image/jpeg", "jpg"),
    Png("PNG", "image/png", "png"),
    Webp("WebP", "image/webp", "webp");

    /** Resolves [Auto] against the source file's mime type. */
    fun resolve(sourceMime: String?): ExportFormat =
        if (this != Auto) this else if (sourceMime == "image/png") Png else Jpeg
}
