package com.secondream.aiidler

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.pow

@Composable
fun GameScreen(
    gameLogic: GameLogic,
    audioManager: AudioManager,
    hapticManager: HapticManager
) {
    val gameState by gameLogic.gameState.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Particle system
    var particles by remember { mutableStateOf(listOf<Particle>()) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var tapPosition by remember { mutableStateOf(Offset.Zero) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000 / 60) // 60 FPS
            particles = particles.filter { it.isAlive }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background + GPU Farm visualization
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        scope.launch {
                            tapPosition = offset
                            val (gain, breakthrough) = gameLogic.tap()
                            
                            // Audio
                            audioManager.playTapSound()
                            if (breakthrough) {
                                audioManager.playBreakthroughSound()
                            }
                            
                            // Haptic
                            hapticManager.tap()
                            if (breakthrough) {
                                hapticManager.breakthrough()
                            }
                            
                            // Particles
                            repeat(12) { i ->
                                val angle = (i * 360f / 12) * PI / 180f
                                particles = particles + Particle(
                                    x = offset.x,
                                    y = offset.y,
                                    vx = cos(angle).toFloat() * 3f,
                                    vy = sin(angle).toFloat() * 3f,
                                    life = 800
                                )
                            }
                            
                            lastTapTime = System.currentTimeMillis()
                        }
                    }
                }
        ) {
            // Dark gradient background
            drawRect(Color(0xFF0B0B0D))
            
            // Animated grid
            val time = (System.currentTimeMillis() % 4000) / 1000f
            for (x in 0..20) {
                for (y in 0..20) {
                    val alpha = (sin(time + x + y) * 0.5f + 0.3f).coerceIn(0f, 1f)
                    drawCircle(
                        color = Color(0xFF4A9EFF).copy(alpha = alpha * 0.3f),
                        radius = 2f,
                        center = Offset(x * 100f, y * 100f)
                    )
                }
            }
            
            // GPU Farm (grows with upgrades)
            drawGPUFarm(gameState)
            
            // Particle rendering
            particles.forEach { particle ->
                drawCircle(
                    color = Color(0xFFD9A85C).copy(alpha = particle.alpha),
                    radius = particle.size,
                    center = Offset(particle.x, particle.y)
                )
                particle.update()
            }
            
            // Breakthrough screen flash
            val timeSinceTap = System.currentTimeMillis() - lastTapTime
            if (timeSinceTap < 300) {
                val flashAlpha = 1f - (timeSinceTap / 300f)
                drawRect(
                    color = Color(0xFFD9A85C).copy(alpha = flashAlpha * 0.4f)
                )
            }
        }

        // HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Level ${gameState.level}",
                fontSize = 32.sp,
                color = Color(0xFFD9A85C)
            )
            Text(
                text = "${formatNumber(gameState.gpu)} GPU",
                fontSize = 24.sp,
                color = Color(0xFF4A9EFF)
            )
            Text(
                text = "Per Sec: ${gameState.workers * 10}",
                fontSize = 12.sp,
                color = Color(0xFF888888)
            )
        }

        // Upgrades Panel
        UpgradesPanel(
            gameState = gameState,
            gameLogic = gameLogic,
            audioManager = audioManager,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}

@Composable
fun UpgradesPanel(
    gameState: GameState,
    gameLogic: GameLogic,
    audioManager: AudioManager,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .background(Color(0xFF1A1A1F).copy(alpha = 0.95f))
            .padding(12.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "UPGRADES",
            fontSize = 14.sp,
            color = Color(0xFFD9A85C),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        gameState.upgrades.forEach { (id, upgrade) ->
            val cost = (upgrade.baseCost * (1.15.pow(upgrade.owned.toDouble()))).toLong()
            val canAfford = gameState.gpu >= cost
            
            Button(
                onClick = {
                    if (gameLogic.buyUpgrade(id)) {
                        audioManager.playUpgradeSound()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(vertical = 4.dp),
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAfford) Color(0xFF4A9EFF) else Color(0xFF333333),
                    disabledContainerColor = Color(0xFF333333)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = upgrade.name,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Owned: ${upgrade.owned}",
                            fontSize = 10.sp,
                            color = Color(0xFFAAAAAA)
                        )
                    }
                    Text(
                        text = formatNumber(cost),
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawGPUFarm(gameState: GameState) {
    val racks = gameState.upgrades["gpu_farm"]?.owned ?: 0
    val spacing = 120f
    val startX = (size.width / 2) - (racks * spacing / 2)
    
    repeat(racks) { i ->
        val x = startX + (i * spacing)
        val y = size.height / 2
        
        // Rack glow
        drawCircle(
            color = Color(0xFF4A9EFF).copy(alpha = 0.2f),
            radius = 60f,
            center = Offset(x, y)
        )
        
        // Rack body
        drawRect(
            color = Color(0xFF1A3A52),
            topLeft = Offset(x - 25f, y - 40f),
            size = androidx.compose.ui.geometry.Size(50f, 80f)
        )
        
        // GPU lights (animated)
        val time = (System.currentTimeMillis() % 1000) / 1000f
        repeat(4) { j ->
            val lightAlpha = (sin(time + j) * 0.5f + 0.7f).coerceIn(0f, 1f)
            drawCircle(
                color = Color(0xFF00FF88).copy(alpha = lightAlpha),
                radius = 4f,
                center = Offset(x - 10f + (j % 2) * 20f, y - 30f + (j / 2) * 30f)
            )
        }
    }
}

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Int,
    var maxLife: Int = life
) {
    val alpha: Float
        get() = life.toFloat() / maxLife
    
    val size: Float
        get() = 4f * alpha
    
    val isAlive: Boolean
        get() = life > 0
    
    fun update() {
        x += vx
        y += vy
        vy += 0.2f // gravity
        life -= 16 // ~60FPS
    }
}

fun formatNumber(value: Long): String {
    return when {
        value >= 1_000_000_000 -> "%.2fB".format(value / 1_000_000_000.0)
        value >= 1_000_000 -> "%.2fM".format(value / 1_000_000.0)
        value >= 1_000 -> "%.2fK".format(value / 1_000.0)
        else -> value.toString()
    }
}
