package com.example.nextscene
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.nextscene.ui.MainScreen
import com.example.nextscene.ui.theme.NextsceneTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        enableEdgeToEdge()
        setContent {
            NextsceneTheme {
                MainScreen()
            }
        }
    }
}
