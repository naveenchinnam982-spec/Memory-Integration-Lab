package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.data.local.AppDatabase
import com.example.data.repository.MemoryRepository
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AnalyticsViewModel
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ComparisonViewModel
import com.example.ui.viewmodel.MemoryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = MemoryRepository(database.memoryDao(), database.chatDao())

        lifecycleScope.launch {
            repository.initializeWithDefaultMemoriesIfEmpty()
        }

        val chatViewModel = ChatViewModel(repository)
        val memoryViewModel = MemoryViewModel(repository)
        val comparisonViewModel = ComparisonViewModel(repository)
        val analyticsViewModel = AnalyticsViewModel(repository)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        chatViewModel = chatViewModel,
                        memoryViewModel = memoryViewModel,
                        comparisonViewModel = comparisonViewModel,
                        analyticsViewModel = analyticsViewModel
                    )
                }
            }
        }
    }
}
