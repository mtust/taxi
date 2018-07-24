package com.tustanovskyy.taxi.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.maps.model.Geometry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Place {

    @Id
    String id;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    LocalDateTime date;

    Geometry geometry;
    String name;
    Integer distance;


}
