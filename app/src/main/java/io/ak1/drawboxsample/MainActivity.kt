package io.ak1.drawboxsample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.ak1.drawboxsample.ui.components.Root
import io.ak1.drawboxsample.ui.screens.HomeScreen


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Root(window = window) {
                HomeScreen()
            }

            /*bitmap.value?.let {
                Image(
                    painter = BitmapPainter(it.asImageBitmap()),
                    contentDescription = "hi",
                    modifier = Modifier.size(200.dp)
                )

            }*/
        }
    }
}


