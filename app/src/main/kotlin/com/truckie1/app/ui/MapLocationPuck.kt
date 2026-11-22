package com.truckie1.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.maps.MapCameraTarget

/**
 * Blue my-location puck for MapLibre map home (`ff_map_location_puck`).
 * Camera and puck share the same Fused Location [LocationFix].
 * Gherkin: features/map_first_home.feature
 */
object MapLocationPuck {
    /** Standard Maps-style blue. */
    const val PUCK_COLOR_HEX: String = "#4285F4"

    const val showsBluePuck: Boolean = true
    const val centersCameraOnSameFix: Boolean = true
    const val accuracyCircleOptional: Boolean = true
    const val usesMapLibreLocationComponent: Boolean = true

    data class Resolved(
        val puck: LocationFix?,
        val camera: MapCameraTarget,
    )

    fun isEnabled(flags: FeatureFlags): Boolean = flags.isEnabled(FlagKeys.MAP_LOCATION_PUCK)

    fun shouldShow(
        flags: FeatureFlags,
        hasLocationPermission: Boolean,
        fix: LocationFix?,
    ): Boolean = isEnabled(flags) && hasLocationPermission && fix != null

    /**
     * Resolves puck visibility and camera from one fused fix.
     * Without permission the puck is hidden and the camera falls back.
     */
    fun resolve(
        fix: LocationFix?,
        hasLocationPermission: Boolean,
    ): Resolved {
        if (!hasLocationPermission || fix == null) {
            return Resolved(puck = null, camera = MapCameraTarget.fallback())
        }
        return Resolved(
            puck = fix,
            camera = MapCameraTarget.resolve(
                latitude = fix.latitude,
                longitude = fix.longitude,
            ),
        )
    }

    fun createPuckBitmap(sizePx: Int = 64): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val blue = Color.parseColor(PUCK_COLOR_HEX)
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = blue
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, sizePx * 0.42f, halo)
        canvas.drawCircle(cx, cy, sizePx * 0.30f, fill)
        return bitmap
    }
}
