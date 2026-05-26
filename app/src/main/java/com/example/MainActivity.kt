package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SajiloViewModel
import com.example.ui.screens.MainMarketplaceContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable premium modern Edge-to-Edge full screen views
        enableEdgeToEdge()
        
        setContent {
            val viewModel: SajiloViewModel = viewModel()
            MainMarketplaceContainer(viewModel = viewModel)
        }
    }
}
