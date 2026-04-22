package com.pantrypilot.ui.common;

import androidx.compose.animation.core.InfiniteTransition;
import androidx.compose.animation.core.RepeatMode;
import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.foundation.shape.RoundedCornerShape;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.AssistChipDefaults;
import androidx.compose.material3.MaterialTheme;
import androidx.compose.material3.SuggestionChipDefaults;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Color;

import com.pantrypilot.data.model.PantryItem;

public class Components {

    // ── Stock status chip ────────────────────────────────────────────────────
    @Composable
    public static void StockChip(PantryItem.StockStatus status) {
        Color bgColor, textColor;
        String label;
        ImageVector icon;

        switch (status) {
            case OUT:
                bgColor = new Color(0xFFFFEBEE);
                textColor = new Color(0xFFC62828);
                label = "Out";
                icon = Icons.Filled.Cancel;
                break;
            case LOW:
                bgColor = new Color(0xFFFFF8E1);
                textColor = new Color(0xFFF57F17);
                label = "Low";
                icon = Icons.Filled.Warning;
                break;
            default:
                bgColor = new Color(0xFFE8F5E9);
                textColor = new Color(0xFF2E7D32);
                label = "OK";
                icon = Icons.Filled.CheckCircle;
                break;
        }

        AssistChip(
                onClick = {},
                label = {Text(label, fontSize = 11.sp)},
                leadingIcon = {
                        Icon(icon, null,
                                modifier = Modifier.size(14.dp),
                                tint = textColor);
                },
        colors = AssistChipDefaults.assistChipColors(
                containerColor = bgColor,
                labelColor = textColor
        )
        )
    }

    // ── Offline banner ────────────────────────────────────────────────────────
    @Composable
    public static void OfflineBanner(boolean isOffline) {
        AnimatedVisibility(
                visible = isOffline,
                enter = slideInVertically(initialOffsetY = {-it}),
                exit = slideOutVertically(targetOffsetY = {-it})
        ) {
            Box(
                    modifier = Modifier
                            .fillMaxWidth()
                            .background(new Color(0xFFF59E0B))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
            ) {
                Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.WifiOff, null,
                            tint = new Color(0xFF1C1B1F),
                            modifier = Modifier.size(14.dp));
                    Spacer(Modifier.width(6.dp));
                    Text("Offline — changes will sync when connected",
                            style = MaterialTheme.typography.labelSmall,
                            color = new Color(0xFF1C1B1F));
                }
            }
        }
    }

    // ── Shimmer loading placeholder ───────────────────────────────────────────
    @Composable
    public static void ShimmerList(int count) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)) {
            for (int i = 0; i < count; i++) {
                ShimmerCard();
            }
        }
    }

    @Composable
    private static void ShimmerCard() {
        InfiniteTransition transition = rememberInfiniteTransition();
        float alpha = transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse)
        ).getValue();

        Box(
                modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
        );
    }

    // ── Empty state ──────────────────────────────────────────────────────────
    @Composable
    public static void EmptyState(String emoji, String heading, String ctaLabel, Runnable onCta) {
        Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 64.sp);
            Spacer(Modifier.height(16.dp));
            Text(heading,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center);
            Spacer(Modifier.height(20.dp));
            Button(onClick = onCta::run) {
                Text(ctaLabel);
            }
        }
    }

    // ── Section header ────────────────────────────────────────────────────────
    @Composable
    public static void SectionHeader(String title) {
        Text(
                title.toUpperCase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
        );
    }

    // ── Running total chip ────────────────────────────────────────────────────
    @Composable
    public static void RunningTotal(double total) {
        SuggestionChip(
                onClick = {},
                label = {Text("Total: ₹" + String.format("%.0f", total))},
                colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                )
        );
    }
}
