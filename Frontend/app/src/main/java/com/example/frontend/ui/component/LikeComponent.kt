package com.example.frontend.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun LikeComponent(
    isLiked: Boolean,
    likeCount: Int,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    isVertical: Boolean = false,
    iconSize: Dp = 24.dp,
    unlikedTint: Color = Color.Gray,
    textColor: Color = Color.Gray
) {
    val scale = remember { Animatable(1f) }
    val sparkleScale = remember { Animatable(0f) }
    val sparkleAlpha = remember { Animatable(0f) }

    val tintColor by animateColorAsState(
        targetValue = if (isLiked) Color(0xFFF91880) else unlikedTint,
        animationSpec = tween(durationMillis = 300),
        label = "LikeColor"
    )

    val currentIcon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder

    LaunchedEffect(isLiked) {
        if (isLiked) {
            launch {
                sparkleScale.snapTo(0.5f)
                sparkleScale.animateTo(1.8f, tween(400, easing = LinearOutSlowInEasing))
            }
            launch {
                sparkleAlpha.snapTo(0.5f)
                sparkleAlpha.animateTo(0f, tween(400))
            }
            launch {
                scale.animateTo(1.4f, tween(150))
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
        } else {
            scale.animateTo(1f, tween(100))
            sparkleAlpha.snapTo(0f)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    val iconBox = @Composable {
        Box(contentAlignment = Alignment.Center) {
            if (sparkleAlpha.value > 0f) {
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .scale(sparkleScale.value)
                        .background(Color(0xFFF91880).copy(alpha = sparkleAlpha.value), CircleShape)
                )
            }

            Icon(
                imageVector = currentIcon,
                contentDescription = "Like",
                tint = tintColor,
                modifier = Modifier
                    .size(iconSize)
                    .scale(scale.value)
            )
        }
    }

    // Layout vertical handle
    if (isVertical) {
        Column(
            modifier = modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onLikeClick
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            iconBox()
            Spacer(Modifier.height(4.dp))
            Text(
                text = likeCount.toString(),
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    } else {
        Row(
            modifier = modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onLikeClick
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            iconBox()
            Spacer(Modifier.width(6.dp))
            Text(
                text = likeCount.toString(),
                fontSize = 12.sp,
                color = textColor
            )
        }
    }
}