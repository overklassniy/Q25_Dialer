package com.overklassniy.q25.dialer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.overklassniy.q25.dialer.R
import com.overklassniy.q25.dialer.data.PreferencesManager
import com.overklassniy.q25.dialer.ui.theme.Background
import com.overklassniy.q25.dialer.ui.theme.CallGreen
import com.overklassniy.q25.dialer.ui.theme.CallRed
import com.overklassniy.q25.dialer.ui.theme.DefaultCallBackground
import com.overklassniy.q25.dialer.ui.theme.OnBackground
import com.overklassniy.q25.dialer.ui.theme.OnPrimary
import com.overklassniy.q25.dialer.ui.theme.OnSurface
import com.overklassniy.q25.dialer.ui.theme.OnSurfaceVariant
import com.overklassniy.q25.dialer.ui.theme.Primary
import com.overklassniy.q25.dialer.ui.theme.Secondary
import com.overklassniy.q25.dialer.ui.theme.Surface
import com.overklassniy.q25.dialer.ui.theme.SurfaceVariant

private val defaultColors = mapOf(
    PreferencesManager.KEY_COLOR_PRIMARY to Primary,
    PreferencesManager.KEY_COLOR_SECONDARY to Secondary,
    PreferencesManager.KEY_COLOR_BACKGROUND to Background,
    PreferencesManager.KEY_COLOR_SURFACE to Surface,
    PreferencesManager.KEY_COLOR_ON_PRIMARY to OnPrimary,
    PreferencesManager.KEY_COLOR_ON_BACKGROUND to OnBackground,
    PreferencesManager.KEY_COLOR_ON_SURFACE to OnSurface,
    PreferencesManager.KEY_COLOR_SURFACE_VARIANT to SurfaceVariant,
    PreferencesManager.KEY_COLOR_ON_SURFACE_VARIANT to OnSurfaceVariant,
    // Incoming call screen colors
    PreferencesManager.KEY_COLOR_INCOMING_CALL_BACKGROUND to DefaultCallBackground,
    PreferencesManager.KEY_COLOR_INCOMING_CALL_TEXT to Color.White,
    PreferencesManager.KEY_COLOR_INCOMING_CALL_DECLINE_BUTTON to CallRed,
    PreferencesManager.KEY_COLOR_INCOMING_CALL_ACCEPT_BUTTON to CallGreen,
    PreferencesManager.KEY_COLOR_INCOMING_CALL_MESSAGE_BUTTON to Primary,
    // Ongoing call screen colors
    PreferencesManager.KEY_COLOR_ONGOING_CALL_BACKGROUND to DefaultCallBackground,
    PreferencesManager.KEY_COLOR_ONGOING_CALL_TEXT to Color.White,
    PreferencesManager.KEY_COLOR_ONGOING_CALL_END_BUTTON to CallRed,
    PreferencesManager.KEY_COLOR_ONGOING_CALL_CONTROL_BUTTON_BG to SurfaceVariant,
    PreferencesManager.KEY_COLOR_ONGOING_CALL_CONTROL_BUTTON_ICON to OnSurfaceVariant,
    PreferencesManager.KEY_COLOR_ONGOING_CALL_DIALPAD_BG to Color(0xFF1C1C1E),
    PreferencesManager.KEY_COLOR_ONGOING_CALL_DIALPAD_TEXT to Color.White,
    PreferencesManager.KEY_COLOR_ONGOING_CALL_HOLD_BAR to Color(0xFF2C2C2E),
)

@Composable
fun ColorSettingsScreen(
    onColorsChanged: () -> Unit = {},
    highlightedIndex: Int = -1,
    activateTrigger: Int = 0,
    onItemCount: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    var showColorPicker by remember { mutableStateOf<String?>(null) }
    var colorRefreshKey by remember { mutableIntStateOf(0) }

    val themeColorEntries = remember(colorRefreshKey) {
        listOf(
            PreferencesManager.KEY_COLOR_PRIMARY to R.string.color_primary,
            PreferencesManager.KEY_COLOR_ON_PRIMARY to R.string.color_on_primary,
            PreferencesManager.KEY_COLOR_SECONDARY to R.string.color_secondary,
            PreferencesManager.KEY_COLOR_BACKGROUND to R.string.color_background,
            PreferencesManager.KEY_COLOR_ON_BACKGROUND to R.string.color_on_background,
            PreferencesManager.KEY_COLOR_SURFACE to R.string.color_surface,
            PreferencesManager.KEY_COLOR_ON_SURFACE to R.string.color_on_surface,
            PreferencesManager.KEY_COLOR_SURFACE_VARIANT to R.string.color_surface_variant,
            PreferencesManager.KEY_COLOR_ON_SURFACE_VARIANT to R.string.color_on_surface_variant,
        ).map { (key, labelRes) ->
            val saved = prefs.getCustomColor(key)
            val displayColor = if (saved != null) Color(saved.toULong()) else defaultColors[key]!!
            Triple(key, labelRes, displayColor)
        }
    }

    val incomingCallColorEntries = remember(colorRefreshKey) {
        listOf(
            PreferencesManager.KEY_COLOR_INCOMING_CALL_BACKGROUND to R.string.color_incoming_call_background,
            PreferencesManager.KEY_COLOR_INCOMING_CALL_TEXT to R.string.color_incoming_call_text,
            PreferencesManager.KEY_COLOR_INCOMING_CALL_DECLINE_BUTTON to R.string.color_incoming_call_decline_button,
            PreferencesManager.KEY_COLOR_INCOMING_CALL_ACCEPT_BUTTON to R.string.color_incoming_call_accept_button,
            PreferencesManager.KEY_COLOR_INCOMING_CALL_MESSAGE_BUTTON to R.string.color_incoming_call_message_button,
        ).map { (key, labelRes) ->
            val saved = prefs.getCustomColor(key)
            val displayColor = if (saved != null) Color(saved.toULong()) else defaultColors[key]!!
            Triple(key, labelRes, displayColor)
        }
    }

    val ongoingCallColorEntries = remember(colorRefreshKey) {
        listOf(
            PreferencesManager.KEY_COLOR_ONGOING_CALL_BACKGROUND to R.string.color_ongoing_call_background,
            PreferencesManager.KEY_COLOR_ONGOING_CALL_TEXT to R.string.color_ongoing_call_text,
            PreferencesManager.KEY_COLOR_ONGOING_CALL_END_BUTTON to R.string.color_ongoing_call_end_button,
            PreferencesManager.KEY_COLOR_ONGOING_CALL_CONTROL_BUTTON_BG to R.string.color_ongoing_call_control_button_bg,
            PreferencesManager.KEY_COLOR_ONGOING_CALL_CONTROL_BUTTON_ICON to R.string.color_ongoing_call_control_button_icon,
            PreferencesManager.KEY_COLOR_ONGOING_CALL_DIALPAD_BG to R.string.color_ongoing_call_dialpad_bg,
            PreferencesManager.KEY_COLOR_ONGOING_CALL_DIALPAD_TEXT to R.string.color_ongoing_call_dialpad_text,
            PreferencesManager.KEY_COLOR_ONGOING_CALL_HOLD_BAR to R.string.color_ongoing_call_hold_bar,
        ).map { (key, labelRes) ->
            val saved = prefs.getCustomColor(key)
            val displayColor = if (saved != null) Color(saved.toULong()) else defaultColors[key]!!
            Triple(key, labelRes, displayColor)
        }
    }

    val scrollState = rememberScrollState()

    // Compute total items: 9 theme colors + 5 incoming call colors + 8 ongoing call colors + 1 reset button (if visible)
    val hasCustomColors = prefs.hasCustomColors()
    // Use constant sizes (9 theme + 5 incoming + 8 ongoing) + optional reset button
    val totalItems = 9 + 5 + 8 + (if (hasCustomColors) 1 else 0)
    LaunchedEffect(totalItems) { onItemCount(totalItems) }

    // Track positions of items for scroll
    val itemPositions = remember { mutableMapOf<Int, Int>() }

    // Scroll to keep highlighted item fully visible (using actual positions)
    LaunchedEffect(highlightedIndex, itemPositions[highlightedIndex]) {
        if (highlightedIndex >= 0) {
            val position = itemPositions[highlightedIndex]
            if (position != null) {
                scrollState.animateScrollTo(position.coerceAtMost(scrollState.maxValue))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // Track item index for keyboard navigation highlighting
        var itemIndex = 0
        fun nextIndex(): Int = itemIndex++

        // Section: Theme colors
        Text(
            text = stringResource(R.string.settings_custom_colors).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        )

        themeColorEntries.forEachIndexed { _, (key, labelRes, displayColor) ->
            val rowIndex = nextIndex()
            ColorSettingRow(
                title = stringResource(labelRes),
                displayColor = displayColor,
                isCustom = prefs.getCustomColor(key) != null,
                onClick = { showColorPicker = key },
                isHighlighted = highlightedIndex == rowIndex,
                activateTrigger = activateTrigger,
                onActivate = { showColorPicker = key },
                onPositioned = { pos -> itemPositions[rowIndex] = pos },
            )
        }

        // Section: Incoming call screen colors
        Text(
            text = stringResource(R.string.color_section_incoming_call_screen).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        )

        incomingCallColorEntries.forEachIndexed { _, (key, labelRes, displayColor) ->
            val rowIndex = nextIndex()
            ColorSettingRow(
                title = stringResource(labelRes),
                displayColor = displayColor,
                isCustom = prefs.getCustomColor(key) != null,
                onClick = { showColorPicker = key },
                isHighlighted = highlightedIndex == rowIndex,
                activateTrigger = activateTrigger,
                onActivate = { showColorPicker = key },
                onPositioned = { pos -> itemPositions[rowIndex] = pos },
            )
        }

        // Section: Ongoing call screen colors
        Text(
            text = stringResource(R.string.color_section_ongoing_call_screen).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        )

        ongoingCallColorEntries.forEachIndexed { _, (key, labelRes, displayColor) ->
            val rowIndex = nextIndex()
            ColorSettingRow(
                title = stringResource(labelRes),
                displayColor = displayColor,
                isCustom = prefs.getCustomColor(key) != null,
                onClick = { showColorPicker = key },
                isHighlighted = highlightedIndex == rowIndex,
                activateTrigger = activateTrigger,
                onActivate = { showColorPicker = key },
                onPositioned = { pos -> itemPositions[rowIndex] = pos },
            )
        }

        if (hasCustomColors) {
            Spacer(Modifier.height(8.dp))
            val resetIndex = nextIndex()
            val isResetHighlighted = highlightedIndex == resetIndex
            val backgroundColor = if (isResetHighlighted)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else
                Color.Transparent
            // Activate when highlighted and trigger fires
            LaunchedEffect(activateTrigger) {
                if (isResetHighlighted && activateTrigger > 0) {
                    prefs.resetCustomColors()
                    colorRefreshKey++
                    Toast
                        .makeText(context, R.string.colors_reset_done, Toast.LENGTH_SHORT)
                        .show()
                    // Restart to apply
                    val intent = (context as? android.app.Activity)?.intent
                    (context as? android.app.Activity)?.finish()
                    if (intent != null) context.startActivity(intent)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onPlaced { coordinates ->
                        itemPositions[resetIndex] = coordinates.positionInParent().y.toInt()
                    }
                    .background(backgroundColor)
                    .clickable {
                        prefs.resetCustomColors()
                        colorRefreshKey++
                        Toast
                            .makeText(context, R.string.colors_reset_done, Toast.LENGTH_SHORT)
                            .show()
                        // Restart to apply
                        val intent = (context as? android.app.Activity)?.intent
                        (context as? android.app.Activity)?.finish()
                        if (intent != null) context.startActivity(intent)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.settings_reset_colors),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_reset_colors_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }

    if (showColorPicker != null) {
        val pickerKey = showColorPicker!!
        val saved = prefs.getCustomColor(pickerKey)
        val initialColor = if (saved != null) Color(saved.toULong()) else defaultColors[pickerKey]!!
        val isCustom = saved != null

        HsvColorPickerDialog(
            initialColor = initialColor,
            showReset = isCustom,
            onDismiss = { showColorPicker = null },
            onColorSelected = { color ->
                prefs.setCustomColor(pickerKey, color.value.toLong())
                colorRefreshKey++
                onColorsChanged()
                @Suppress("UNUSED_VALUE")
                showColorPicker = null
            },
            onReset = {
                prefs.removeCustomColor(pickerKey)
                colorRefreshKey++
                onColorsChanged()
                @Suppress("UNUSED_VALUE")
                showColorPicker = null
            },
        )
    }
}

@Composable
private fun ColorSettingRow(
    title: String,
    displayColor: Color,
    isCustom: Boolean,
    onClick: () -> Unit,
    isHighlighted: Boolean = false,
    activateTrigger: Int = 0,
    onActivate: () -> Unit = {},
    onPositioned: ((Int) -> Unit)? = null,
) {
    val backgroundColor = if (isHighlighted)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else
        Color.Transparent
    // Activate when highlighted and trigger fires
    LaunchedEffect(activateTrigger) {
        if (isHighlighted && activateTrigger > 0) {
            onActivate()
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onPlaced { coordinates ->
                onPositioned?.invoke(coordinates.positionInParent().y.toInt())
            }
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isCustom) {
                    String.format("#%06X", displayColor.toArgb() and 0xFFFFFF)
                } else {
                    stringResource(R.string.settings_custom_colors_desc)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
        Spacer(Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(displayColor)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
    }
}

//  HSV Square Color Picker

private fun colorToHsv(color: Color): FloatArray {
    val argb = color.toArgb()
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)
    return hsv
}

private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val argb = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
    return Color(argb)
}

@Composable
private fun HsvColorPickerDialog(
    initialColor: Color,
    showReset: Boolean = false,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
    onReset: () -> Unit = {},
) {
    val initHsv = remember { colorToHsv(initialColor) }
    var hue by remember { mutableFloatStateOf(initHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initHsv[1]) }
    var value by remember { mutableFloatStateOf(initHsv[2]) }

    val currentColor = hsvToColor(hue, saturation, value)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.color_picker_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Saturation-Value square
                SaturationValuePanel(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onSaturationValueChanged = { s, v ->
                        saturation = s
                        value = v
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )

                // Hue slider
                Text(
                    text = stringResource(R.string.color_hue),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                HueSlider(
                    hue = hue,
                    onHueChanged = { hue = it },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Preview + hex
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    )
                    Text(
                        text = String.format("#%06X", currentColor.toArgb() and 0xFFFFFF),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(currentColor) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            Row {
                if (showReset) {
                    TextButton(onClick = onReset) {
                        Text(
                            text = stringResource(R.string.color_reset),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        },
    )
}

@Composable
private fun SaturationValuePanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onSaturationValueChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hueColor = hsvToColor(hue, 1f, 1f)

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val s = (offset.x / size.width).coerceIn(0f, 1f)
                        val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        onSaturationValueChanged(s, v)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val s = (change.position.x / size.width).coerceIn(0f, 1f)
                        val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        onSaturationValueChanged(s, v)
                    }
                },
        ) {
            // White-to-hue horizontal gradient
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, hueColor),
                ),
                size = size,
            )
            // Transparent-to-black vertical gradient overlay
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                ),
                size = size,
            )

            // Draw selector circle
            val cx = saturation * size.width
            val cy = (1f - value) * size.height
            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx()),
            )
            drawCircle(
                color = Color.Black,
                radius = 8.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

@Composable
private fun HueSlider(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hueColors = remember {
        (0..360 step 30).map { h ->
            hsvToColor(h.toFloat(), 1f, 1f)
        }
    }

    Box(modifier = modifier.height(24.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onHueChanged((offset.x / size.width).coerceIn(0f, 1f) * 360f)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        onHueChanged((change.position.x / size.width).coerceIn(0f, 1f) * 360f)
                    }
                },
        ) {
            drawRect(
                brush = Brush.horizontalGradient(hueColors),
                size = Size(size.width, size.height),
            )

            // Thumb
            val thumbX = (hue / 360f) * size.width
            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = Offset(thumbX, size.height / 2f),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}