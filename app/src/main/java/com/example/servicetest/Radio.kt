package com.example.servicetest

data class Radio(
    val name: String,
    val logo: String,
    val streamURL: String,
    var isSelected: Boolean = false,
    var isLoading: Boolean = false,
    var isPlaying: Boolean = false,
): Comparable<Radio> {
    override fun compareTo(other: Radio): Int {
        return name.compareTo(other.name)
    }
}