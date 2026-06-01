package com.tustanovskyy.taxi.domain.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RatingResponse {
    private String id;
    private String rideId;
    private String raterId;
    private String ratedUserId;
    private int score;
    private String comment;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime createdDate;
}
