package com.tustanovskyy.taxi.domain.request;

import lombok.Data;

@Data
public class RatingRequest {
    private String rideId;
    private String ratedUserId;
    private int score;
    private String comment;
}
