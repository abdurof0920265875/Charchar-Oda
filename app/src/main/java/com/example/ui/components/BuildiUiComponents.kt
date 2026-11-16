package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Deck
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BuildProject
import com.example.data.model.ProjectCategory
import com.example.data.model.ProjectStatus
import com.example.data.model.StepPhase
import com.example.ui.theme.BlueprintNavy
import com.example.ui.theme.SageMeadow
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.TimberAmber
import java.text.NumberFormat
import java.util.Locale

fun formatCents(cents: Long): String {
  val formatter = NumberFormat.getCurrencyInstance(Locale.US)
  return formatter.format(cents / 100.0)
}

fun formatQuantity(qty: Double): String {
  return if (qty % 1.0 == 0.0) qty.toLong().toString()
  else String.format(Locale.US, "%.2f", qty).trimEnd('0').trimEnd('.')
}

fun categoryIcon(category: ProjectCategory): ImageVector =
  when (category) {
    ProjectCategory.RENOVATION -> Icons.Filled.HomeRepairService
    ProjectCategory.FURNITURE -> Icons.Filled.Chair
    ProjectCategory.OUTDOOR -> Icons.Filled.Deck
    ProjectCategory.SYSTEMS -> Icons.Filled.ElectricalServices
    ProjectCategory.CUSTOM -> Icons.Filled.Build
  }

fun categoryColor(category: ProjectCategory): Color =
  when (category) {
    ProjectCategory.RENOVATION -> TerracottaPrimary
    ProjectCategory.FURNITURE -> TimberAmber
    ProjectCategory.OUTDOOR -> SageMeadow
    ProjectCategory.SYSTEMS -> BlueprintNavy
    ProjectCategory.CUSTOM -> Color(0xFF7B4B94)
  }

@Composable
fun statusColors(status: ProjectStatus): Pair<Color, Color> =
  when (status) {
    ProjectStatus.PLANNING ->
      MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    ProjectStatus.IN_PROGRESS ->
      MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    ProjectStatus.FINISHING ->
      Color(0xFFFDF0D5) to Color(0xFF6E4700)
    ProjectStatus.COMPLETED ->
      MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
  }

@Composable
fun phaseBadgeColor(phase: StepPhase): Pair<Color, Color> =
  when (phase) {
    StepPhase.PREP ->
      MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    StepPhase.MATERIALS ->
      Color(0xFFFDF0D5) to Color(0xFF6E4700)
    StepPhase.BUILD ->
      MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    StepPhase.FINISH ->
      MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
  }

@Composable
fun Modifier.blueprintGridBackground(alpha: Float = 0.35f): Modifier {
  val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha)
  return this.drawBehind {
    val step = 28.dp.toPx()
    var x = 0f
    while (x <= size.width) {
      drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
      x += step
    }
    var y = 0f
    while (y <= size.height) {
      drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
      y += step
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectEditorDialog(
  initialProject: BuildProject? = null,
  onDismiss: () -> Unit,
  onSave: (
    title: String,
    category: ProjectCategory,
    location: String,
    status: ProjectStatus,
    budgetCents: Long,
    notes: String,
  ) -> Unit,
) {
  var title by remember { mutableStateOf(initialProject?.title ?: "") }
  var category by remember { mutableStateOf(initialProject?.categoryEnum ?: ProjectCategory.RENOVATION) }
  var location by remember { mutableStateOf(initialProject?.location ?: "") }
  var status by remember { mutableStateOf(initialProject?.statusEnum ?: ProjectStatus.PLANNING) }
  var budgetText by remember {
    mutableStateOf(
      if (initialProject != null && initialProject.budgetCents > 0)
        String.format(Locale.US, "%.2f", initialProject.budgetCents / 100.0)
      else ""
    )
  }
  var notes by remember { mutableStateOf(initialProject?.notes ?: "") }
  var titleError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (initialProject == null) stringResource(R.string.new_build) else stringResource(R.string.edit_project),
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it.take(100)
            titleError = false
          },
          label = { Text("Project Title *") },
          placeholder = { Text("e.g. Floating Oak Credenza") },
          isError = titleError,
          supportingText = if (titleError) { { Text("Enter a project name") } } else null,
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
          modifier = Modifier.fillMaxWidth().testTag("project_title_input"),
        )

        OutlinedTextField(
          value = location,
          onValueChange = { location = it.take(80) },
          label = { Text("Room / Site Location") },
          placeholder = { Text("e.g. Dining Room, Garage") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
          modifier = Modifier.fillMaxWidth().testTag("project_location_input"),
        )

        OutlinedTextField(
          value = budgetText,
          onValueChange = { budgetText = it },
          label = { Text("Target Budget ($)") },
          placeholder = { Text("350.00") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth().testTag("project_budget_input"),
        )

        Text(
          text = "Category",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          ProjectCategory.entries.forEach { item ->
            FilterChip(
              selected = category == item,
              onClick = { category = item },
              label = { Text(item.label) },
            )
          }
        }

        Text(
          text = "Status",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          ProjectStatus.entries.forEach { item ->
            FilterChip(
              selected = status == item,
              onClick = { status = item },
              label = { Text(item.label) },
            )
          }
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it.take(1000) },
          label = { Text("Blueprint Notes") },
          placeholder = { Text("Joinery style, finish specs, contractor reminders...") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth().testTag("project_notes_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isBlank()) {
            titleError = true
          } else {
            val budgetCents =
              ((budgetText.trim().toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0) * 100).toLong()
            onSave(title.trim(), category, location.trim(), status, budgetCents, notes.trim())
          }
        },
        modifier = Modifier.testTag("save_project_button"),
      ) {
        Text(if (initialProject == null) "Create Build" else "Save Changes")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

@Composable
fun ProfileAccountDialog(
  displayName: String?,
  email: String?,
  onDismiss: () -> Unit,
  onSignOut: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.secondaryContainer,
          modifier = Modifier.size(40.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.Person,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }
        Column {
          Text(
            text = displayName?.ifBlank { "Builder Account" } ?: "Builder Account",
            style = MaterialTheme.typography.titleMedium,
          )
          if (!email.isNullOrBlank()) {
            Text(
              text = email,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.CloudDone,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.tertiary,
            )
            Column {
              Text(
                text = "Cloud Sync Active",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
              )
              Text(
                text = "All projects, steps, materials, and measurements are backed up to your account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
              )
            }
          }
        }
        HorizontalDivider()
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedButton(
          onClick = onSignOut,
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.fillMaxWidth().testTag("sign_out_button"),
        ) {
          Text(stringResource(R.string.sign_out))
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done")
      }
    },
  )
}
