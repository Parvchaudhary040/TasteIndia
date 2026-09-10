package com.parv.tasteindia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.parv.tasteindia.navigation.TasteIndiaNavHost
import com.parv.tasteindia.ui.theme.TasteIndiaTheme

/**
 * The only Activity. It does nothing but host the Compose tree; all screen logic lives in
 * composables + ViewModels, so there is no "giant Activity".
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TasteIndiaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    TasteIndiaNavHost()
                }
            }
        }
    }
}
