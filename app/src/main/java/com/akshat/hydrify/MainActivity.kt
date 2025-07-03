package com.akshat.hydrify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.akshat.hydrify.ui.theme.HydrifyTheme
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import android.content.Context
import com.akshat.hydrify.DataStoreManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HydrifyTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavHost(navController = navController, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = "onboarding",
        modifier = modifier
    ) {
        composable("onboarding") { OnboardingScreen(onContinue = { navController.navigate("main") }) }
        composable("main") { MainScreen(onSettings = { navController.navigate("settings") }) }
        composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
    }
}

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    var goal by remember { mutableStateOf(2000) }
    var input by remember { mutableStateOf(goal.toString()) }
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text("Welcome to Hydrify!", style = MaterialTheme.typography.headlineMedium)
        Text("Set your daily water goal (ml):", modifier = Modifier.padding(top = 24.dp))
        androidx.compose.material3.OutlinedTextField(
            value = input,
            onValueChange = {
                input = it
                goal = it.toIntOrNull() ?: goal
            },
            label = { Text("Daily Goal (ml)") },
            singleLine = true,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        androidx.compose.material3.Button(
            onClick = {
                scope.launch {
                    DataStoreManager.setDailyGoal(context, goal)
                    DataStoreManager.setOnboarded(context, true)
                    onContinue()
                }
            },
            enabled = goal > 0
        ) {
            Text("Get Started")
        }
    }
}

@Composable
fun MainScreen(onSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dailyGoal by DataStoreManager.getDailyGoal(context).collectAsState(initial = 2000)
    val todayIntake by DataStoreManager.getTodayIntake(context).collectAsState(initial = 0)
    val progress = (todayIntake / dailyGoal.toFloat()).coerceIn(0f, 1f)
    var showChart by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Today's Intake", style = MaterialTheme.typography.titleLarge)
        Text("$todayIntake ml / $dailyGoal ml", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(vertical = 8.dp))
        CircularProgressIndicator(progress = progress, modifier = Modifier.size(120.dp), strokeWidth = 10.dp)
        androidx.compose.material3.Button(
            onClick = { scope.launch { DataStoreManager.addWaterIntake(context, 250) } },
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text("+250 ml")
        }
        androidx.compose.material3.Button(
            onClick = { showChart = !showChart },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(if (showChart) "Hide Progress Chart" else "Show Progress Chart")
        }
        if (showChart) {
            PlaceholderChart()
        }
        androidx.compose.material3.Button(
            onClick = onSettings,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Settings")
        }
    }
}

@Composable
fun PlaceholderChart() {
    val context = LocalContext.current
    val dailyGoal by DataStoreManager.getDailyGoal(context).collectAsState(initial = 2000)
    val weekData by DataStoreManager.getLast7DaysIntake(context).collectAsState(initial = List(7) { "---" to 0 })
    androidx.compose.material3.Card(
        modifier = Modifier
            .padding(top = 24.dp)
            .fillMaxWidth(0.9f)
            .height(160.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
        ) {
            Text("Last 7 Days", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            weekData.forEach { (label, value) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(label, modifier = Modifier.width(36.dp))
                    val percent = (value / dailyGoal.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .height(18.dp)
                            .width((percent * 180).dp)
                            .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                    )
                    Text("  $value ml", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dailyGoal by DataStoreManager.getDailyGoal(context).collectAsState(initial = 2000)
    var input by remember { mutableStateOf(dailyGoal.toString()) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)
        Text("Adjust your daily goal (ml):", modifier = Modifier.padding(top = 24.dp))
        androidx.compose.material3.OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Daily Goal (ml)") },
            singleLine = true,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        androidx.compose.material3.Button(
            onClick = {
                val newGoal = input.toIntOrNull() ?: dailyGoal
                scope.launch { DataStoreManager.setDailyGoal(context, newGoal) }
            },
            enabled = input.toIntOrNull() != null && input.toIntOrNull()!! > 0
        ) {
            Text("Save Goal")
        }
        androidx.compose.material3.Button(
            onClick = {
                scope.launch { DataStoreManager.setOnboarded(context, false) }
                onBack()
            },
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Reset Onboarding")
        }
        androidx.compose.material3.Button(
            onClick = onBack,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Back")
        }
    }
}