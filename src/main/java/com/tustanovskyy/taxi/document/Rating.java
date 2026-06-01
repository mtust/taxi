package com.tustanovskyy.taxi.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Rating {
    @Id
    private String id;
    private String rideId;
    private String raterId;
    private String ratedUserId;
    private int score;
    private String comment;
    @CreatedDate
    private LocalDateTime createdDate;
}
