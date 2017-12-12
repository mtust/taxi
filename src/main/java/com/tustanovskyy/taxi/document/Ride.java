package com.tustanovskyy.taxi.document;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.maps.model.GeocodedWaypoint;
import com.google.maps.model.GeocodingResult;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document
@Data
public class Ride {

    @Id
    String id;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    LocalDateTime date;

    GeocodingResult addressFrom;
    GeocodingResult addressTo;

    GeocodedWaypoint geocodedWaypoint;

}
