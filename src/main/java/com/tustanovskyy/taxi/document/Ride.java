package com.tustanovskyy.taxi.document;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ride {
    @Id
    String id;

    @CreatedDate
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    LocalDateTime date;

    Place placeFrom;
    Place placeTo;
    Boolean isActive;
    Long userId;

}
