package com.example.ui.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BuildMaterial
import com.example.data.model.BuildProject
import com.example.data.model.BuildStep
import com.example.data.model.ProjectCategory
import com.example.data.model.ProjectStatus
import com.example.data.model.SiteMeasurement
import com.example.data.model.StepPhase
import com.example.data.repository.BuildRepository
import com.example.ui.state.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectDetailViewModel(
  private val repository: BuildRepository,
  private val currentUserId: String,
  private val projectId: String,
) : ViewModel() {

  private val _errorBanner = MutableStateFlow<String?>(null)
  val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

  val projectState: StateFlow<UiState<BuildProject?>> =
    repository
      .observeProject(currentUserId, projectId)
      .map<BuildProject?, UiState<BuildProject?>> { UiState.Success(it) }
      .catch { error ->
        val root = unwrapFirestoreOrSelf(error)
        if (root == null) throw error
        Log.w(TAG, "Error observing project $projectId", root)
        emit(UiState.Error(root.message ?: "Failed to load project"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val stepsState: StateFlow<UiState<List<BuildStep>>> =
    repository
      .observeSteps(currentUserId, projectId)
      .map<List<BuildStep>, UiState<List<BuildStep>>> { UiState.Success(it) }
      .catch { error ->
        val root = unwrapFirestoreOrSelf(error)
        if (root == null) throw error
        Log.w(TAG, "Error observing steps", root)
        emit(UiState.Error(root.message ?: "Failed to load steps"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val materialsState: StateFlow<UiState<List<BuildMaterial>>> =
    repository
      .observeMaterials(currentUserId, projectId)
      .map<List<BuildMaterial>, UiState<List<BuildMaterial>>> { UiState.Success(it) }
      .catch { error ->
        val root = unwrapFirestoreOrSelf(error)
        if (root == null) throw error
        Log.w(TAG, "Error observing materials", root)
        emit(UiState.Error(root.message ?: "Failed to load materials"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val measurementsState: StateFlow<UiState<List<SiteMeasurement>>> =
    repository
      .observeMeasurements(currentUserId, projectId)
      .map<List<SiteMeasurement>, UiState<List<SiteMeasurement>>> { UiState.Success(it) }
      .catch { error ->
        val root = unwrapFirestoreOrSelf(error)
        if (root == null) throw error
        Log.w(TAG, "Error observing measurements", root)
        emit(UiState.Error(root.message ?: "Failed to load measurements"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  private fun unwrapFirestoreOrSelf(error: Throwable): Throwable? {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) return null
    return firestoreError ?: error
  }

  fun clearErrorBanner() {
    _errorBanner.value = null
  }

  fun updateProjectStatus(status: ProjectStatus) {
    viewModelScope.launch {
      try {
        repository.updateProjectStatus(projectId, status)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _errorBanner.value = e.localizedMessage ?: "Failed to update status."
      }
    }
  }

  fun updateProjectDetails(
    title: String,
    category: ProjectCategory,
    location: String,
    status: ProjectStatus,
    budgetCents: Long,
    notes: String,
  ) {
    if (title.isBlank()) {
      _errorBanner.value = "Project title cannot be empty."
      return
    }
    viewModelScope.launch {
      try {
        repository.updateProject(projectId, title, category, location, status, budgetCents, notes)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _errorBanner.value = e.localizedMessage ?: "Failed to save changes."
      }
    }
  }

  fun deleteProject(onDeleted: () -> Unit) {
    viewModelScope.launch {
      try {
        repository.deleteProject(projectId)
        onDeleted()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _errorBanner.value = e.localizedMessage ?: "Failed to delete project."
      }
    }
  }

  fun addStep(title: String, phase: StepPhase, nextOrder: Long) {
    if (title.isBlank()) return
    viewModelScope.launch {
      try {
        repository.addStep(projectId, title, phase, nextOrder)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _errorBanner.value = e.localizedMessage ?: "Failed to add build step."
      }
    }
  }

  fun toggleStep(step: BuildStep) {
    viewModelScope.launch {
      try {
        repository.toggleStepCompleted(projectId, step.id, !step.completed)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _errorBanner.value = e.localizedMessage ?: "Failed to update step."
      }
    }
  }

  fun deleteStep(stepId: String) {
    viewModelScope.launch {
      try {
        repository.deleteStep(projectId, stepId)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _errorBanner.value = e.localizedMessage ?: "Failed to delete step."
      }
    }
  }

  fun addMaterial(name: String, quantity: Double, unit: String, unitCostCents: Long) {
    if (name.isBlank() || quantity <= 0.0) return
    viewModelScope.launch {
      try {
        repository.addMaterial(projectId, name, quantity, unit, unitCostCents)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _errorBanner.value = e.localizedMessage ?: "Failed to add material."
      }
    }
  }

  fun toggleMaterialPurchased(material: BuildMaterial) {
    viewModelScope.launch {
      try {
        repository.toggleMaterialPurchased(projectId, material.id, !material.purchased)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _errorBanner.value = e.localizedMessage ?: "Failed to update material."
      }
    }
  }

  fun deleteMaterial(materialId: String) {
    viewModelScope.launch {
      try {
        repository.deleteMaterial(projectId, materialId)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _errorBanner.value = e.localizedMessage ?: "Failed to delete material."
      }
    }
  }

  fun addMeasurement(label: String, dimensionValue: String, note: String) {
    if (label.isBlank() || dimensionValue.isBlank()) return
    viewModelScope.launch {
      try {
        repository.addMeasurement(projectId, label, dimensionValue, note)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _errorBanner.value = e.localizedMessage ?: "Failed to save measurement."
      }
    }
  }

  fun deleteMeasurement(measurementId: String) {
    viewModelScope.launch {
      try {
        repository.deleteMeasurement(projectId, measurementId)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _errorBanner.value = e.localizedMessage ?: "Failed to delete measurement."
      }
    }
  }

  private companion object {
    const val TAG = "ProjectDetailVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
