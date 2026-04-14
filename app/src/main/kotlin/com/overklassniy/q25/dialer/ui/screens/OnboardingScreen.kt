package com.overklassniy.q25.dialer.ui.screens

import android.app.role.RoleManager
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.overklassniy.q25.dialer.R
import com.overklassniy.q25.dialer.service.QwertyAccessibilityService
import com.overklassniy.q25.dialer.util.PermissionHelper

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }

    var isDefaultDialer by remember {
        mutableStateOf(
            try {
                val rm = context.getSystemService(RoleManager::class.java)
                rm?.isRoleHeld(RoleManager.ROLE_DIALER) == true
            } catch (_: Exception) { false }
        )
    }
    var isAccessibilityEnabled by remember {
        mutableStateOf(
            PermissionHelper.isAccessibilityServiceEnabled(context, QwertyAccessibilityService::class.java)
        )
    }

    val dialerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        isDefaultDialer = try {
            val rm = context.getSystemService(RoleManager::class.java)
            rm?.isRoleHeld(RoleManager.ROLE_DIALER) == true
        } catch (_: Exception) { false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (currentStep) {
            0 -> {
                // Step 1: Default dialer
                OnboardingStep(
                    icon = Icons.Filled.Phone,
                    title = stringResource(R.string.onboarding_default_dialer),
                    description = stringResource(R.string.onboarding_default_dialer_desc),
                    isDone = isDefaultDialer,
                    actionLabel = stringResource(R.string.onboarding_setup),
                    changeLabel = stringResource(R.string.onboarding_change),
                    onAction = {
                        try {
                            val rm = context.getSystemService(RoleManager::class.java)
                            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                                if (rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
                                    // Already default dialer – open default apps settings to change
                                    context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                                } else {
                                    val intent = rm.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                                    dialerLauncher.launch(intent)
                                }
                            }
                        } catch (_: Exception) { }
                    },
                )
            }
            1 -> {
                // Step 2: Accessibility service
                OnboardingStep(
                    icon = Icons.Filled.Accessibility,
                    title = stringResource(R.string.onboarding_accessibility),
                    description = stringResource(R.string.onboarding_accessibility_desc),
                    isDone = isAccessibilityEnabled,
                    actionLabel = stringResource(R.string.onboarding_setup),
                    changeLabel = stringResource(R.string.onboarding_change),
                    onAction = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        } catch (_: Exception) { }
                    },
                    onRefreshStatus = {
                        isAccessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(
                            context, QwertyAccessibilityService::class.java
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(
                onClick = {
                    if (currentStep < 1) {
                        currentStep++
                    } else {
                        onComplete()
                    }
                },
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.onboarding_skip))
            }

            if (currentStep < 1) {
                Button(
                    onClick = { currentStep++ },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.onboarding_next))
                }
            } else {
                Button(
                    onClick = onComplete,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.onboarding_done))
                }
            }
        }
    }
}

@Composable
private fun OnboardingStep(
    icon: ImageVector,
    title: String,
    description: String,
    isDone: Boolean,
    actionLabel: String,
    changeLabel: String,
    onAction: () -> Unit,
    onRefreshStatus: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            if (isDone) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.BottomEnd),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!isDone) {
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(actionLabel)
            }
        } else {
            Text(
                text = "✓",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.tertiary,
            )
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(changeLabel)
            }
        }

        if (onRefreshStatus != null) {
            OutlinedButton(
                onClick = onRefreshStatus,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(text = "↻", fontSize = 14.sp)
            }
        }
    }
}