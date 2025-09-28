package com.example.aichat.llm.tools

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * A simple tool that returns the current time in multiple formats.
 */
object TimeTool {
    const val NAME = "time"

    fun getTime(): Map<String, Any> {
        val now = ZonedDateTime.now()
        val iso = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val pretty = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"))
        return mapOf(
            "epoch_ms" to Instant.now().toEpochMilli(),
            "iso_8601" to iso,
            "timezone_id" to ZoneId.systemDefault().id,
            "pretty" to pretty
        )
    }
}