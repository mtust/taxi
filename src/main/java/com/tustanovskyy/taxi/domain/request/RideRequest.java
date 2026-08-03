package com.tustanovskyy.taxi.domain.request;

import com.tustanovskyy.taxi.domain.Place;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideRequest {
    private Place placeFrom;
    private Place placeTo;
    private boolean searchOnlyFrom;
    private Integer passengerCount;
    /**
     * Both null => an immediate ("now") ride. Both set => a scheduled ride for that window -
     * validated in RideService#createRide (max window/advance from application.yml).
     */
    private LocalDateTime scheduledFrom;
    private LocalDateTime scheduledTo;
}
