package com.tustanovskyy.taxi.document;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document
@Data
public class User {

    @Id
    String id;

    String facebookId;
    String name;
    String surname;

    List<Ride> historicalRides;
    Ride activeRide;

}
