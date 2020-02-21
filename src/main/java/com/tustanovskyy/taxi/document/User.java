package com.tustanovskyy.taxi.document;


import com.mongodb.gridfs.GridFSFile;
import lombok.Data;

import javax.persistence.*;
import java.util.List;

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
//    @Column
//    GridFSFile photo;



//    List<Ride> historicalRides;
//    Ride activeRide;

}
