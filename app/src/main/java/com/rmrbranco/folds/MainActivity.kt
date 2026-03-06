package com.rmrbranco.folds

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rmrbranco.folds.ui.theme.FoldsTheme
import kotlin.system.exitProcess

/**
 * MainActivity is the primary entry point for the Folds application.
 *
 * This activity handles the initial setup of the app, including checking and
 * requesting permission to ignore battery optimizations, setting up the UI, and
 * managing the main content display.
 *
 * It also ensures that the app runs continuously by prompting the user to allow
 * background usage if battery optimizations are enabled for the app. If the user
 * denies this request, the app is terminated.
 *
 */

class MainActivity : ComponentActivity() {
    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar se o app está ignorando as otimizações de bateria
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // A partir do Android Marshmallow (API 23)
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)

            if (!isIgnoringBatteryOptimizations) {  // Se não estiver ignorando otimizações de bateria
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.permission_required)
                builder.setMessage(R.string.disable_battery_optimization_message)
                builder.setPositiveButton(R.string.go_to_settings) { _, _ ->
                    // Redireciona o usuário para as configurações do aplicativo
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:${packageName}")
                    startActivity(intent)
                }

                builder.setNegativeButton(R.string.cancel) { dialog, _ ->
                    // Fecha o diálogo e encerra a atividade caso o usuário cancele
                    dialog.dismiss()
                    finish()  // Finaliza a atividade atual
                    exitProcess(0)  // Encerra o aplicativo
                }

                builder.show()  // Exibe o diálogo

            } else {

                moveTaskToBack(true)

            }
        }

        enableEdgeToEdge()
        setContent {
            FoldsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = getString(R.string.app_name),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = getString(R.string.welcome_message, name),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FoldsTheme {
        Greeting("Android")
    }
}
