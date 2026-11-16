package com.example.ui.tools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.components.blueprintGridBackground
import com.example.ui.theme.BuildiMonoStyle
import java.util.Locale
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuilderToolsScreen(
  onOpenAccountDialog: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
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
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(36.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Filled.Calculate,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
            Column {
              Text(
                text = "Workshop Tools",
                style = MaterialTheme.typography.titleLarge,
              )
              Text(
                text = "Instant Site Calculators",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        },
        actions = {
          IconButton(
            onClick = onOpenAccountDialog,
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(Icons.Filled.Person, contentDescription = "Builder account")
          }
        },
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        item { PaintCoverageCalculatorCard() }
        item { LumberAndConcreteCalculatorCard() }
        item { UnitConverterCard() }
      }
    }
  }
}

@Composable
private fun PaintCoverageCalculatorCard() {
  var widthFt by remember { mutableStateOf("14") }
  var heightFt by remember { mutableStateOf("9") }
  var coats by remember { mutableStateOf("2") }
  var openings by remember { mutableStateOf("1") }

  val w = widthFt.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
  val h = heightFt.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
  val c = coats.toIntOrNull()?.coerceIn(1, 10) ?: 1
  val o = openings.toIntOrNull()?.coerceAtLeast(0) ?: 0
  val netAreaSqFt = ((w * h) - (o * 20.0)).coerceAtLeast(0.0)
  val totalCoverageSqFt = netAreaSqFt * c
  val gallonsNeeded = if (totalCoverageSqFt <= 0.0) 0 else ceil(totalCoverageSqFt / 350.0).toInt()

  CalculatorCardShell(
    icon = Icons.Filled.FormatPaint,
    title = "Paint & Primer Estimator",
    subtitle = "350 sq ft/gal standard coverage, 20 sq ft deduction per door/window",
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      NumberField(
        value = widthFt,
        onValueChange = { widthFt = it },
        label = "Wall Width (ft)",
        modifier = Modifier.weight(1f).testTag("paint_width_input"),
      )
      NumberField(
        value = heightFt,
        onValueChange = { heightFt = it },
        label = "Wall Height (ft)",
        modifier = Modifier.weight(1f).testTag("paint_height_input"),
      )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      NumberField(
        value = coats,
        onValueChange = { coats = it },
        label = "Coats",
        modifier = Modifier.weight(1f),
      )
      NumberField(
        value = openings,
        onValueChange = { openings = it },
        label = "Doors/Windows",
        modifier = Modifier.weight(1f),
      )
    }
    ResultStrip(
      primaryLabel = "Recommended Paint",
      primaryValue = "$gallonsNeeded gal",
      secondaryLabel = "Total Coat Area",
      secondaryValue = String.format(Locale.US, "%.0f sq ft", totalCoverageSqFt),
    )
  }
}

@Composable
private fun LumberAndConcreteCalculatorCard() {
  var isLumberMode by remember { mutableStateOf(true) }

  // Lumber inputs
  var thickIn by remember { mutableStateOf("2") }
  var widthIn by remember { mutableStateOf("6") }
  var lengthFt by remember { mutableStateOf("8") }
  var qty by remember { mutableStateOf("10") }

  // Concrete inputs
  var slabLengthFt by remember { mutableStateOf("10") }
  var slabWidthFt by remember { mutableStateOf("8") }
  var slabDepthIn by remember { mutableStateOf("4") }

  CalculatorCardShell(
    icon = Icons.Filled.SquareFoot,
    title = "Lumber & Concrete Estimator",
    subtitle = "Board feet for timber orders or 80 lb concrete bags for slabs",
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = isLumberMode,
        onClick = { isLumberMode = true },
        label = { Text("Board Feet") },
      )
      FilterChip(
        selected = !isLumberMode,
        onClick = { isLumberMode = false },
        label = { Text("Concrete Slab") },
      )
    }

    if (isLumberMode) {
      val t = thickIn.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
      val w = widthIn.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
      val l = lengthFt.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
      val q = qty.toIntOrNull()?.coerceAtLeast(0) ?: 0
      val boardFeet = (t * w * l / 12.0) * q

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NumberField(value = thickIn, onValueChange = { thickIn = it }, label = "Thick (in)", modifier = Modifier.weight(1f))
        NumberField(value = widthIn, onValueChange = { widthIn = it }, label = "Width (in)", modifier = Modifier.weight(1f))
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NumberField(value = lengthFt, onValueChange = { lengthFt = it }, label = "Length (ft)", modifier = Modifier.weight(1f))
        NumberField(value = qty, onValueChange = { qty = it }, label = "Boards", modifier = Modifier.weight(1f))
      }
      ResultStrip(
        primaryLabel = "Total Volume",
        primaryValue = String.format(Locale.US, "%.1f BF", boardFeet),
        secondaryLabel = "Per Board",
        secondaryValue = String.format(Locale.US, "%.2f BF", t * w * l / 12.0),
      )
    } else {
      val sl = slabLengthFt.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
      val sw = slabWidthFt.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
      val sd = slabDepthIn.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
      val cubicFeet = sl * sw * (sd / 12.0)
      val cubicYards = cubicFeet / 27.0
      val bags80lb = if (cubicFeet <= 0.0) 0 else ceil(cubicFeet / 0.6).toInt()

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NumberField(value = slabLengthFt, onValueChange = { slabLengthFt = it }, label = "Length (ft)", modifier = Modifier.weight(1f))
        NumberField(value = slabWidthFt, onValueChange = { slabWidthFt = it }, label = "Width (ft)", modifier = Modifier.weight(1f))
        NumberField(value = slabDepthIn, onValueChange = { slabDepthIn = it }, label = "Depth (in)", modifier = Modifier.weight(1f))
      }
      ResultStrip(
        primaryLabel = "80 lb Bags Needed",
        primaryValue = "$bags80lb bags",
        secondaryLabel = "Cubic Yards",
        secondaryValue = String.format(Locale.US, "%.2f yd³", cubicYards),
      )
    }
  }
}

@Composable
private fun UnitConverterCard() {
  var valueText by remember { mutableStateOf("48") }
  var mode by remember { mutableStateOf(0) } // 0: in -> cm, 1: ft -> m, 2: sq ft -> sq m

  val input = valueText.toDoubleOrNull() ?: 0.0
  val (fromLabel, toLabel, resultText) =
    when (mode) {
      0 -> Triple("Inches", "Centimeters", String.format(Locale.US, "%.2f cm", input * 2.54))
      1 -> Triple("Feet", "Meters", String.format(Locale.US, "%.2f m", input * 0.3048))
      else -> Triple("Sq Ft", "Sq Meters", String.format(Locale.US, "%.2f m²", input * 0.092903))
    }

  CalculatorCardShell(
    icon = Icons.Filled.SwapHoriz,
    title = "Dimension Converter",
    subtitle = "Quick imperial to metric workshop conversions",
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("in → cm") })
      FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("ft → m") })
      FilterChip(selected = mode == 2, onClick = { mode = 2 }, label = { Text("ft² → m²") })
    }
    NumberField(
      value = valueText,
      onValueChange = { valueText = it },
      label = "Enter $fromLabel",
      modifier = Modifier.fillMaxWidth().testTag("converter_input"),
    )
    ResultStrip(
      primaryLabel = toLabel,
      primaryValue = resultText,
      secondaryLabel = "Input",
      secondaryValue = "${valueText.ifBlank { "0" }} $fromLabel",
    )
  }
}

@Composable
private fun CalculatorCardShell(
  icon: ImageVector,
  title: String,
  subtitle: String,
  content: @Composable () -> Unit,
) {
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
          color = MaterialTheme.colorScheme.secondaryContainer,
          modifier = Modifier.size(40.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.size(22.dp),
            )
          }
        }
        Column {
          Text(text = title, style = MaterialTheme.typography.titleMedium)
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      content()
    }
  }
}

@Composable
private fun NumberField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    modifier = modifier,
  )
}

@Composable
private fun ResultStrip(
  primaryLabel: String,
  primaryValue: String,
  secondaryLabel: String,
  secondaryValue: String,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.secondary,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
          text = primaryLabel,
          style = MaterialTheme.typography.bodySmall,
          color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.78f),
        )
        Text(
          text = primaryValue,
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = androidx.compose.ui.graphics.Color.White,
        )
      }
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = secondaryLabel,
          style = MaterialTheme.typography.bodySmall,
          color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.78f),
        )
        Text(
          text = secondaryValue,
          style = BuildiMonoStyle,
          color = MaterialTheme.colorScheme.primaryContainer,
        )
      }
    }
  }
}
