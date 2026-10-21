package com.pg.privacyguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pg.privacyguard.ui.PrivacyGuardApp
import com.pg.privacyguard.ui.theme.PrivacyGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrivacyGuardTheme {
                PrivacyGuardApp()
            }
        }
    }
}
