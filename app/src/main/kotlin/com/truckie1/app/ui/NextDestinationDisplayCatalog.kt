package com.truckie1.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.trips.Route

/**
 * Next destination address while Navigate is active (`ff_next_destination_display`).
 *
 * Truckie is for **human drivers**, not self-driving: the upcoming stop (or
 * route end) address is first-class alongside turn-by-turn so drivers can take
 * a better route they know.
 *
 * **Phone:** keep the chip compact / subtle (optional via this flag) so it does
 * not clutter the map. **Android Auto (ATNA-10):** when Auto navigation lands,
 * the next destination **must** show clearly on the car screen — not optional
 * chrome there.
 *
 * Out of scope: style route overlays / [MapRoutePathStyle] (ATNA-82 polyline).
 * Gherkin: features/next_destination_display.feature
 */
object NextDestinationDisplayCatalog {
    const val FLAG_KEY: String = FlagKeys.NEXT_DESTINATION_DISPLAY
    const val TEST_TAG: String = "next_destination_banner"
    const val LABEL: String = "Next"

    /** Address stays visible even if turn instructions are shown later. */
    const val showsAddressAlongsideTurnGuidance: Boolean = true
    const val replacesAddressWithTurnOnly: Boolean = false
    const val usesComposeMaterial3Banner: Boolean = true
    const val touchesLineManager: Boolean = false
    const val touchesMapRoutePathAnnotations: Boolean = false
    const val touchesMapRoutePathStyle: Boolean = false

    /** Phone map overlay stays compact so it does not clutter the handset. */
    const val phoneSurfaceIsCompact: Boolean = true
    const val phoneMaxAddressLines: Int = 1

    /**
     * Android Auto (ATNA-10): next destination must be first-class and clearly
     * visible on the car UI when Auto navigation ships. Phone remains compact.
     */
    const val androidAutoMustShowClearly: Boolean = true
    const val androidAutoTrackedBy: String = "ATNA-10"

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.NEXT_DESTINATION_DISPLAY)

    fun shouldShow(flags: FeatureFlags, navigating: Boolean): Boolean =
        isEnabled(flags) && navigating

    /**
     * Upcoming stop by [RouteStop.order]; when no intermediate stops, route end.
     * Null when not navigating or no resolvable address.
     */
    fun resolveAddress(
        route: Route?,
        navigating: Boolean,
        upcomingStopIndex: Int = 0,
    ): String? {
        if (!navigating || route == null) return null
        val ordered = route.stops.sortedBy { it.order }
        if (ordered.isNotEmpty()) {
            val idx = upcomingStopIndex.coerceIn(0, ordered.lastIndex)
            return ordered[idx].addressLabel
        }
        return route.end?.addressLabel
    }

    /** Flag-gated display address for map-home wiring. */
    fun displayAddress(
        flags: FeatureFlags,
        route: Route?,
        navigating: Boolean,
        upcomingStopIndex: Int = 0,
    ): String? {
        if (!shouldShow(flags, navigating)) return null
        return resolveAddress(route, navigating = true, upcomingStopIndex)
    }
}

/**
 * Compact phone chip for the upcoming destination address.
 * Android Auto gets a clearer surface when ATNA-10 lands — not this chip.
 */
@Composable
fun NextDestinationBanner(
    address: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(NextDestinationDisplayCatalog.TEST_TAG)
            .semantics {
                contentDescription =
                    "${NextDestinationDisplayCatalog.LABEL}: $address"
            }
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${NextDestinationDisplayCatalog.LABEL} · ",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Text(
            text = address,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = NextDestinationDisplayCatalog.phoneMaxAddressLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}
