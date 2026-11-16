package com.example.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.BuildMaterial
import com.example.data.model.BuildProject
import com.example.data.model.BuildStep
import com.example.data.model.ProjectStatus
import com.example.data.model.SiteMeasurement
import com.example.ui.components.ProjectEditorDialog
import com.example.ui.components.blueprintGridBackground
import com.example.ui.components.categoryColor
import com.example.ui.components.categoryIcon
import com.example.ui.components.formatCents
import com.example.ui.components.formatQuantity
import com.example.ui.components.phaseBadgeColor
import com.example.ui.state.UiState
import com.example.ui.theme.BuildiMonoStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
  viewModel: ProjectDetailViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val projectState by viewModel.projectState.collectAsStateWithLifecycle()
  val stepsState by viewModel.stepsState.collectAsStateWithLifecycle()
  val materialsState by viewModel.materialsState.collectAsStateWithLifecycle()
  val measurementsState by viewModel.measurementsState.collectAsStateWithLifecycle()
  val errorBanner by viewModel.errorBanner.collectAsStateWithLifecycle()

  var selectedTab by remember { mutableIntStateOf(0) }
  var showEditDialog by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  var showAddStepDialog by remember { mutableStateOf(false) }
  var showAddMaterialDialog by remember { mutableStateOf(false) }
  var showAddMeasurementDialog by remember { mutableStateOf(false) }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(errorBanner) {
    val msg = errorBanner
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearErrorBanner()
    }
  }

  val currentProject = (projectState as? UiState.Success)?.data
  val steps = (stepsState as? UiState.Success)?.data ?: emptyList()
  val materials = (materialsState as? UiState.Success)?.data ?: emptyList()
  val measurements = (measurementsState as? UiState.Success)?.data ?: emptyList()

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
          ),
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("detail_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to projects",
            )
          }
        },
        title = {
          Text(
            text = currentProject?.title ?: "Build Workspace",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleLarge,
          )
        },
        actions = {
          if (currentProject != null) {
            IconButton(
              onClick = { showEditDialog = true },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("edit_project_button"),
            ) {
              Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_project))
            }
            IconButton(
              onClick = { showDeleteDialog = true },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_project_button"),
            ) {
              Icon(
                Icons.Filled.DeleteOutline,
                contentDescription = stringResource(R.string.delete_project),
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        },
      )
    },
    floatingActionButton = {
      if (currentProject != null) {
        val fabLabel =
          when (selectedTab) {
            0 -> "Add Step"
            1 -> "Add Material"
            else -> "Add Dimension"
          }
        ExtendedFloatingActionButton(
          onClick = {
            when (selectedTab) {
              0 -> showAddStepDialog = true
              1 -> showAddMaterialDialog = true
              else -> showAddMeasurementDialog = true
            }
          },
          icon = { Icon(Icons.Filled.Add, contentDescription = null) },
          text = { Text(fabLabel) },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.testTag("detail_add_fab"),
        )
      }
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .blueprintGridBackground(alpha = 0.30f),
      contentAlignment = Alignment.TopCenter,
    ) {
      when (val pState = projectState) {
        is UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }

        is UiState.Error -> {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.widthIn(max = 600.dp).padding(16.dp),
          ) {
            Text(
              text = pState.message,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(16.dp),
            )
          }
        }

        is UiState.Success -> {
          val project = pState.data
          if (project == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text("This build project no longer exists.", style = MaterialTheme.typography.bodyLarge)
            }
          } else {
            val estimatedTotalCents = materials.sumOf { it.totalCostCents }
            val purchasedTotalCents = materials.filter { it.purchased }.sumOf { it.totalCostCents }
            val completedStepsCount = steps.count { it.completed }

            LazyColumn(
              modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
              contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              item {
                ProjectHeaderSummaryCard(
                  project = project,
                  completedSteps = completedStepsCount,
                  totalSteps = steps.size,
                  estimatedCostCents = estimatedTotalCents,
                  onStatusSelected = { viewModel.updateProjectStatus(it) },
                )
              }

              item {
                SecondaryTabRow(
                  selectedTabIndex = selectedTab,
                  containerColor = MaterialTheme.colorScheme.surface,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("${stringResource(R.string.tab_steps)} (${steps.size})") },
                    modifier = Modifier.testTag("tab_steps"),
                  )
                  Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("${stringResource(R.string.tab_materials)} (${materials.size})") },
                    modifier = Modifier.testTag("tab_materials"),
                  )
                  Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("${stringResource(R.string.tab_measurements)} (${measurements.size})") },
                    modifier = Modifier.testTag("tab_measurements"),
                  )
                }
              }

              when (selectedTab) {
                0 -> {
                  if (steps.isEmpty()) {
                    item {
                      EmptySectionCard(
                        icon = Icons.Filled.CheckCircle,
                        title = "No build steps yet",
                        subtitle = "Break down your project into clear steps across Prep, Materials, Build, and Finish.",
                      )
                    }
                  } else {
                    items(steps, key = { it.id }) { step ->
                      StepRowCard(
                        step = step,
                        onToggle = { viewModel.toggleStep(step) },
                        onDelete = { viewModel.deleteStep(step.id) },
                      )
                    }
                  }
                }

                1 -> {
                  item {
                    MaterialsCostSummaryCard(
                      estimatedTotalCents = estimatedTotalCents,
                      purchasedTotalCents = purchasedTotalCents,
                      budgetCents = project.budgetCents,
                    )
                  }
                  if (materials.isEmpty()) {
                    item {
                      EmptySectionCard(
                        icon = Icons.Filled.Inventory2,
                        title = "Bill of materials is empty",
                        subtitle = "Add lumber, hardware, paint, or fixtures to track quantities and live cost.",
                      )
                    }
                  } else {
                    items(materials, key = { it.id }) { material ->
                      MaterialRowCard(
                        material = material,
                        onTogglePurchased = { viewModel.toggleMaterialPurchased(material) },
                        onDelete = { viewModel.deleteMaterial(material.id) },
                      )
                    }
                  }
                }

                else -> {
                  if (measurements.isEmpty()) {
                    item {
                      EmptySectionCard(
                        icon = Icons.Filled.Straighten,
                        title = "No field measurements yet",
                        subtitle = "Save exact wall widths, clearances, and cut dimensions right inside this project.",
                      )
                    }
                  } else {
                    items(measurements, key = { it.id }) { measurement ->
                      MeasurementRowCard(
                        measurement = measurement,
                        onDelete = { viewModel.deleteMeasurement(measurement.id) },
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showEditDialog && currentProject != null) {
    ProjectEditorDialog(
      initialProject = currentProject,
      onDismiss = { showEditDialog = false },
      onSave = { title, category, location, status, budgetCents, notes ->
        showEditDialog = false
        viewModel.updateProjectDetails(title, category, location, status, budgetCents, notes)
      },
    )
  }

  if (showDeleteDialog && currentProject != null) {
    ConfirmDeleteProjectDialog(
      projectTitle = currentProject.title,
      onDismiss = { showDeleteDialog = false },
      onConfirm = {
        showDeleteDialog = false
        viewModel.deleteProject(onDeleted = onNavigateBack)
      },
    )
  }

  if (showAddStepDialog) {
    AddStepDialog(
      onDismiss = { showAddStepDialog = false },
      onAdd = { title, phase ->
        showAddStepDialog = false
        viewModel.addStep(title, phase, steps.size.toLong())
      },
    )
  }

  if (showAddMaterialDialog) {
    AddMaterialDialog(
      onDismiss = { showAddMaterialDialog = false },
      onAdd = { name, qty, unit, costCents ->
        showAddMaterialDialog = false
        viewModel.addMaterial(name, qty, unit, costCents)
      },
    )
  }

  if (showAddMeasurementDialog) {
    AddMeasurementDialog(
      onDismiss = { showAddMeasurementDialog = false },
      onAdd = { label, value, note ->
        showAddMeasurementDialog = false
        viewModel.addMeasurement(label, value, note)
      },
    )
  }
}

@Composable
private fun ProjectHeaderSummaryCard(
  project: BuildProject,
  completedSteps: Int,
  totalSteps: Int,
  estimatedCostCents: Long,
  onStatusSelected: (ProjectStatus) -> Unit,
) {
  val accentColor = categoryColor(project.categoryEnum)
  val progress = if (totalSteps == 0) 0f else completedSteps.toFloat() / totalSteps.toFloat()

  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = accentColor.copy(alpha = 0.14f),
          modifier = Modifier.size(40.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = categoryIcon(project.categoryEnum),
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(22.dp),
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = project.categoryEnum.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (project.location.isNotBlank()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = project.location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
        if (project.budgetCents > 0) {
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "Target",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = formatCents(project.budgetCents),
              style = BuildiMonoStyle.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      if (project.notes.isNotBlank()) {
        Text(
          text = project.notes,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ProjectStatus.entries.forEach { status ->
          FilterChip(
            selected = project.statusEnum == status,
            onClick = { onStatusSelected(status) },
            label = { Text(status.label) },
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Build Progress",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = if (totalSteps == 0) "No steps yet" else "$completedSteps / $totalSteps done",
            style = BuildiMonoStyle,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier.fillMaxWidth().height(8.dp),
          color = MaterialTheme.colorScheme.tertiary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
      }

      if (estimatedCostCents > 0) {
        Text(
          text = "Estimated Materials: ${formatCents(estimatedCostCents)}",
          style = BuildiMonoStyle,
          color =
            if (project.budgetCents in 1 until estimatedCostCents) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.secondary,
        )
      }
    }
  }
}

@Composable
private fun StepRowCard(
  step: BuildStep,
  onToggle: () -> Unit,
  onDelete: () -> Unit,
) {
  val (phaseBg, phaseFg) = phaseBadgeColor(step.phaseEnum)
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Checkbox(
        checked = step.completed,
        onCheckedChange = { onToggle() },
        modifier = Modifier.minimumInteractiveComponentSize().testTag("step_checkbox_${step.id}"),
      )
      Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
        Surface(shape = RoundedCornerShape(6.dp), color = phaseBg) {
          Text(
            text = step.phaseEnum.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = phaseFg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = step.title,
          style = MaterialTheme.typography.bodyLarge,
          textDecoration = if (step.completed) TextDecoration.LineThrough else TextDecoration.None,
          color =
            if (step.completed) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
        )
      }
      IconButton(
        onClick = onDelete,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Icon(
          imageVector = Icons.Filled.DeleteOutline,
          contentDescription = "Delete step",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun MaterialsCostSummaryCard(
  estimatedTotalCents: Long,
  purchasedTotalCents: Long,
  budgetCents: Long,
) {
  val isOverBudget = budgetCents in 1 until estimatedTotalCents
  Card(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isOverBudget) MaterialTheme.colorScheme.errorContainer
          else MaterialTheme.colorScheme.secondaryContainer
      ),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = if (isOverBudget) "Over Target Budget" else "Estimated Materials",
          style = MaterialTheme.typography.labelLarge,
          color =
            if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
          text = formatCents(estimatedTotalCents),
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
          color =
            if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
      Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = "Purchased",
          style = MaterialTheme.typography.bodySmall,
          color =
            if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
          text = formatCents(purchasedTotalCents),
          style = BuildiMonoStyle.copy(fontWeight = FontWeight.Bold),
          color =
            if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
    }
  }
}

@Composable
private fun MaterialRowCard(
  material: BuildMaterial,
  onTogglePurchased: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Checkbox(
        checked = material.purchased,
        onCheckedChange = { onTogglePurchased() },
        modifier = Modifier.minimumInteractiveComponentSize().testTag("material_checkbox_${material.id}"),
      )
      Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
        Text(
          text = material.name,
          style = MaterialTheme.typography.titleMedium,
          textDecoration = if (material.purchased) TextDecoration.LineThrough else TextDecoration.None,
          color =
            if (material.purchased) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "${formatQuantity(material.quantity)} ${material.unit} × ${formatCents(material.unitCostCents)}",
          style = BuildiMonoStyle,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = formatCents(material.totalCostCents),
        style = BuildiMonoStyle.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.width(4.dp))
      IconButton(
        onClick = onDelete,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Icon(
          imageVector = Icons.Filled.DeleteOutline,
          contentDescription = "Delete material",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun MeasurementRowCard(
  measurement: SiteMeasurement,
  onDelete: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondary,
      ) {
        Text(
          text = measurement.dimensionValue,
          style = BuildiMonoStyle.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = measurement.label, style = MaterialTheme.typography.titleMedium)
        if (measurement.note.isNotBlank()) {
          Text(
            text = measurement.note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      IconButton(
        onClick = onDelete,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Icon(
          imageVector = Icons.Filled.DeleteOutline,
          contentDescription = "Delete measurement",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun EmptySectionCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(34.dp),
      )
      Text(text = title, style = MaterialTheme.typography.titleMedium)
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
