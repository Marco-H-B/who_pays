package com.whopays.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whopays.app.ui.theme.TitaniumBorder
import com.whopays.app.ui.theme.TitaniumMuted
import com.whopays.app.ui.theme.TitaniumSurface
import com.whopays.app.ui.theme.TitaniumWhite

/**
 * Cápsula superior unificada de 36dp inspirada en el estándar de diseño Cupertino.
 * Mantiene la misma altura homogénea en sus 3 controles interactivos.
 */
@Composable
fun HeaderCapsule(
    isMultiplayer: Boolean,
    onModeChange: (Boolean) -> Unit,
    onAuthClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Contenedor selector segmentado de modos
        Row(
            modifier = Modifier
                .height(36.dp)
                .background(
                    color = TitaniumSurface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(18.dp)
                )
                .border(1.dp, TitaniumBorder, RoundedCornerShape(18.dp))
                .padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Modo Local
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (!isMultiplayer) TitaniumWhite else Color.Transparent)
                    .clickable { onModeChange(false) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "1 Celular",
                    color = if (!isMultiplayer) Color.Black else TitaniumMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Botón Modo Mesa QR
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isMultiplayer) TitaniumWhite else Color.Transparent)
                    .clickable { onModeChange(true) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Mesa QR",
                    color = if (isMultiplayer) Color.Black else TitaniumMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Espaciador lateral sutil
        Box(modifier = Modifier.size(8.dp))

        // Botón de Perfil / Autenticación (círculo limpio de 36dp sin bordes dobles)
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(TitaniumSurface, CircleShape)
                .border(1.dp, TitaniumBorder, CircleShape)
                .clip(CircleShape)
                .clickable { onAuthClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "M",
                color = TitaniumWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
