package org.idmji.biblia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import org.idmji.biblia.ui.BibleScreen
import org.idmji.biblia.ui.BibleViewModel

class MainActivity : ComponentActivity() {
    private val bibleViewModel: BibleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BibleScreen(viewModel = bibleViewModel)
        }
    }
}
