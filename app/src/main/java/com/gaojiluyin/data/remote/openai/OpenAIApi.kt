package com.gaojiluyin.data.remote.openai

import com.gaojiluyin.data.remote.claude.OrganizedDocument

interface OpenAIApi {
    suspend fun organizeTranscript(transcript: String): Result<OrganizedDocument>
}
