package com.tustanovskyy.taxi.document;


import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@CompoundIndexes({
    @CompoundIndex(name = "ride_user_active_date", def = "{'userId': 1, 'isActive': 1, 'date': -1}"),
    @CompoundIndex(name = "ride_active_date", def = "{'isActive': 1, 'date': 1}"),
    @CompoundIndex(name = "ride_from_geo", def = "{'placeFrom.coordinates': '2dsphere', 'isActive': 1, 'date': 1}"),
    @CompoundIndex(name = "ride_to_geo", def = "{'placeTo.coordinates': '2dsphere', 'isActive': 1, 'date': 1}")
})
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
    private boolean searchOnlyFrom;
    private Integer passengerCount;
    private String agreedPartnerUserId;
    private Boolean isCompleted;

}
