package com.gaojiluyin.data.remote.claude

interface ClaudeApi {
    suspend fun organizeTranscript(transcript: String): Result<OrganizedDocument>
}
