package com.doodoobops.mahms.ui.auth

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.doodoobops.mahms.R
import com.doodoobops.mahms.ui.theme.MpiloBlue200
import com.doodoobops.mahms.ui.theme.MpiloBlue300
import com.doodoobops.mahms.ui.theme.MpiloBlue700
import com.doodoobops.mahms.ui.theme.Neutral0
import com.doodoobops.mahms.ui.theme.PublicSans

@Composable
fun PostSplashScreen(onContinue: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onContinue()
    }

    val transition = rememberInfiniteTransition(label = "logoFloat")
    val offsetY by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "translateY",
    )

    Surface(color = MpiloBlue700, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.mpilo_logo_white),
                    contentDescription = "Mpilo Central",
                    modifier = Modifier
                        .size(140.dp)
                        .graphicsLayer { translationY = offsetY * density },
                )
                Text(
                    "Mpilo Central",
                    color = Neutral0,
                    fontFamily = PublicSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    letterSpacing = (-0.01).em,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Text(
                    "Healthcare Management System",
                    color = MpiloBlue200,
                    fontFamily = PublicSans,
                    fontSize = 13.sp,
                    letterSpacing = 0.04.em,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                "\"Mpilo\" means life",
                color = MpiloBlue300,
                fontFamily = PublicSans,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
                    .alpha(0.9f),
            )
        }
    }
}
