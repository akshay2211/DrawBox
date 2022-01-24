package io.ak1.drawboxsample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.material.BottomAppBar
import androidx.compose.material.BottomNavigation
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FloatingActionButton
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
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


