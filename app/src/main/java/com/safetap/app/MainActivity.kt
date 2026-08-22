package com.safetap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.safetap.app.navigation.SafeTapNavHost
import com.safetap.app.ui.theme.SafeTapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafeTapTheme {
                val navController = rememberNavController()
                SafeTapNavHost(
                    navController = navController,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
