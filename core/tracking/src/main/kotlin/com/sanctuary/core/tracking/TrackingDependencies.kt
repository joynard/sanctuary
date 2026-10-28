package com.sanctuary.core.tracking

import com.sanctuary.core.data.ScrollRepository
import com.sanctuary.core.data.TrackingPreferences

/**
 * Implemented by the application container. The accessibility service lives in a
 * library module, so it must not reference the app class directly.
 */
interface TrackingDependencies {
    val scrollRepository: ScrollRepository
    val trackingPreferences: TrackingPreferences
}

interface TrackingDependenciesProvider {
    val trackingDependencies: TrackingDependencies
}
