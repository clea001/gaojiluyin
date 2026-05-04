package com.gaojiluyin.data.remote.claude

data class OrganizedDocument(
    val title: String,
    val summary: String,
    val keyPoints: List<String>,
    val organizedContent: String,
    val tags: List<String>
)
