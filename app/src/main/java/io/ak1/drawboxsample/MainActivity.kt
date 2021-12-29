package io.ak1.drawboxsample

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.ak1.drawbox.*
import io.ak1.drawboxsample.ui.theme.DrawBoxTheme
import io.ak1.drawboxsample.ui.theme.isSystemInDarkThemeCustom
import io.ak1.drawboxsample.ui.theme.statusBarConfig


val arr = arrayOf(
    Color.Black,
    Color.DarkGray,
    Color.Gray,
    Color.LightGray,
    Color.White,
    Color.Red,
    Color.Green,
    Color.Blue,
    Color.Yellow,
    Color.Cyan,
    Color.Magenta
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDark = isSystemInDarkThemeCustom()
            DrawBoxTheme(isDark) {
                window.statusBarConfig(isDark)
                val bitmap = remember { mutableStateOf<Bitmap?>(null) }
                val undoVisibility = remember { mutableStateOf(false) }
                val redoVisibility = remember { mutableStateOf(false) }
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Column {

                        DrawBox(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f, fill = false)
                        ) { undoCount, redoCount ->

                            Log.e("DrawBox", "$undoCount   $redoCount")
                            undoVisibility.value = undoCount != 0
                            redoVisibility.value = redoCount != 0
                        }
                        bitmap.value?.let {
                            Image(
                                painter = BitmapPainter(it.asImageBitmap()),
                                contentDescription = "hi",
                                modifier = Modifier.size(200.dp)
                            )

                        }
                        Row {
                            Button(onClick = {
                                Log.e("trying to ", "get the bitmap")
                                bitmap.value = getDrawBoxBitmap()
                            }) {
                                Text(text = "download")
                            }
                            Button(onClick = { unDo() }, enabled = undoVisibility.value) {
                                Text(text = "unDo")
                            }
                            Button(onClick = { reDo() }, enabled = redoVisibility.value) {
                                Text(text = "reDo")
                            }
                            Button(
                                onClick = { reset() },
                                enabled = redoVisibility.value || undoVisibility.value
                            ) {
                                Text(text = "reset")
                            }
                        }

                        Text(text = "Stroke Width")
                        CustomSeekbar {
                            setStrokeWidth(it.toFloat())
                        }
                        Text(text = "Colors")
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(47.dp)
                        ) {
                            items(arr) { item ->
                                Image(
                                    painter = ColorPainter(item),
                                    contentDescription = "hi",
                                    Modifier
                                        .padding(2.dp)
                                        .size(45.dp)
                                        .clip(
                                            CircleShape
                                        )
                                        .clickable {
                                            setStrokeColor(item)
                                        }
                                )
                            }

                        }

                    }
                }
            }
        }
    }
}


@Composable
fun CustomSeekbar(max: Int = 200, onProgressChanged: (Int) -> Unit) {
    val context = LocalContext.current
    AndroidView(
        { SeekBar(context) },
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
    ) {
        it.max = max
        it.progress = max
        it.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                Log.e("Progress", "-> $p1")
                onProgressChanged(p1)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
    }
}