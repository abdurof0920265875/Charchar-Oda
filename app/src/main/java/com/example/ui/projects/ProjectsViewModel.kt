package com.example.ui.projects

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BuildProject
import com.example.data.model.ProjectCategory
import com.example.data.model.ProjectStatus
import com.example.data.repository.BuildRepository
import com.example.ui.state.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(
  private val repository: BuildRepository,
  private val currentUserId: String,
) : ViewModel() {

  private val _selectedCategory = MutableStateFlow<ProjectCategory?>(null)
  val selectedCategory: StateFlow<ProjectCategory?> = _selectedCategory.asStateFlow()

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private val rawProjectsState: StateFlow<UiState<List<BuildProject>>> =
    repository
      .observeProjects(currentUserId)
      .map<List<BuildProject>, UiState<List<BuildProject>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing projects", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load projects"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val projectsState: StateFlow<UiState<List<BuildProject>>> =
    combine(rawProjectsState, _selectedCategory) { state, category ->
      when (state) {
        is UiState.Success -> {
          val filtered =
            if (category == null) state.data
            else state.data.filter { it.categoryEnum == category }
          UiState.Success(filtered)
        }
        else -> state
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  val allProjectsCount: StateFlow<Int> =
    rawProjectsState
      .map { state -> (state as? UiState.Success)?.data?.size ?: 0 }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = 0,
      )

  val activeProjectsCount: StateFlow<Int> =
    rawProjectsState
      .map { state ->
        (state as? UiState.Success)?.data?.count { it.statusEnum != ProjectStatus.COMPLETED } ?: 0
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = 0,
      )

  val totalBudgetCents: StateFlow<Long> =
    rawProjectsState
      .map { state ->
        (state as? UiState.Success)?.data?.sumOf { it.budgetCents } ?: 0L
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = 0L,
      )

  fun selectCategory(category: ProjectCategory?) {
    _selectedCategory.value = if (_selectedCategory.value == category) null else category
  }

  fun clearMessage() {
    _actionMessage.value = null
  }

  fun createProject(
    title: String,
    category: ProjectCategory,
    location: String,
    status: ProjectStatus,
    budgetCents: Long,
    notes: String,
    onCreated: (String) -> Unit = {},
  ) {
    if (title.isBlank()) {
      _actionMessage.value = "Project title is required."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      try {
        val id = repository.createProject(title, category, location, status, budgetCents, notes)
        onCreated(id)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _actionMessage.value = e.localizedMessage ?: "Could not create project."
      } finally {
        _isBusy.value = false
      }
    }
  }

  fun deleteProject(projectId: String) {
    viewModelScope.launch {
      _isBusy.value = true
      try {
        repository.deleteProject(projectId)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _actionMessage.value = e.localizedMessage ?: "Could not delete project."
      } finally {
        _isBusy.value = false
      }
    }
  }

  fun seedStarterProject() {
    viewModelScope.launch {
      _isBusy.value = true
      try {
        repository.seedStarterProjectIfEmpty()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _actionMessage.value = e.localizedMessage ?: "Could not load sample project."
      } finally {
        _isBusy.value = false
      }
    }
  }

  private companion object {
    const val TAG = "ProjectsVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
