package com.tustanovskyy.taxi.domain.request;

import com.tustanovskyy.taxi.domain.Place;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideRequest {

    private Place placeFrom;
    private Place placeTo;
    private Boolean isActive;
    private String userId;
}
