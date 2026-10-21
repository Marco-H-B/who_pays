package com.whopays.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whopays.app.ui.theme.TitaniumAccent
import com.whopays.app.ui.theme.TitaniumBg
import com.whopays.app.ui.theme.TitaniumBorder
import com.whopays.app.ui.theme.TitaniumDanger
import com.whopays.app.ui.theme.TitaniumMuted
import com.whopays.app.ui.theme.TitaniumOverlay
import com.whopays.app.ui.theme.TitaniumSurface
import com.whopays.app.ui.theme.TitaniumWhite

/**
 * Modal de cobro con diseño Apple HIG Monochrome Titanium.
 * Respeta la ventana estricta de tiempo de vida (TTL) de 3:00 minutos.
 */
@Composable
fun PaywallModal(
    isVisible: Boolean,
    ttlSecondsRemaining: Int,
    onDismiss: () -> Unit,
    onPurchaseSinglePass: () -> Unit,
    onSubscribePro: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(TitaniumOverlay)
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(TitaniumSurface)
                    .border(1.dp, TitaniumBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(enabled = false) {}
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicador de arrastre superior (grabber)
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .fillMaxWidth(0.12f)
                        .background(TitaniumBorder, RoundedCornerShape(2.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Temporizador TTL de 3 minutos
                val minutes = ttlSecondsRemaining / 60
                val seconds = ttlSecondsRemaining % 60
                val timerColor = when {
                    ttlSecondsRemaining > 100 -> TitaniumAccent
                    ttlSecondsRemaining > 30 -> Color(0xFFFFD60A)
                    else -> TitaniumDanger
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .background(TitaniumBg, RoundedCornerShape(12.dp))
                        .border(1.dp, timerColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Vigencia de orden: %02d:%02d".format(minutes, seconds),
                        color = timerColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Límite de Dedos Superado",
                    color = TitaniumWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "El modo gratuito soporta hasta 5 dedos. Para partidas con 6 a 10 dedos o salas QR remotas, elige una opción:",
                    color = TitaniumMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Opción 1: Pase Rápido 1.99 USD
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(TitaniumBg)
                        .border(1.dp, TitaniumBorder, RoundedCornerShape(14.dp))
                        .clickable { onPurchaseSinglePass() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Pase de Fiesta (2 Horas)",
                            color = TitaniumWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Partidas ilimitadas durante 2 horas",
                            color = TitaniumMuted,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "$1.99 USD",
                        color = TitaniumWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Opción 2: WHO PAYS? PRO 9.99 USD / mes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(TitaniumBg)
                        .border(1.dp, TitaniumAccent, RoundedCornerShape(14.dp))
                        .clickable { onSubscribePro() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "WHO PAYS? PRO (Mensual)",
                            color = TitaniumWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Sin límites de dedos ni anuncios",
                            color = TitaniumAccent,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "$9.99/mes",
                        color = TitaniumAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Volver",
                    color = TitaniumMuted,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(8.dp)
                )
            }
        }
    }
}
