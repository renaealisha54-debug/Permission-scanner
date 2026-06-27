package com.permissionscanner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.permissionscanner.app.ui.ScannerScreen
import com.permissionscanner.app.ui.ScannerViewModel

class MainActivity : ComponentActivity() {

    private val vm: ScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface { ScannerScreen(vm) }
            }
        }
    }
}
