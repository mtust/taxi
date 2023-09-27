package com.tustanovskyy.taxi.document;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.*;
import java.sql.Timestamp;

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String facebookId;
    
    private String name;
    
    private String surname;
    
    private String email;
    
    private String phoneNumber;
    
    private String verificationCode;
    
    @CreatedDate
    private Timestamp createdDate;
    
    private Timestamp verificationCodeDate;
    
    private boolean registrationCompleted;
//    
//    GridFSFile photo;



//    List<Ride> historicalRides;
//    Ride activeRide;

}
