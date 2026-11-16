package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BuildMaterial
import com.example.data.model.BuildProject
import com.example.data.model.BuildStep
import com.example.data.model.ProjectCategory
import com.example.data.model.ProjectStatus
import com.example.data.model.SiteMeasurement
import com.example.data.model.StepPhase
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BuildRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun projectsCollection(uid: String) =
    db.collection("users").document(uid).collection("projects")

  private fun projectDoc(uid: String, projectId: String) =
    projectsCollection(uid).document(projectId)

  private fun stepsCollection(uid: String, projectId: String) =
    projectDoc(uid, projectId).collection("steps")

  private fun materialsCollection(uid: String, projectId: String) =
    projectDoc(uid, projectId).collection("materials")

  private fun measurementsCollection(uid: String, projectId: String) =
    projectDoc(uid, projectId).collection("measurements")

  private fun logAndRethrowIfSignedIn(
    error: Throwable,
    op: OperationType,
    path: String,
    uid: String?,
  ): Nothing {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && uid != null && auth.currentUser?.uid == uid) {
      handleFirestoreError(exceptionToLog, op, path, auth)
    }
    throw error
  }

  fun observeProjects(userId: String): Flow<List<BuildProject>> = flow {
    val path = "users/$userId/projects"
    emitAll(
      projectsCollection(userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(BuildProject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logAndRethrowIfSignedIn(error, OperationType.LIST, path, userId)
        }
    )
  }

  fun observeProject(userId: String, projectId: String): Flow<BuildProject?> = flow {
    val docRef = projectDoc(userId, projectId)
    emitAll(
      docRef
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(BuildProject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } else {
            null
          }
        }
        .catch { error ->
          logAndRethrowIfSignedIn(error, OperationType.GET, docRef.path, userId)
        }
    )
  }

  fun observeSteps(userId: String, projectId: String): Flow<List<BuildStep>> = flow {
    val path = "users/$userId/projects/$projectId/steps"
    emitAll(
      stepsCollection(userId, projectId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(BuildStep::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedWith(compareBy<BuildStep> { it.completed }.thenBy { it.sortOrder }.thenBy { it.createdAt ?: Timestamp(0, 0) })
        }
        .catch { error ->
          logAndRethrowIfSignedIn(error, OperationType.LIST, path, userId)
        }
    )
  }

  fun observeMaterials(userId: String, projectId: String): Flow<List<BuildMaterial>> = flow {
    val path = "users/$userId/projects/$projectId/materials"
    emitAll(
      materialsCollection(userId, projectId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(BuildMaterial::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedWith(compareBy<BuildMaterial> { it.purchased }.thenByDescending { it.createdAt ?: Timestamp(0, 0) })
        }
        .catch { error ->
          logAndRethrowIfSignedIn(error, OperationType.LIST, path, userId)
        }
    )
  }

  fun observeMeasurements(userId: String, projectId: String): Flow<List<SiteMeasurement>> = flow {
    val path = "users/$userId/projects/$projectId/measurements"
    emitAll(
      measurementsCollection(userId, projectId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(SiteMeasurement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logAndRethrowIfSignedIn(error, OperationType.LIST, path, userId)
        }
    )
  }

  suspend fun createProject(
    title: String,
    category: ProjectCategory,
    location: String,
    status: ProjectStatus,
    budgetCents: Long,
    notes: String,
  ): String {
    val uid = requireUserId()
    val docRef = projectsCollection(uid).document()
    val payload =
      mapOf(
        "userId" to uid,
        "title" to title.trim().take(100),
        "category" to category.wireValue,
        "location" to location.trim().take(80),
        "status" to status.wireValue,
        "budgetCents" to budgetCents.coerceAtLeast(0L),
        "notes" to notes.trim().take(1000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun updateProject(
    projectId: String,
    title: String,
    category: ProjectCategory,
    location: String,
    status: ProjectStatus,
    budgetCents: Long,
    notes: String,
  ) {
    val uid = requireUserId()
    require(projectId.isNotBlank()) { "Project ID must not be blank" }
    val docRef = projectDoc(uid, projectId)
    val updates =
      mapOf(
        "title" to title.trim().take(100),
        "category" to category.wireValue,
        "location" to location.trim().take(80),
        "status" to status.wireValue,
        "budgetCents" to budgetCents.coerceAtLeast(0L),
        "notes" to notes.trim().take(1000),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun updateProjectStatus(projectId: String, status: ProjectStatus) {
    val uid = requireUserId()
    require(projectId.isNotBlank()) { "Project ID must not be blank" }
    val docRef = projectDoc(uid, projectId)
    try {
      docRef
        .update(
          mapOf(
            "status" to status.wireValue,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun deleteProject(projectId: String) {
    val uid = requireUserId()
    require(projectId.isNotBlank()) { "Project ID must not be blank" }
    val docRef = projectDoc(uid, projectId)
    try {
      val steps = stepsCollection(uid, projectId).get().await()
      val materials = materialsCollection(uid, projectId).get().await()
      val measurements = measurementsCollection(uid, projectId).get().await()
      val batch = db.batch()
      steps.documents.forEach { batch.delete(it.reference) }
      materials.documents.forEach { batch.delete(it.reference) }
      measurements.documents.forEach { batch.delete(it.reference) }
      batch.delete(docRef)
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      throw e
    }
  }

  suspend fun addStep(
    projectId: String,
    title: String,
    phase: StepPhase,
    sortOrder: Long,
  ): String {
    val uid = requireUserId()
    require(projectId.isNotBlank()) { "Project ID must not be blank" }
    val docRef = stepsCollection(uid, projectId).document()
    val payload =
      mapOf(
        "userId" to uid,
        "projectId" to projectId,
        "title" to title.trim().take(120),
        "phase" to phase.wireValue,
        "completed" to false,
        "sortOrder" to sortOrder.coerceIn(0L, 100000L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun toggleStepCompleted(projectId: String, stepId: String, completed: Boolean) {
    val uid = requireUserId()
    val docRef = stepsCollection(uid, projectId).document(stepId)
    try {
      docRef
        .update(
          mapOf(
            "completed" to completed,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun deleteStep(projectId: String, stepId: String) {
    val uid = requireUserId()
    val docRef = stepsCollection(uid, projectId).document(stepId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      throw e
    }
  }

  suspend fun addMaterial(
    projectId: String,
    name: String,
    quantity: Double,
    unit: String,
    unitCostCents: Long,
  ): String {
    val uid = requireUserId()
    require(projectId.isNotBlank()) { "Project ID must not be blank" }
    val docRef = materialsCollection(uid, projectId).document()
    val payload =
      mapOf(
        "userId" to uid,
        "projectId" to projectId,
        "name" to name.trim().take(100),
        "quantity" to quantity.coerceIn(0.01, 1000000.0),
        "unit" to unit.trim().ifEmpty { "pcs" }.take(24),
        "unitCostCents" to unitCostCents.coerceAtLeast(0L),
        "purchased" to false,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun toggleMaterialPurchased(projectId: String, materialId: String, purchased: Boolean) {
    val uid = requireUserId()
    val docRef = materialsCollection(uid, projectId).document(materialId)
    try {
      docRef
        .update(
          mapOf(
            "purchased" to purchased,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun deleteMaterial(projectId: String, materialId: String) {
    val uid = requireUserId()
    val docRef = materialsCollection(uid, projectId).document(materialId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      throw e
    }
  }

  suspend fun addMeasurement(
    projectId: String,
    label: String,
    dimensionValue: String,
    note: String,
  ): String {
    val uid = requireUserId()
    require(projectId.isNotBlank()) { "Project ID must not be blank" }
    val docRef = measurementsCollection(uid, projectId).document()
    val payload =
      mapOf(
        "userId" to uid,
        "projectId" to projectId,
        "label" to label.trim().take(80),
        "dimensionValue" to dimensionValue.trim().take(40),
        "note" to note.trim().take(300),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun deleteMeasurement(projectId: String, measurementId: String) {
    val uid = requireUserId()
    val docRef = measurementsCollection(uid, projectId).document(measurementId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      throw e
    }
  }

  suspend fun seedStarterProjectIfEmpty() {
    val uid = requireUserId()
    val existingSnapshot = projectsCollection(uid).limit(1).get().await()
    if (!existingSnapshot.isEmpty) return

    val projectId =
      createProject(
        title = "Cedar Patio Bench",
        category = ProjectCategory.OUTDOOR,
        location = "Backyard Terrace",
        status = ProjectStatus.IN_PROGRESS,
        budgetCents = 24000L,
        notes = "Weather-resistant cedar slat bench with concealed pocket-screw joinery and matte spar urethane finish.",
      )
    addStep(projectId, "Measure patio alcove and finalize cut list", StepPhase.PREP, 0)
    addStep(projectId, "Pick up cedar 2x4s and exterior pocket screws", StepPhase.MATERIALS, 1)
    addStep(projectId, "Assemble leg frames and seat slats", StepPhase.BUILD, 2)
    addStep(projectId, "Sand to 180 grit and apply two coats of spar urethane", StepPhase.FINISH, 3)

    addMaterial(projectId, "Western Red Cedar 2x4 (8ft)", 6.0, "pcs", 1650L)
    addMaterial(projectId, "Exterior Pocket Screws (2.5 in)", 1.0, "box", 1800L)
    addMaterial(projectId, "Matte Spar Urethane", 1.0, "qt", 2600L)

    addMeasurement(projectId, "Bench Overall Length", "60 in", "Fits neatly along railing")
    addMeasurement(projectId, "Seat Height", "17.5 in", "Standard outdoor dining clearance")
  }
}
