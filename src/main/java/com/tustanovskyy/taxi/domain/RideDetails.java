package com.tustanovskyy.taxi.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

import com.tustanovskyy.taxi.domain.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideDetails {

    private String id;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime date;

    private Place placeFrom;
    private Place placeTo;
    private Boolean isActive;
    private UserResponse user;
    private boolean searchOnlyFrom;
    private Integer passengerCount;
    private String agreedPartnerUserId;

    /**
     * Whether the ride's OWNER and the caller of this endpoint have an active chat with at least
     * one real (non-system) message in it - not just an auto-created, still-empty chat (opening
     * the chat screen creates the Chat document before anyone's typed anything). Only ever
     * populated by RideService#findPartnersRide (the only place that knows who "the caller" is);
     * defaults to false elsewhere (e.g. GET /rides/{id}, which has no caller-relative context).
     */
    private boolean hasChatWithMe;

    // Plain ISO-8601 (no @JsonFormat) so the FE can do date arithmetic (countdowns, re-scheduling).
    private LocalDateTime scheduledFrom;
    private LocalDateTime scheduledTo;
    private Boolean isScheduled;
}
