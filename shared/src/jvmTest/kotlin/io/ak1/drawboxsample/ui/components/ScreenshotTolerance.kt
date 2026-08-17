package io.ak1.drawboxsample.ui.components

import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions

/**
 * Comparison tolerance for screenshot tests whose output contains rendered
 * **text**. Skia rasterizes glyphs (anti-aliasing, hinting) slightly
 * differently across operating systems, so a baseline recorded on one platform
 * drifts by a few sub-pixels against the same render on another (e.g. macOS
 * locally vs. Linux on CI). These options absorb that text drift while still
 * catching real layout/content changes.
 *
 * Use this ONLY for text-bearing snapshots. Pure-geometry snapshots stay on the
 * default byte-exact comparison and must not weaken it with this.
 */
internal val textRenderingTolerance = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(
        // Allow a small fraction of pixels to differ (glyph edges).
        changeThreshold = 0.02f,
        imageComparator = SimpleImageComparator(
            // Treat near-identical colors as equal (anti-aliasing gradients).
            maxDistance = 0.007f,
            // Tolerate a 1-2px glyph shift from hinting differences.
            vShift = 2,
            hShift = 2,
        ),
    ),
)
