package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ProjectCategory(val wireValue: String, val label: String) {
  RENOVATION("renovation", "Renovation"),
  FURNITURE("furniture", "Furniture"),
  OUTDOOR("outdoor", "Outdoor"),
  SYSTEMS("systems", "Systems"),
  CUSTOM("custom", "Custom Build");

  companion object {
    fun fromWire(value: String): ProjectCategory =
      entries.firstOrNull { it.wireValue == value } ?: CUSTOM
  }
}

enum class ProjectStatus(val wireValue: String, val label: String) {
  PLANNING("planning", "Planning"),
  IN_PROGRESS("in_progress", "In Progress"),
  FINISHING("finishing", "Finishing"),
  COMPLETED("completed", "Completed");

  companion object {
    fun fromWire(value: String): ProjectStatus =
      entries.firstOrNull { it.wireValue == value } ?: PLANNING
  }
}

enum class StepPhase(val wireValue: String, val label: String) {
  PREP("prep", "Prep"),
  MATERIALS("materials", "Materials"),
  BUILD("build", "Build"),
  FINISH("finish", "Finish");

  companion object {
    fun fromWire(value: String): StepPhase =
      entries.firstOrNull { it.wireValue == value } ?: BUILD
  }
}

@IgnoreExtraProperties
data class BuildProject(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val category: String = ProjectCategory.RENOVATION.wireValue,
  val location: String = "",
  val status: String = ProjectStatus.PLANNING.wireValue,
  val budgetCents: Long = 0L,
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val categoryEnum: ProjectCategory
    get() = ProjectCategory.fromWire(category)

  val statusEnum: ProjectStatus
    get() = ProjectStatus.fromWire(status)
}

@IgnoreExtraProperties
data class BuildStep(
  @DocumentId val id: String = "",
  val userId: String = "",
  val projectId: String = "",
  val title: String = "",
  val phase: String = StepPhase.BUILD.wireValue,
  val completed: Boolean = false,
  val sortOrder: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val phaseEnum: StepPhase
    get() = StepPhase.fromWire(phase)
}

@IgnoreExtraProperties
data class BuildMaterial(
  @DocumentId val id: String = "",
  val userId: String = "",
  val projectId: String = "",
  val name: String = "",
  val quantity: Double = 1.0,
  val unit: String = "pcs",
  val unitCostCents: Long = 0L,
  val purchased: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val totalCostCents: Long
    get() = Math.round(quantity * unitCostCents)
}

@IgnoreExtraProperties
data class SiteMeasurement(
  @DocumentId val id: String = "",
  val userId: String = "",
  val projectId: String = "",
  val label: String = "",
  val dimensionValue: String = "",
  val note: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
