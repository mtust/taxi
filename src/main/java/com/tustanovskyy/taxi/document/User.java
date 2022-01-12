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
    Long id;
    @Column
    String facebookId;
    @Column
    String name;
    @Column
    String surname;
    @Column
    String email;
    @Column
    String phoneNumber;
    @Column
    String verificationCode;
    @Column
    @CreatedDate
    Timestamp createdDate;
    @Column
    Timestamp verificationCodeDate;
    @Column
    boolean registrationCompleted;
//    @Column
//    GridFSFile photo;



//    List<Ride> historicalRides;
//    Ride activeRide;

}
