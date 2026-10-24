package com.tustanovskyy.taxi.domain.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Real counts of other users' rides currently searching for a partner around a point - see
 * RideService#getRideStats. Shown on the FE's empty Partners states so early users can see
 * there is (or isn't yet) activity nearby; never padded with made-up numbers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideStatsResponse {
    /** Active "now" rides whose search window hasn't ended yet. */
    private long activeNow;
    /** Active user-scheduled rides (always within the next max-advance-hours, i.e. today/tomorrow). */
    private long scheduled;
    private int radiusKm;
}
