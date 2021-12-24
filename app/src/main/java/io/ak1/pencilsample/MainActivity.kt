package io.ak1.pencilsample

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.drawToBitmap
import io.ak1.pencil.*
import io.ak1.pencilsample.ui.theme.PencilTheme

class MainActivity : ComponentActivity() {
    val arr = arrayOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Magenta, Color.Cyan)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PencilTheme {
                val bitmap = remember { mutableStateOf<Bitmap?>(null) }
                val context = LocalContext.current
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Column {

                        Pencil(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f, fill = true)
                        )
                        /*bitmap.value?.let {
                            Image(
                                painter = BitmapPainter(it.asImageBitmap()),
                                contentDescription = "hi",
                                modifier = Modifier.size(200.dp)
                            )

                        }
                        Button(onClick = { bitmap.value = getBitmap() }) {

                        }*/
                        Text(text = "Alpha")
                        CustomSeekbar(10) {
                            setStrokeAlpha(it.toFloat() / 10)
                        }
                        Text(text = "Stroke Width")
                        CustomSeekbar {
                            setStrokeWidth(it.toFloat())
                        }
                        Text(text = "Colors")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(55.dp)
                        ) {
                            arr.forEach {
                                Image(
                                    painter = ColorPainter(it),
                                    contentDescription = "hi",
                                    Modifier
                                        .padding(2.dp)
                                        .fillMaxHeight()
                                        .width(55.dp)
                                        .clip(
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            setStrokeColor(it)
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
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    PencilTheme {
        Greeting("Android")
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