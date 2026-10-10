package com.rizkysiregar.feedzmore.domain.post

import androidx.work.WorkInfo
import java.util.UUID

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: UI-facing snapshot of an in-progress background upload.
 */
data class PendingUpload(
	val id: UUID,
	val caption: String,
	val stageLabel: String,
	val progress: Int,
	val state: WorkInfo.State
)
