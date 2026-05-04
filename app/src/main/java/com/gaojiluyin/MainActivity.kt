package com.gaojiluyin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gaojiluyin.ui.navigation.AppNavigation
import com.gaojiluyin.ui.theme.GaoJiLuYinTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GaoJiLuYinTheme {
                AppNavigation()
            }
        }
    }
}
