package com.example.ui.projects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.BuildProject
import com.example.data.model.ProjectCategory
import com.example.ui.components.ProjectEditorDialog
import com.example.ui.components.blueprintGridBackground
import com.example.ui.components.categoryColor
import com.example.ui.components.categoryIcon
import com.example.ui.components.formatCents
import com.example.ui.components.statusColors
import com.example.ui.state.UiState
import com.example.ui.theme.BuildiMonoStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
  viewModel: ProjectsViewModel,
  onOpenProject: (String) -> Unit,
  onOpenAccountDialog: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
  val totalCount by viewModel.allProjectsCount.collectAsStateWithLifecycle()
  val activeCount by viewModel.activeProjectsCount.collectAsStateWithLifecycle()
  val totalBudgetCents by viewModel.totalBudgetCents.collectAsStateWithLifecycle()
  val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

  var showCreateDialog by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(actionMessage) {
    val msg = actionMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessage()
    }
  }

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
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Surface(
              shape = CircleShape,
              color = Color.White,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier.size(38.dp),
            ) {
              Image(
                painter = painterResource(id = R.drawable.app_logo_custom),
                contentDescription = "App logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
              )
            }
            Column {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
              )
              Text(
                text = "Workshop Studio",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        },
        actions = {
          IconButton(
            onClick = onOpenAccountDialog,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("account_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Person,
              contentDescription = "Builder account",
            )
          }
        },
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { showCreateDialog = true },
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text(stringResource(R.string.new_build)) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("create_project_fab"),
      )
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .blueprintGridBackground(alpha = 0.35f),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        item {
          StudioSummaryBanner(
            totalCount = totalCount,
            activeCount = activeCount,
            totalBudgetCents = totalBudgetCents,
          )
        }

        item {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterChip(
              selected = selectedCategory == null,
              onClick = { viewModel.selectCategory(null) },
              label = { Text("All Builds") },
            )
            ProjectCategory.entries.forEach { category ->
              FilterChip(
                selected = selectedCategory == category,
                onClick = { viewModel.selectCategory(category) },
                label = { Text(category.label) },
              )
            }
          }
        }

        when (val state = projectsState) {
          is UiState.Loading -> {
            item {
              Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
              ) {
                CircularProgressIndicator()
              }
            }
          }

          is UiState.Error -> {
            item {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = state.message,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.padding(16.dp),
                )
              }
            }
          }

          is UiState.Success -> {
            val projects = state.data
            if (projects.isEmpty()) {
              item {
                AnimatedVisibility(visible = true, enter = fadeIn()) {
                  EmptyProjectsBlueprintCard(
                    hasCategoryFilter = selectedCategory != null,
                    isBusy = isBusy,
                    onClearFilter = { viewModel.selectCategory(null) },
                    onCreateProject = { showCreateDialog = true },
                    onLoadSample = { viewModel.seedStarterProject() },
                  )
                }
              }
            } else {
              items(projects, key = { it.id }) { project ->
                ProjectBuildCard(
                  project = project,
                  onClick = { onOpenProject(project.id) },
                )
              }
            }
          }
        }
      }
    }
  }

  if (showCreateDialog) {
    ProjectEditorDialog(
      onDismiss = { showCreateDialog = false },
      onSave = { title, category, location, status, budgetCents, notes ->
        showCreateDialog = false
        viewModel.createProject(
          title = title,
          category = category,
          location = location,
          status = status,
          budgetCents = budgetCents,
          notes = notes,
          onCreated = onOpenProject,
        )
      },
    )
  }
}

@Composable
private fun StudioSummaryBanner(
  totalCount: Int,
  activeCount: Int,
  totalBudgetCents: Long,
) {
  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "BLUEPRINT OVERVIEW",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primaryContainer,
          )
          Text(
            text = if (activeCount == 1) "1 Active Build" else "$activeCount Active Builds",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
          )
        }
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color.White.copy(alpha = 0.14f),
        ) {
          Text(
            text = "$totalCount total",
            style = BuildiMonoStyle,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        MetricPill(
          label = "Planned Budget",
          value = formatCents(totalBudgetCents),
          modifier = Modifier.weight(1f),
        )
        MetricPill(
          label = "Completed",
          value = "${(totalCount - activeCount).coerceAtLeast(0)}",
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun MetricPill(label: String, value: String, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color.White.copy(alpha = 0.1f),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.78f),
      )
      Text(
        text = value,
        style = BuildiMonoStyle.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun ProjectBuildCard(
  project: BuildProject,
  onClick: () -> Unit,
) {
  val accentColor = categoryColor(project.categoryEnum)
  val (statusBg, statusFg) = statusColors(project.statusEnum)

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onClick)
        .testTag("project_card_${project.id}"),
  ) {
    Row(modifier = Modifier.fillMaxWidth()) {
      Box(
        modifier =
          Modifier
            .width(6.dp)
            .height(132.dp)
            .background(accentColor)
      )
      Column(
        modifier = Modifier.weight(1f).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = accentColor.copy(alpha = 0.14f),
              modifier = Modifier.size(34.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = categoryIcon(project.categoryEnum),
                  contentDescription = project.categoryEnum.label,
                  tint = accentColor,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
            Text(
              text = project.categoryEnum.label.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = statusBg,
          ) {
            Text(
              text = project.statusEnum.label,
              style = MaterialTheme.typography.labelMedium,
              color = statusFg,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = project.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (project.notes.isNotBlank()) {
            Text(
              text = project.notes,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            if (project.location.isNotBlank()) {
              Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = project.location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (project.budgetCents > 0) {
              Text(
                text = formatCents(project.budgetCents),
                style = BuildiMonoStyle.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "Open project",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyProjectsBlueprintCard(
  hasCategoryFilter: Boolean,
  isBusy: Boolean,
  onClearFilter: () -> Unit,
  onCreateProject: () -> Unit,
  onLoadSample: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_empty_blueprint),
        contentDescription = "Empty architectural blueprint",
        contentScale = ContentScale.Crop,
        modifier =
          Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(16.dp)),
      )

      Text(
        text = if (hasCategoryFilter) "No builds in this category" else "Draft your first build",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text =
          if (hasCategoryFilter) {
            "Switch back to all builds or start a new project under this category."
          } else {
            "Organize renovation phases, bill of materials, budget targets, and field measurements in one studio."
          },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (hasCategoryFilter) {
        OutlinedButton(onClick = onClearFilter, modifier = Modifier.fillMaxWidth()) {
          Text("Show All Builds")
        }
      } else {
        Button(
          onClick = onCreateProject,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("empty_create_project_button"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.new_build))
        }
        OutlinedButton(
          onClick = onLoadSample,
          enabled = !isBusy,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("load_sample_button"),
        ) {
          Icon(Icons.Filled.AutoAwesome, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.load_sample_project))
        }
      }
    }
  }
}
