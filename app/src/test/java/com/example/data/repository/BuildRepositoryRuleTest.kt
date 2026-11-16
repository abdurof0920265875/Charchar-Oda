package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ProjectCategory
import com.example.data.model.ProjectStatus
import com.example.data.model.StepPhase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createProjectAndSubcollections_authenticatedOwner_succeedsAndStreams() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = BuildRepository(firestore, auth)

    val projectId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createProject(
          title = "Workshop Workbench",
          category = ProjectCategory.FURNITURE,
          location = "Garage",
          status = ProjectStatus.PLANNING,
          budgetCents = 45000L,
          notes = "Maple butcher block top",
        )
      }
    assertTrue(projectId.isNotBlank())

    val projects =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeProjects(uid).first { list -> list.any { it.id == projectId } }
      }
    assertEquals("Workshop Workbench", projects.first { it.id == projectId }.title)

    val stepId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.addStep(projectId, "Mill apron rails", StepPhase.BUILD, 0)
      }
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.toggleStepCompleted(projectId, stepId, true)
    }

    val steps =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeSteps(uid, projectId).first { list -> list.any { it.id == stepId && it.completed } }
      }
    assertTrue(steps.first { it.id == stepId }.completed)

    val materialId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.addMaterial(projectId, "Maple Top", 1.0, "slab", 22000L)
      }
    val materials =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeMaterials(uid, projectId).first { list -> list.any { it.id == materialId } }
      }
    assertEquals(22000L, materials.first { it.id == materialId }.totalCostCents)

    val measurementId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.addMeasurement(projectId, "Bench Height", "36 in", "Matches table saw")
      }
    val measurements =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeMeasurements(uid, projectId).first { list -> list.any { it.id == measurementId } }
      }
    assertEquals("36 in", measurements.first { it.id == measurementId }.dimensionValue)
  }

  @Test
  fun observeProjects_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = BuildRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createProject(
        title = "Alice Kitchen Island",
        category = ProjectCategory.RENOVATION,
        location = "Kitchen",
        status = ProjectStatus.IN_PROGRESS,
        budgetCents = 120000L,
        notes = "Quartz counter",
      )
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = BuildRepository(firestore, auth)
    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        bobRepo.observeProjects(aliceUid).first()
      }
    } catch (e: Exception) {
      caught = e
    }
    val firestoreException = checkNotNull(caught?.unwrapFirestoreException()) {
      "Expected nested FirebaseFirestoreException, got $caught"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
  }

  @Test
  fun observeProjects_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = BuildRepository(firestore, auth)

    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeProjects("some_uid").first()
      }
    } catch (e: Exception) {
      caught = e
    }
    val firestoreException = checkNotNull(caught?.unwrapFirestoreException()) {
      "Expected nested FirebaseFirestoreException, got $caught"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_builder@test.com"
    const val BOB_EMAIL = "bob_builder@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
