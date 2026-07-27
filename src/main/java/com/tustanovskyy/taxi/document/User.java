package com.tustanovskyy.taxi.document;


import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.List;
import com.tustanovskyy.taxi.domain.Role;
import com.tustanovskyy.taxi.domain.Sex;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
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

    private Place homeAddress;

    private Role role;

    private Sex sex;

    private String language;

    @JsonIgnore
    private String password;

    @Indexed(unique = true)
    private String phoneNumber;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedDate;

    @JsonIgnore
    private LocalDateTime lastTimePhoneVerified;

    /**
     * Sliding window (max 3) of timestamps when a verification SMS was sent to this user.
     * Used to enforce an escalating cooldown between resends and prevent SMS spam.
     */
    @JsonIgnore
    private List<LocalDateTime> smsSentAt;

    private boolean registrationCompleted;

    private boolean passwordForgot;
    private boolean passwordForgotDate;

    private Double averageRating;
    private Integer ratingCount;

}
