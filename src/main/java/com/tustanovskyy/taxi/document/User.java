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

    /** Expo push token for this user's device - never exposed to any client, only used
     * server-side to send push notifications (see ExpoPushService). Null until they register
     * one (on login), cleared on logout. Single-device only: a fresh login overwrites it. */
    @JsonIgnore
    private String pushToken;

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

    /**
     * Soft-delete flag set when the user requests account deletion. Their identifying info
     * (name/email/phone/password/home address) is wiped immediately, but the document itself -
     * and their rides/chats/messages/ratings - are only hard-deleted after a retention window
     * (see UserService#purgeDeletedAccounts), giving a safety margin before data is unrecoverable.
     */
    private boolean deleted;

    @JsonIgnore
    private LocalDateTime deletedAt;

}
