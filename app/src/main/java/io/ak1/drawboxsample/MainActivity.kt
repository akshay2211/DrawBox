package io.ak1.drawboxsample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.ak1.drawboxsample.helper.activityChooser
import io.ak1.drawboxsample.helper.checkAndAskPermission
import io.ak1.drawboxsample.helper.saveImage
import io.ak1.drawboxsample.ui.components.Root
import io.ak1.drawboxsample.ui.screens.HomeScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Root(window = window) {
                HomeScreen {
                    checkAndAskPermission {
                        CoroutineScope(Dispatchers.IO).launch {
                            val uri = saveImage(it)
                            withContext(Dispatchers.Main) {
                                startActivity(activityChooser(uri))
                            }
                        }
                    }
                }
            }
        }
    }
}


