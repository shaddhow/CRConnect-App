package com.example.crconnect.student

import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.crconnect.ProfileImageCacheHelper
import java.io.File
import androidx.compose.runtime.key
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crconnect.R
import com.example.crconnect.SessionManager
import com.example.crconnect.core.utils.HapticUtils

@Composable
fun DesignCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(28.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .clickable { onClick() }
                } else Modifier
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(padding)
        ) {
            content()
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val contentColor = if (enabled) Color.White else DisabledText
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .then(
                if (enabled) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = Color(0x660066FF),
                        spotColor = Color(0x660066FF)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = Color.White,
            disabledContainerColor = DisabledBg,
            disabledContentColor = DisabledText
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
                color = contentColor
            )
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    isDanger: Boolean = false
) {
    val contentColor = when {
        !enabled -> DisabledText
        isDanger -> ErrorColor
        else -> Primary
    }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isDanger) DangerTint else MaterialTheme.colorScheme.surface,
            contentColor = contentColor,
            disabledContainerColor = DisabledBg,
            disabledContentColor = DisabledText
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = when {
                !enabled -> DisabledBg
                isDanger -> DangerBorder
                else -> Primary
            }
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
                color = contentColor
            )
        }
    }
}

@Composable
fun TextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Primary
) {
    Text(
        text = text,
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (enabled) color else DisabledText,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
fun TopHeader(
    title: String = "Class Routine",
    name: String = "Student",
    department: String = "Dept. of CSE, BUBT",
    initials: String = "ST",
    profileImageUrl: String = "",
    isCr: Boolean = false,
    onMenuClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayTitle = title.ifBlank { "Class Routine" }
    val displayDepartment = if (department.isNotBlank()) {
        if (department.contains("CSE", ignoreCase = true) && !department.contains("BUBT", ignoreCase = true)) {
            "Dept. of CSE, BUBT"
        } else {
            department
        }
    } else {
        "Dept. of CSE, BUBT"
    }
    val displayInitials = if (initials.isNotBlank()) initials else computeInitials(name.ifBlank { "Student" })
    val isDark = LocalIsDarkTheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Menu Button: 44dp x 44dp, 24dp ic_menu icon
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 0.dp,
            onClick = onMenuClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_menu),
                    contentDescription = "Open menu",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 2. Avatar Circle: 44dp x 44dp, filled #0066FF
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(2.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            key(profileImageUrl) {
                val context = LocalContext.current
                val bitmap = remember(profileImageUrl) {
                    val path = profileImageUrl
                    try {
                        if (path.isNotBlank() && !path.startsWith("http") && !path.startsWith("data:") && !path.startsWith("content:")) {
                            val file = File(path)
                            if (file.exists() && file.length() > 0) BitmapFactory.decodeFile(file.absolutePath) else null
                        } else {
                            val session = SessionManager(context)
                            val userKey = session.userID.ifBlank { session.userEmail }
                            val cached = ProfileImageCacheHelper.getValidLocalProfilePath(context, userKey)
                            if (cached != null) BitmapFactory.decodeFile(cached) else null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else if (profileImageUrl.isNotBlank()) {
                    AndroidView(
                        factory = { ctx ->
                            ImageView(ctx).apply {
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                Glide.with(ctx)
                                    .load(profileImageUrl)
                                    .circleCrop()
                                    .signature(ObjectKey(profileImageUrl))
                                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                    .into(this)
                            }
                        },
                        update = { view ->
                            Glide.with(view.context)
                                .load(profileImageUrl)
                                .circleCrop()
                                .signature(ObjectKey(profileImageUrl))
                                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                .into(view)
                        },
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    AnimatedAvatarPlaceholder(size = 40.dp, iconSize = 20.dp)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 3. Title & Subtitle Column
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayTitle,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isCr) {
                    Spacer(modifier = Modifier.width(6.dp))
                    GlassCrBadge()
                }
            }
            Text(
                text = displayDepartment,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 4. Secure Chip
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF0D3B24) else Color(0xFFDCFCE7))
                .border(
                    1.dp,
                    if (isDark) Color(0xFF15803D).copy(alpha = 0.5f) else Color(0xFFBBF7D0),
                    CircleShape
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Secure",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 5. Power / Sign Out Button: 44dp x 44dp, 24dp ic_power icon
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 0.dp,
            onClick = onSignOutClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_power),
                    contentDescription = "Sign out",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun StudentBottomNavigation(
    selectedTab: Int,
    isGateOpen: Boolean = true,
    onTabSelected: (Int) -> Unit,
    onLockedTabClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val navSelectedColor = if (isDark) Color(0xFF3B82F6) else Color(0xFF0066FF)
    val navUnselectedColor = if (isDark) Color(0xFF2A3A52) else Color(0xFFB0BCCF)
    val surfaceColor = if (isDark) Color(0xFF111827) else Color.White
    val borderStrokeColor = if (isDark) Color(0x12FFFFFF) else Color(0x0F000000)
    val view = LocalView.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = if (isDark) 0.dp else 10.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = Color(0x1A000000),
                    spotColor = Color(0x1A000000)
                ),
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderStrokeColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val items = listOf(
                    Triple("Routine", R.drawable.ic_nav_routine, 0),
                    Triple("Election", R.drawable.ic_nav_candidates, 1),
                    Triple("Profile", R.drawable.ic_nav_profile, 2)
                )

                items.forEach { (label, iconRes, index) ->
                    val isLocked = (index == 1 && !isGateOpen)
                    val selected = selectedTab == index
                    val itemColor = if (isLocked) DisabledText else if (selected) navSelectedColor else navUnselectedColor

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                HapticUtils.lightClick(view)
                                if (isLocked) {
                                    onLockedTabClicked()
                                } else {
                                    onTabSelected(index)
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = label,
                                tint = itemColor,
                                modifier = Modifier.size(22.dp)
                            )
                            if (isLocked) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-4).dp)
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(ErrorColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color.White,
                                        modifier = Modifier.size(9.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = label,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = itemColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassCrBadge(
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val badgeBg = if (isDark) Color(0xFF381500) else Color(0xFFFEF3C7)
    val badgeBorder = if (isDark) Color(0xFF92400E).copy(alpha = 0.5f) else Color(0xFFFDE68A)
    val badgeText = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeBg)
            .border(1.dp, badgeBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "CR",
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = badgeText
        )
    }
}

@Composable
fun AnimatedAvatarPlaceholder(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AvatarPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .alpha(alpha)
            .clip(CircleShape)
            .background(Color(0xFF0066FF)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Profile",
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

