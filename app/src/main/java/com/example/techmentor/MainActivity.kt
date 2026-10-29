package com.example.techmentor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.techmentor.navigation.NavGraph
import com.example.techmentor.ui.theme.TechMentorTheme
import com.example.techmentor.data.Sessao

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Sessao.iniciar(this)
        enableEdgeToEdge()

        setContent {
            TechMentorTheme {
                NavGraph()
            }
        }
    }
}