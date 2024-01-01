package com.tustanovskyy.taxi.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideDetailsDto {
    @Id
    private String id;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime date;

    private PlaceDto placeFrom;
    private PlaceDto placeTo;
    private Boolean isActive;
    private UserDto user;
}
