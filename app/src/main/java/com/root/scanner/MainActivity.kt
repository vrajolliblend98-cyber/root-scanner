package com.root.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.root.scanner.model.Device
import com.root.scanner.ui.screens.DeviceDetailScreen
import com.root.scanner.ui.screens.ScanScreen
import com.root.scanner.ui.theme.RootTheme
import com.root.scanner.viewmodel.ScanViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RootTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun RootApp(viewModel: ScanViewModel) {
    var selectedDevice by remember { mutableStateOf<Device?>(null) }

    if (selectedDevice == null) {
        ScanScreen(
            viewModel = viewModel,
            onDeviceClick = { device -> selectedDevice = device }
        )
    } else {
        DeviceDetailScreen(
            device = selectedDevice!!,
            viewModel = viewModel,
            onBack = { selectedDevice = null }
        )
    }
}
