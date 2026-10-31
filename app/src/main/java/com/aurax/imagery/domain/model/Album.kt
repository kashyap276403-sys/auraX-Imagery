package com.aurax.imagery.domain.model

data class Album(
    val id: Long,
    val name: String,
    /** URI of the most recent item, used as the album cover. */
    val coverUri: String?,
    val itemCount: Int,
)
