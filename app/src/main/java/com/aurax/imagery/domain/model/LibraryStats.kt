package com.aurax.imagery.domain.model

/** Totals over the indexed library (what AuraX can currently see). */
data class LibraryStats(
    val total: Int = 0,
    val photos: Int = 0,
    val videos: Int = 0,
    val bytes: Long = 0,
)
