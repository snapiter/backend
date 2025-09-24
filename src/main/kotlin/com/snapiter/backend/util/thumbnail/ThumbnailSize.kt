package com.snapiter.backend.util.thumbnail

enum class ThumbnailSize(val width: Int, val height: Int, val value: String) {
    SMALL(100, 100, "100x100"),
    MEDIUM(500, 500, "500x500"),
    LARGE(1000, 1000, "1000x1000");

    companion object {
        fun fromValue(value: String): ThumbnailSize? =
            entries.find { it.value == value }
    }
}
