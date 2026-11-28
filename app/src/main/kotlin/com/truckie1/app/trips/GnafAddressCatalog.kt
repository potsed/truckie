package com.truckie1.app.trips

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/**
 * G-NAF premise geocode (`ff_gnaf_address_geocode`, ATNA-108).
 *
 * Local-first QLD sample pack (`assets/gnaf/qld_sample_addresses.json`);
 * full Geoscape G-NAF Core / state extract can replace the bootstrap JSON
 * without changing [GnafAddressIndex].
 */
object GnafAddressCatalog {
    const val FLAG: String = FlagKeys.GNAF_ADDRESS_GEOCODE
    const val ASSET_PATH: String = GnafAddressIndex.ASSET_PATH

    /** Production default off; debug dogfood enables in [com.truckie1.app.TruckieApp]. */
    const val DEFAULT_ENABLED: Boolean = false

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.GNAF_ADDRESS_GEOCODE)

    fun indexFromJson(body: String): GnafAddressIndex =
        GnafAddressIndex.parsePackJson(body)
}
