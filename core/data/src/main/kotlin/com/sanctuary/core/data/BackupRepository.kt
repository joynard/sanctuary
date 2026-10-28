package com.sanctuary.core.data

import android.net.Uri
import java.time.LocalDate

interface BackupRepository {
    fun suggestedFileName(date: LocalDate): String

    suspend fun export(uri: Uri): Result<Unit>
}
