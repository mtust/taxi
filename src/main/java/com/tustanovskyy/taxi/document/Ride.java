package com.tustanovskyy.taxi.document;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ride {
    @Id
    private String id;

    @CreatedDate
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime date;

    private Place placeFrom;
    private Place placeTo;
    private Boolean isActive;
    private String userId;

}
