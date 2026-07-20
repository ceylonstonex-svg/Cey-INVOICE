package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class AromaParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    val color: Color,
    var alpha: Float = 1.0f,
    var life: Float = 1.0f, // 1.0 down to 0.0
    val decay: Float = Random.nextFloat() * 0.015f + 0.008f,
    var rotation: Float = Random.nextFloat() * 360f,
    val rotationSpeed: Float = (Random.nextFloat() - 0.5f) * 4f,
    val style: Int = 0 // 0 = Circle, 1 = Leaf, 2 = Sparkle
)

enum class SpiceAroma(
    val title: String,
    val description: String,
    val baseColor: Color,
    val accentColor: Color,
    val particleStyle: Int
) {
    CINNAMON(
        "True Cinnamon",
        "Golden drifting sweet quills",
        AccentGold,
        Color(0xFFE5A93B),
        1
    ),
    CARDAMOM(
        "Pure Cardamom",
        "Uplifting cool green aroma",
        AccentEmerald,
        Color(0xFF81E6A3),
        0
    ),
    BLACK_PEPPER(
        "Black Pepper",
        "Energetic spicy amber pops",
        Color(0xFFFF7043),
        Color(0xFFFFB74D),
        2
    ),
    CLOVES(
        "Royal Cloves",
        "Aromatic purple herbal vortex",
        Color(0xFFAB47BC),
        Color(0xFFCE93D8),
        1
    )
}

@Composable
fun CeyvanaZenAromaVisualizer(
    modifier: Modifier = Modifier
) {
    var selectedSpice by remember { mutableStateOf(SpiceAroma.CINNAMON) }
    val particles = remember { mutableStateListOf<AromaParticle>() }
    val coroutineScope = rememberCoroutineScope()
    var frameTick by remember { mutableStateOf(0) }
    var isVortexEnabled by remember { mutableStateOf(false) }

    // Simulation loop updating particle positions & forces at 60 FPS
    LaunchedEffect(isVortexEnabled, selectedSpice) {
        while (true) {
            delay(16) // ~60fps
            frameTick++

            // Create gentle ambient steam stream from the bottom automatically
            if (particles.size < 45 && frameTick % 5 == 0) {
                val ambientCount = if (isVortexEnabled) 3 else 1
                repeat(ambientCount) {
                    val spawnXOffset = (Random.nextFloat() - 0.5f) * 80f
                    val baseSpeedY = if (isVortexEnabled) -3f - Random.nextFloat() * 2f else -1.2f - Random.nextFloat() * 0.8f
                    val baseSpeedX = if (isVortexEnabled) (Random.nextFloat() - 0.5f) * 6f else (Random.nextFloat() - 0.5f) * 0.6f

                    particles.add(
                        AromaParticle(
                            x = -1f, // Calculated inside Draw based on canvas size
                            y = -1f,
                            vx = baseSpeedX,
                            vy = baseSpeedY,
                            size = Random.nextFloat() * 10f + 6f,
                            color = if (Random.nextBoolean()) selectedSpice.baseColor else selectedSpice.accentColor,
                            decay = Random.nextFloat() * 0.01f + 0.005f,
                            style = selectedSpice.particleStyle
                        )
                    )
                }
            }

            // Update existing particles
            val iterator = particles.listIterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                p.life -= p.decay
                if (p.life <= 0f) {
                    iterator.remove()
                } else {
                    p.alpha = p.life

                    if (isVortexEnabled) {
                        // Swirling circular spiral gravity vortex animation
                        val dx = p.x - 300f // Assume pivot in center, will auto-adjust in draw
                        val dy = p.y - 200f
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                        if (dist > 5f) {
                            // Tangential velocity force
                            p.vx += (-dy / dist) * 0.4f
                            p.vy += (dx / dist) * 0.4f
                            // Attraction to center
                            p.vx -= (dx / dist) * 0.15f
                            p.vy -= (dy / dist) * 0.15f
                        }
                    } else {
                        // Natural organic warm thermal updraft + sin wave noise
                        p.vy -= 0.04f // Tiny continuous upwards buoyancy acceleration
                        p.vx += sin(p.life * 8f + p.y * 0.01f) * 0.15f // Wave-like sway
                    }

                    // Apply velocity damping/friction
                    p.vx *= 0.98f
                    p.vy *= 0.98f

                    // Move
                    p.x += p.vx
                    p.y += p.vy
                    p.rotation += p.rotationSpeed
                }
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ceyvana_zen_aroma_visualizer"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0B1410) // Ultra deep forest dark matching Ceyvana
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, ForestMedium.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = "Aroma Zen Logo",
                        tint = AccentGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "CEYVANA ZEN AROMA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Tap canvas to bloom authentic aroma waves",
                            fontSize = 10.sp,
                            color = ForestSage
                        )
                    }
                }

                // Interactive vortex boost switch
                IconButton(
                    onClick = {
                        isVortexEnabled = !isVortexEnabled
                        if (isVortexEnabled) {
                            // Release a massive spiral trigger
                            repeat(30) {
                                particles.add(
                                    AromaParticle(
                                        x = -1f,
                                        y = -1f,
                                        vx = (Random.nextFloat() - 0.5f) * 12f,
                                        vy = (Random.nextFloat() - 0.5f) * 12f,
                                        size = Random.nextFloat() * 12f + 8f,
                                        color = if (Random.nextBoolean()) selectedSpice.baseColor else selectedSpice.accentColor,
                                        decay = Random.nextFloat() * 0.008f + 0.003f,
                                        style = selectedSpice.particleStyle
                                    )
                                )
                            }
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isVortexEnabled) AccentGold.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f)
                    ),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (isVortexEnabled) Icons.Default.Cyclone else Icons.Default.Waves,
                        contentDescription = "Toggle Aroma Cyclone",
                        tint = if (isVortexEnabled) AccentGold else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Interactive Canvas Sandbox Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF060D0A)) // Even darker pitch green background for high contrast sparks
                    .border(1.dp, ForestPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                // Interactive tap listener on the sandbox Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(selectedSpice) {
                            detectTapGestures { offset ->
                                // Spawn a beautiful burst of particles on touch
                                repeat(16) {
                                    val angle = Random.nextFloat() * 2 * Math.PI
                                    val speed = Random.nextFloat() * 5f + 1.5f
                                    particles.add(
                                        AromaParticle(
                                            x = offset.x,
                                            y = offset.y,
                                            vx = (cos(angle) * speed).toFloat(),
                                            vy = (sin(angle) * speed).toFloat(),
                                            size = Random.nextFloat() * 10f + 5f,
                                            color = if (Random.nextBoolean()) selectedSpice.baseColor else selectedSpice.accentColor,
                                            decay = Random.nextFloat() * 0.02f + 0.01f,
                                            style = selectedSpice.particleStyle
                                        )
                                    )
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    if (w > 0 && h > 0) {
                        // Drawing logic for each active particle
                        particles.forEach { p ->
                            // Initialize coordinates if newly spawned with placeholder -1
                            if (p.x < 0f) {
                                p.x = w * 0.15f + Random.nextFloat() * w * 0.7f
                            }
                            if (p.y < 0f) {
                                p.y = h + 10f
                            }

                            // Dynamic adjustment of center for Vortex
                            val centerX = w / 2f
                            val centerY = h / 2f

                            drawParticle(p, density)
                        }

                        // Background subtle warm glow center
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    selectedSpice.baseColor.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                center = Offset(w / 2, h / 2),
                                radius = w * 0.45f
                            )
                        )
                    }
                }

                // Decorative Ambient Ring
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseRadius by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_radius"
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(110.dp)
                        .scale(pulseRadius)
                        .border(
                            width = 0.5.dp,
                            color = selectedSpice.baseColor.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                )

                // Info overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = AccentGold.copy(alpha = 0.4f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Touch Sandbox",
                        fontSize = 9.sp,
                        color = Color.LightGray.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Spice Aroma Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpiceAroma.values().forEach { spice ->
                    val isSelected = selectedSpice == spice
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            spice.baseColor.copy(alpha = 0.25f),
                                            spice.accentColor.copy(alpha = 0.15f)
                                        )
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.03f),
                                            Color.White.copy(alpha = 0.03f)
                                        )
                                    )
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) spice.baseColor.copy(alpha = 0.6f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedSpice = spice
                                // Trigger immediate pop of particles
                                repeat(12) {
                                    particles.add(
                                        AromaParticle(
                                            x = -1f,
                                            y = -1f,
                                            vx = (Random.nextFloat() - 0.5f) * 6f,
                                            vy = -2f - Random.nextFloat() * 4f,
                                            size = Random.nextFloat() * 8f + 5f,
                                            color = if (Random.nextBoolean()) spice.baseColor else spice.accentColor,
                                            decay = Random.nextFloat() * 0.015f + 0.007f,
                                            style = spice.particleStyle
                                        )
                                    )
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = spice.title.split(" ").last(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Description of active aroma
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(selectedSpice.baseColor, CircleShape)
                    )
                    Text(
                        text = "*${selectedSpice.title}*: ${selectedSpice.description}",
                        fontSize = 11.sp,
                        color = Color.LightGray.copy(alpha = 0.85f),
                        lineHeight = 14.sp
                    )
                }
            }

            // Beautiful Zen Background Music synthesizer
            val isMusicPlaying by ZenMusicManager.isPlayingFlow.collectAsState()
            val activeSoundscape by ZenMusicManager.currentSoundscape.collectAsState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C1B14), RoundedCornerShape(16.dp))
                    .border(1.dp, ForestMedium.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Header Row with title and animated Equalizer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isMusicPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                contentDescription = "Music",
                                tint = AccentGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "CEYVANA ZEN SYNTHESIZER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                        
                        // Animated equalizer bars
                        AnimatedEqualizer(isPlaying = isMusicPlaying)
                    }

                    // Soundscape Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ZenMusicManager.ZenSoundscape.values().forEach { landscape ->
                            val isSelected = activeSoundscape == landscape
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) AccentGold.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) AccentGold.copy(alpha = 0.6f) else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        ZenMusicManager.setSoundscape(landscape)
                                        if (!isMusicPlaying) {
                                            ZenMusicManager.start()
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = landscape.title,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) AccentGold else Color.Gray
                                )
                            }
                        }
                    }

                    // Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { ZenMusicManager.togglePlay() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMusicPlaying) Color(0xFFC62828) else ForestPrimary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isMusicPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isMusicPlaying) "Pause" else "Play",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isMusicPlaying) "PAUSE ZEN" else "PLAY ZEN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = activeSoundscape.desc,
                            fontSize = 10.sp,
                            color = ForestSage,
                            modifier = Modifier.weight(1f),
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedEqualizer(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 5f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 3f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h4"
    )

    Row(
        modifier = Modifier.height(24.dp).padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val bars = listOf(h1, h2, h3, h4)
        bars.forEach { height ->
            val finalHeight = if (isPlaying) height.dp else 4.dp
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(finalHeight)
                    .background(AccentGold, RoundedCornerShape(1.5.dp))
            )
        }
    }
}

// Particle rendering logic on Canvas
fun DrawScope.drawParticle(p: AromaParticle, density: Float) {
    val sizePx = p.size * density
    val col = p.color.copy(alpha = p.alpha)

    drawContext.canvas.save()
    drawContext.canvas.translate(p.x, p.y)
    drawContext.canvas.rotate(p.rotation)

    when (p.style) {
        1 -> {
            // Leaf path drawing
            val leafPath = Path().apply {
                moveTo(0f, -sizePx / 2)
                cubicTo(sizePx / 3, -sizePx / 3, sizePx / 2, -sizePx / 8, sizePx / 4, sizePx / 2)
                lineTo(0f, sizePx / 3)
                cubicTo(-sizePx / 4, sizePx / 2, -sizePx / 2, -sizePx / 8, -sizePx / 3, -sizePx / 3)
                close()
            }
            drawPath(
                path = leafPath,
                color = col
            )
        }
        2 -> {
            // Star/Sparkle path drawing
            val starPath = Path().apply {
                moveTo(0f, -sizePx)
                quadraticTo(0f, 0f, sizePx, 0f)
                quadraticTo(0f, 0f, 0f, sizePx)
                quadraticTo(0f, 0f, -sizePx, 0f)
                quadraticTo(0f, 0f, 0f, -sizePx)
                close()
            }
            drawPath(
                path = starPath,
                color = col
            )
        }
        else -> {
            // Smooth glowing circle bubble
            drawCircle(
                color = col,
                radius = sizePx / 2
            )
        }
    }
    drawContext.canvas.restore()
}
