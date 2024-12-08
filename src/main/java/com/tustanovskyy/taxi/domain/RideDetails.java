package com.tustanovskyy.taxi.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

import com.tustanovskyy.taxi.domain.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideDetails {

    private String id;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime date;

    private Place placeFrom;
    private Place placeTo;
    private Boolean isActive;
    private UserResponse user;
}
