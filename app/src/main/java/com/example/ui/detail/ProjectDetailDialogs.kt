package com.example.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.StepPhase

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddStepDialog(
  onDismiss: () -> Unit,
  onAdd: (title: String, phase: StepPhase) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var phase by remember { mutableStateOf(StepPhase.BUILD) }
  var showError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Build Step", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it.take(120)
            showError = false
          },
          label = { Text("Step Description *") },
          placeholder = { Text("e.g. Cut 2x4 frame rails to 54 inches") },
          isError = showError,
          supportingText = if (showError) { { Text("Enter a step description") } } else null,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
          modifier = Modifier.fillMaxWidth().testTag("step_title_input"),
        )
        Text(
          text = "Build Phase",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          StepPhase.entries.forEach { item ->
            FilterChip(
              selected = phase == item,
              onClick = { phase = item },
              label = { Text(item.label) },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isBlank()) showError = true else onAdd(title.trim(), phase)
        },
        modifier = Modifier.testTag("confirm_add_step_button"),
      ) {
        Text("Add Step")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun AddMaterialDialog(
  onDismiss: () -> Unit,
  onAdd: (name: String, quantity: Double, unit: String, unitCostCents: Long) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var qtyText by remember { mutableStateOf("1") }
  var unit by remember { mutableStateOf("pcs") }
  var costText by remember { mutableStateOf("") }
  var showError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Material", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it.take(100)
            showError = false
          },
          label = { Text("Material / Item *") },
          placeholder = { Text("e.g. 3/4\" Birch Plywood Sheet") },
          isError = showError,
          supportingText = if (showError) { { Text("Enter material name and valid quantity") } } else null,
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
          modifier = Modifier.fillMaxWidth().testTag("material_name_input"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = qtyText,
            onValueChange = {
              qtyText = it
              showError = false
            },
            label = { Text("Qty") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("material_qty_input"),
          )
          OutlinedTextField(
            value = unit,
            onValueChange = { unit = it.take(24) },
            label = { Text("Unit") },
            placeholder = { Text("pcs, ft, gal") },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("material_unit_input"),
          )
        }
        OutlinedTextField(
          value = costText,
          onValueChange = { costText = it },
          label = { Text("Unit Cost ($)") },
          placeholder = { Text("24.50") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth().testTag("material_cost_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val qty = qtyText.trim().toDoubleOrNull() ?: 0.0
          val costCents = ((costText.trim().toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0) * 100).toLong()
          if (name.isBlank() || qty <= 0.0) {
            showError = true
          } else {
            onAdd(name.trim(), qty, unit.trim().ifBlank { "pcs" }, costCents)
          }
        },
        modifier = Modifier.testTag("confirm_add_material_button"),
      ) {
        Text("Add Material")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun AddMeasurementDialog(
  onDismiss: () -> Unit,
  onAdd: (label: String, dimensionValue: String, note: String) -> Unit,
) {
  var label by remember { mutableStateOf("") }
  var value by remember { mutableStateOf("") }
  var note by remember { mutableStateOf("") }
  var showError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Log Site Measurement", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = label,
          onValueChange = {
            label = it.take(80)
            showError = false
          },
          label = { Text("Label *") },
          placeholder = { Text("e.g. Alcove Width, Counter Height") },
          isError = showError && label.isBlank(),
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
          modifier = Modifier.fillMaxWidth().testTag("measurement_label_input"),
        )
        OutlinedTextField(
          value = value,
          onValueChange = {
            value = it.take(40)
            showError = false
          },
          label = { Text("Dimension *") },
          placeholder = { Text("e.g. 74.5 in or 189 cm") },
          isError = showError && value.isBlank(),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("measurement_value_input"),
        )
        OutlinedTextField(
          value = note,
          onValueChange = { note = it.take(300) },
          label = { Text("Site Note (optional)") },
          placeholder = { Text("Check baseboard trim clearance") },
          minLines = 2,
          maxLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("measurement_note_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (label.isBlank() || value.isBlank()) showError = true
          else onAdd(label.trim(), value.trim(), note.trim())
        },
        modifier = Modifier.testTag("confirm_add_measurement_button"),
      ) {
        Text("Save Dimension")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun ConfirmDeleteProjectDialog(
  projectTitle: String,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Delete Build?") },
    text = {
      Text(
        "Remove \"$projectTitle\" and all of its steps, materials, and site measurements? This cannot be undone."
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.testTag("confirm_delete_project_button"),
      ) {
        Text("Delete")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
