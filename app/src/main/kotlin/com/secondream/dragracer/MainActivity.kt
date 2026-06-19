package com.secondream.dragracer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.filament.LightManager
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DragRacer() }
    }
}

// ---- Tuning ----
private const val TILE = 2f
private val COLS = listOf(-2f, 0f, 2f)
private const val ROWS = 8
private const val FIELD = ROWS * TILE          // 16
private const val FAR = -10f                   // far edge of road field
private const val TARGET = 400f                // drag distance (meters)

private val GEAR_CEIL = listOf(16f, 26f, 36f, 45f, 54f, 62f)        // m/s ceilings
private val REV_RATE = listOf(0.55f, 0.48f, 0.42f, 0.37f, 0.32f, 0.28f) // rpm/s

@Composable
fun DragRacer() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("drag", Context.MODE_PRIVATE) }

    val carInstance = remember { modelLoader.createModelInstance(assetFileLocation = "models/car.glb") }
    val roadInstances = remember {
        List(COLS.size * ROWS) { modelLoader.createModelInstance(assetFileLocation = "models/road.glb") }
    }

    val cameraNode = rememberCameraNode(engine) {
        position = Position(x = 0f, y = 2.8f, z = 7f)
        lookAt(Position(x = 0f, y = 0.2f, z = -6f))
    }

    // ---- Game state ----
    var started by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(0f) }       // m/s
    var distance by remember { mutableStateOf(0f) }    // m
    var elapsed by remember { mutableStateOf(0f) }     // s
    var gear by remember { mutableStateOf(1) }
    var rpm by remember { mutableStateOf(0f) }
    var scroll by remember { mutableStateOf(0f) }
    var feedback by remember { mutableStateOf("") }
    var feedbackT by remember { mutableStateOf(0f) }
    var bestTime by remember { mutableStateOf(prefs.getFloat("best", 0f)) }

    fun launch() {
        started = true; finished = false
        speed = 3f; distance = 0f; elapsed = 0f; gear = 1; rpm = 0.15f; scroll = 0f
        feedback = ""; feedbackT = 0f
    }

    fun reset() {
        started = false; finished = false
        speed = 0f; distance = 0f; elapsed = 0f; gear = 1; rpm = 0f
        feedback = ""; feedbackT = 0f
    }

    fun shift() {
        if (gear >= 6) { feedback = "MAX"; feedbackT = 0.8f; return }
        val r = rpm
        when {
            r in 0.85f..1f -> { feedback = "PERFETTO!"; speed += 2.5f }
            r in 0.70f..0.85f -> { feedback = "BENE"; speed += 1.2f }
            else -> { feedback = "PRESTO"; speed += 0.3f }
        }
        gear += 1
        rpm = 0.25f
        feedbackT = 1.1f
    }

    fun onTap() {
        when {
            !started -> launch()
            finished -> reset()
            else -> shift()
        }
    }

    // ---- Frame loop ----
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
                    if (feedbackT > 0f) feedbackT -= dt
                    if (started && !finished) {
                        elapsed += dt
                        rpm += REV_RATE[gear - 1] * dt
                        val ceil = GEAR_CEIL[gear - 1]
                        speed += (ceil - speed) * 1.2f * dt
                        if (rpm >= 1f) {
                            rpm = 1f
                            speed -= speed * 0.12f * dt
                        }
                        if (speed < 0f) speed = 0f
                        distance += speed * dt
                        scroll += speed * dt
                        if (distance >= TARGET) {
                            finished = true
                            if (bestTime <= 0f || elapsed < bestTime) {
                                bestTime = elapsed
                                prefs.edit().putFloat("best", elapsed).apply()
                            }
                        }
                    }
                }
                last = now
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF05070D))) {

        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode
        ) {
            LightNode(
                engine = engine,
                type = LightManager.Type.DIRECTIONAL,
                apply = { intensity(160_000f); direction(0.4f, -1.0f, -0.6f) }
            )
            LightNode(
                engine = engine,
                type = LightManager.Type.DIRECTIONAL,
                apply = { intensity(90_000f); direction(-0.5f, -0.4f, 0.5f) }
            )

            var idx = 0
            for (c in COLS.indices) {
                for (r in 0 until ROWS) {
                    val z = FAR + ((r * TILE + scroll) % FIELD)
                    val inst = roadInstances[idx]; idx += 1
                    Node(position = Position(x = COLS[c], y = 0f, z = z)) {
                        ModelNode(modelInstance = inst)
                    }
                }
            }

            Node(position = Position(x = 0f, y = 0.4f, z = 0f)) {
                ModelNode(modelInstance = carInstance, scaleToUnits = 3f)
            }
        }

        // ---- HUD ----
        Hud(
            started = started,
            finished = finished,
            speedKmh = (speed * 3.6f).toInt(),
            distance = distance.toInt(),
            elapsed = elapsed,
            bestTime = bestTime,
            gear = gear,
            rpm = rpm,
            feedback = if (feedbackT > 0f) feedback else ""
        )

        // ---- Tap capture (above scene) ----
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }
        )
    }
}

@Composable
private fun Hud(
    started: Boolean,
    finished: Boolean,
    speedKmh: Int,
    distance: Int,
    elapsed: Float,
    bestTime: Float,
    gear: Int,
    rpm: Float,
    feedback: String
) {
    val amber = Color(0xFFD9A85C)
    Box(modifier = Modifier.fillMaxSize().padding(22.dp)) {

        // Top row: distance + time/best
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("DISTANZA", color = Color(0xFF8A93A6), fontSize = 11.sp)
                Text("$distance / 400 m", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("TEMPO", color = Color(0xFF8A93A6), fontSize = 11.sp)
                Text(fmt(elapsed) + " s", color = amber, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (bestTime > 0f) "Best " + fmt(bestTime) + " s" else "Best —",
                    color = Color(0xFF8A93A6), fontSize = 12.sp
                )
            }
        }

        // Center: speed + feedback
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!started) {
                Text("TAP per partire", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            } else if (finished) {
                Text("ARRIVATO", color = amber, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("400 m in " + fmt(elapsed) + " s", color = Color.White, fontSize = 18.sp)
                Spacer(Modifier.height(14.dp))
                Text("TAP per ricominciare", color = Color(0xFF8A93A6), fontSize = 15.sp)
            } else {
                Text(feedback, color = feedbackColor(feedback), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Bottom: speed, gear, rpm bar
        if (started && !finished) {
            Column(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$speedKmh", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                        Text(" km/h", color = Color(0xFF8A93A6), fontSize = 16.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("MARCIA", color = Color(0xFF8A93A6), fontSize = 11.sp)
                        Text("$gear", color = amber, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(10.dp))
                RpmBar(rpm)
                Spacer(Modifier.height(4.dp))
                Text("TAP = cambia marcia nella zona verde", color = Color(0xFF8A93A6), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RpmBar(rpm: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .background(Color(0xFF1A1E29), RoundedCornerShape(8.dp))
    ) {
        // green shift zone (0.70 -> 1.0) on the right 30%
        Box(
            modifier = Modifier
                .fillMaxWidth(0.30f)
                .height(16.dp)
                .align(Alignment.CenterEnd)
                .background(Color(0x3357C24A), RoundedCornerShape(8.dp))
        )
        // current rpm fill
        val fillColor = when {
            rpm >= 0.85f -> Color(0xFF57C24A)
            rpm >= 0.70f -> Color(0xFFE0C04A)
            else -> Color(0xFFB0B8C8)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(rpm.coerceIn(0f, 1f))
                .height(16.dp)
                .background(fillColor, RoundedCornerShape(8.dp))
        )
    }
}

private fun feedbackColor(f: String): Color = when (f) {
    "PERFETTO!" -> Color(0xFF57C24A)
    "BENE" -> Color(0xFFE0C04A)
    "PRESTO" -> Color(0xFFB0B8C8)
    else -> Color(0xFFE05B5B)
}

private fun fmt(v: Float): String {
    val x = (v * 100f).toInt()
    return (x / 100).toString() + "." + (x % 100).toString().padStart(2, '0')
}
