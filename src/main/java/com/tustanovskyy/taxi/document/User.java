package com.tustanovskyy.taxi.document;


import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class User {

    @Id
    private String id;

    private String firstName;

    private String lastName;

    private String email;

    @JsonIgnore
    private String password;

    private String phoneNumber;
    @JsonIgnore
    private String verificationCode;

    @CreatedDate
    private LocalDateTime createdDate;

    @JsonIgnore
    private LocalDateTime verificationCodeDate;

    private boolean registrationCompleted;

}
