package com.tustanovskyy.taxi.document;


import lombok.Data;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String facebookId;
    @Column
    private String name;
    @Column
    private String surname;
    @Column
    String email;
    @Column
    private String phoneNumber;
    @Column
    private String verificationCode;
    @Column
    @CreatedDate
    private Timestamp createdDate;
    @Column
    private Timestamp verificationCodeDate;
    @Column
    private boolean registrationCompleted;
//    @Column
//    GridFSFile photo;



//    List<Ride> historicalRides;
//    Ride activeRide;

}
