package com.tustanovskyy.taxi.domain.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tustanovskyy.taxi.domain.Place;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideResponse {

    private String id;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime date;

    private Place placeFrom;
    private Place placeTo;
    private Boolean isActive;
    private String userId;
}
