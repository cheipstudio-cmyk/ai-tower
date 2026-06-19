package com.secondream.dragracer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.filament.LightManager
import io.github.sceneview.SceneView
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CarViewer() }
    }
}

@Composable
fun CarViewer() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    Box(modifier = Modifier.fillMaxSize()) {
        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraManipulator = rememberCameraManipulator()
        ) {
            rememberModelInstance(modelLoader, "models/car.glb")?.let { instance ->
                ModelNode(modelInstance = instance, scaleToUnits = 2.0f)
            }
            LightNode(
                engine = engine,
                type = LightManager.Type.DIRECTIONAL,
                apply = {
                    intensity(150_000f)
                    direction(0.4f, -1.0f, -0.6f)
                }
            )
            LightNode(
                engine = engine,
                type = LightManager.Type.DIRECTIONAL,
                apply = {
                    intensity(80_000f)
                    direction(-0.5f, -0.3f, 0.5f)
                }
            )
        }

        Text(
            text = "Trascina per ruotare l'auto",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        )
    }
}
