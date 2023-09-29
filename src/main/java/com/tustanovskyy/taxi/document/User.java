package com.tustanovskyy.taxi.document;


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
public class User {

    @Id
    private String id;
    private String facebookId;
    
    private String name;
    
    private String surname;
    
    private String email;
    
    private String phoneNumber;
    
    private String verificationCode;
    
    @CreatedDate
    private LocalDateTime createdDate;
    
    private LocalDateTime verificationCodeDate;
    
    private boolean registrationCompleted;
//    
//    GridFSFile photo;



//    List<Ride> historicalRides;
//    Ride activeRide;

}
