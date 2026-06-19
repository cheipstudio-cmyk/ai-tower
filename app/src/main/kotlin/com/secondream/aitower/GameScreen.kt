package com.secondream.aitower

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Biome(
    val skyTop: Color,
    val skyBottom: Color,
    val blockA: Color,
    val blockB: Color,
    val edge: Color,
    val accent: Color,
    val space: Float
)

private val ANCHORS: List<Pair<Int, Biome>> = listOf(
    0 to Biome(Color(0xFF8FD3FF), Color(0xFFE3F6FF), Color(0xFFC9A06B), Color(0xFF8B5E34), Color(0xFFF2D9A6), Color(0xFF6BBF59), 0f),
    15 to Biome(Color(0xFFFF9E6D), Color(0xFF5B3A78), Color(0xFFB5654D), Color(0xFF6E3B2E), Color(0xFFE8A07C), Color(0xFF2A2F4A), 0f),
    40 to Biome(Color(0xFF1B3A6B), Color(0xFF0B1C39), Color(0xFF5C7FB0), Color(0xFF2A4DA0), Color(0xFF9FC3FF), Color(0xFF14264A), 0f),
    80 to Biome(Color(0xFF2A0A4A), Color(0xFF0A0420), Color(0xFFFF3DAE), Color(0xFF6D1FB0), Color(0xFF00E5FF), Color(0xFFFF00C8), 0.4f),
    150 to Biome(Color(0xFF05060F), Color(0xFF000008), Color(0xFFE6ECFF), Color(0xFF7A86C0), Color(0xFFB8FFF0), Color(0xFFFFFFFF), 1f)
)

private fun lerpBiome(a: Biome, b: Biome, t: Float): Biome = Biome(
    lerp(a.skyTop, b.skyTop, t),
    lerp(a.skyBottom, b.skyBottom, t),
    lerp(a.blockA, b.blockA, t),
    lerp(a.blockB, b.blockB, t),
    lerp(a.edge, b.edge, t),
    lerp(a.accent, b.accent, t),
    a.space + (b.space - a.space) * t
)

private fun biomeFor(level: Int): Biome {
    if (level <= ANCHORS.first().first) return ANCHORS.first().second
    for (i in 0 until ANCHORS.size - 1) {
        val (h0, b0) = ANCHORS[i]
        val (h1, b1) = ANCHORS[i + 1]
        if (level <= h1) {
            val t = ((level - h0).toFloat() / (h1 - h0)).coerceIn(0f, 1f)
            return lerpBiome(b0, b1, t)
        }
    }
    return ANCHORS.last().second
}

private val Gold = Color(0xFFF2C14E)
private val CardBg = Color(0xFF141B33)

@Composable
fun GameScreen(
    audioManager: AudioManager,
    hapticManager: HapticManager,
    strings: Strings,
    lang: Lang,
    initialBest: Int,
    onLang: (Lang) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onSaveBest: (Int) -> Unit
) {
    val game = remember { TowerGame() }
    var tick by remember { mutableStateOf(0L) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    var score by remember { mutableStateOf(0) }
    var best by remember { mutableStateOf(initialBest) }
    var combo by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf(TState.READY) }
    var perfectAlpha by remember { mutableStateOf(0f) }

    var showSettings by remember { mutableStateOf(false) }
    var soundOn by remember { mutableStateOf(audioManager.enabled) }
    var hapticsOn by remember { mutableStateOf(hapticManager.enabled) }

    LaunchedEffect(Unit) { game.best = initialBest }

    LaunchedEffect(Unit) {
        var last = System.currentTimeMillis()
        var wasOver = false
        while (true) {
            kotlinx.coroutines.delay(16)
            val now = System.currentTimeMillis()
            val realDt = (now - last).coerceAtMost(64L).toFloat()
            last = now
            game.update(if (showSettings) 0f else realDt)
            tick = now
            score = game.score
            best = game.best
            combo = game.combo
            state = game.state
            perfectAlpha = game.perfectFlash
            if (game.state == TState.OVER && !wasOver) {
                wasOver = true
                if (game.justBeatBest) onSaveBest(game.best)
            }
            if (game.state != TState.OVER) wasOver = false
        }
    }

    fun handleTap() {
        if (showSettings || game.state == TState.OVER) return
        when (game.onTap()) {
            TapResult.PLACED -> { audioManager.playPlace(); hapticManager.light() }
            TapResult.PERFECT -> { audioManager.playPerfect(); hapticManager.perfect() }
            TapResult.GAMEOVER -> { audioManager.playGameOver(); hapticManager.gameOver() }
            TapResult.STARTED -> {}
            TapResult.NONE -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    canvasSize = it
                    game.configure(it.width.toFloat(), it.height.toFloat())
                }
                .pointerInput(Unit) { detectTapGestures(onTap = { handleTap() }) }
        ) {
            val w = size.width
            val h = size.height
            val frame = tick
            if (frame < 0L || w <= 0f || h <= 0f) return@Canvas

            val biome = biomeFor(game.score)

            // Sky
            drawRect(Brush.verticalGradient(listOf(biome.skyTop, biome.skyBottom)))

            // Stars (fade in for high biomes)
            if (biome.space > 0.01f) {
                for (s in STAR_FIELD) {
                    drawCircle(
                        color = Color.White.copy(alpha = biome.space * s.third),
                        radius = s.third * 2.2f,
                        center = Offset(s.first * w, s.second * h * 0.75f)
                    )
                }
            }

            // Horizon glow
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, biome.accent.copy(alpha = 0.30f)),
                    startY = h * 0.6f,
                    endY = h
                ),
                topLeft = Offset(0f, h * 0.6f),
                size = Size(w, h * 0.4f)
            )

            val camY = game.camY
            val bh = game.blockH
            val gap = 6f
            val radius = CornerRadius(12f, 12f)

            // Placed blocks
            for (b in game.blocks) {
                val top = (-b.level * bh) - camY
                if (top > h + bh || top < -bh) continue
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(biome.blockA, biome.blockB), startY = top, endY = top + bh - gap),
                    topLeft = Offset(b.x, top),
                    size = Size(b.width, bh - gap),
                    cornerRadius = radius
                )
                drawRoundRect(
                    color = biome.edge.copy(alpha = 0.55f),
                    topLeft = Offset(b.x, top),
                    size = Size(b.width, bh - gap),
                    cornerRadius = radius,
                    style = Stroke(width = 2f)
                )
            }

            // Moving block (highlighted)
            if (game.state == TState.PLAYING || game.state == TState.READY) {
                val m = game.moving
                val top = (-m.level * bh) - camY
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(biome.blockA, biome.blockB), startY = top, endY = top + bh - gap),
                    topLeft = Offset(m.x, top),
                    size = Size(m.width, bh - gap),
                    cornerRadius = radius
                )
                drawRoundRect(
                    color = biome.edge,
                    topLeft = Offset(m.x, top),
                    size = Size(m.width, bh - gap),
                    cornerRadius = radius,
                    style = Stroke(width = 3f)
                )
            }

            // Debris
            for (d in game.debris) {
                val top = d.worldY - camY
                drawRoundRect(
                    color = biome.blockB.copy(alpha = 0.9f),
                    topLeft = Offset(d.x, top),
                    size = Size(d.width, bh - gap),
                    cornerRadius = radius
                )
            }

            // Sparks
            for (s in game.sparks) {
                val a = (s.life / s.maxLife).coerceIn(0f, 1f)
                drawCircle(
                    color = biome.edge.copy(alpha = a),
                    radius = s.size * a,
                    center = Offset(s.x, s.y)
                )
            }
        }

        // HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$score", color = Color.White, fontSize = 56.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
            Text("${strings.floors}", color = Color(0xFFB8C9E6), fontSize = 13.sp, fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text("${strings.best}: $best", color = Gold, fontSize = 14.sp, fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold)
        }

        // Menu button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 20.dp, end = 16.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x4DFFFFFF))
                .clickable { showSettings = true }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("\u2630", color = Color.White, fontSize = 18.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
        }

        // Perfect popup
        if (perfectAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 230.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (combo > 1) "${strings.perfect}  ×$combo" else strings.perfect,
                    color = Gold.copy(alpha = perfectAlpha),
                    fontSize = 26.sp,
                    fontFamily = Rajdhani,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // READY overlay
        if (state == TState.READY) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 0.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(strings.tapToStart, color = Color.White, fontSize = 30.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(strings.tip, color = Color(0xFFB8C9E6), fontSize = 15.sp, fontFamily = Rajdhani)
                }
            }
        }

        // GAME OVER overlay
        if (state == TState.OVER) {
            Scrim {
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBg)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(strings.gameOver, color = Color(0xFFE06060), fontSize = 24.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    Text("$score", color = Color.White, fontSize = 52.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
                    Text(strings.floors, color = Color(0xFFB8C9E6), fontSize = 13.sp, fontFamily = Rajdhani)
                    Spacer(Modifier.height(8.dp))
                    if (game.justBeatBest) {
                        Text(strings.newBest, color = Gold, fontSize = 16.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
                    } else {
                        Text("${strings.best}: $best", color = Gold, fontSize = 15.sp, fontFamily = Rajdhani)
                    }
                    Spacer(Modifier.height(22.dp))
                    PrimaryButton(strings.restart, color = Color(0xFF2FB14C)) {
                        game.restart()
                    }
                }
            }
        }

        // SETTINGS overlay
        if (showSettings) {
            Scrim {
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBg)
                        .padding(24.dp)
                ) {
                    Text(strings.settings, color = Gold, fontSize = 22.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(18.dp))
                    ToggleRow(strings.sound, soundOn) { soundOn = it; audioManager.enabled = it; onToggleSound(it) }
                    Spacer(Modifier.height(10.dp))
                    ToggleRow(strings.vibration, hapticsOn) { hapticsOn = it; hapticManager.enabled = it; onToggleHaptics(it) }
                    Spacer(Modifier.height(18.dp))
                    Text(strings.language, color = Color(0xFFB8C9E6), fontSize = 13.sp, fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LangButton("English", lang == Lang.EN, Modifier.weight(1f)) { onLang(Lang.EN) }
                        LangButton("Italiano", lang == Lang.IT, Modifier.weight(1f)) { onLang(Lang.IT) }
                    }
                    Spacer(Modifier.height(20.dp))
                    PrimaryButton(strings.close, color = Color(0xFF33333A)) { showSettings = false }
                }
            }
        }
    }
}

@Composable
private fun Scrim(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun PrimaryButton(text: String, color: Color = Color(0xFF4A9EFF), onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LangButton(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) Color(0xFF4A9EFF) else Color(0xFF2A2F45))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (active) Color.White else Color(0xFF9FB0C8), fontSize = 15.sp, fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontFamily = Rajdhani)
        Switch(checked = value, onCheckedChange = onChange)
    }
}

private val STAR_FIELD: List<Triple<Float, Float, Float>> = buildList {
    var seed = 1234567
    repeat(70) {
        seed = (seed * 1103515245 + 12345) and 0x7fffffff
        val x = (seed % 1000) / 1000f
        seed = (seed * 1103515245 + 12345) and 0x7fffffff
        val y = (seed % 1000) / 1000f
        seed = (seed * 1103515245 + 12345) and 0x7fffffff
        val s = 0.4f + (seed % 1000) / 1000f * 0.8f
        add(Triple(x, y, s))
    }
}
