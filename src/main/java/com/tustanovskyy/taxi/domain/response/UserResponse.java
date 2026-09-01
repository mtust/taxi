package com.tustanovskyy.taxi.domain.response;

import com.tustanovskyy.taxi.domain.Place;
import com.tustanovskyy.taxi.domain.Sex;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UserResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Sex sex;
    private String language;
    private Double averageRating;
    private Integer ratingCount;
    /**
     * Included so the FE can offer a "use my home address" quick-fill shortcut for ride
     * pickup/drop-off without an extra round-trip to {@code GET /users/{id}}.
     */
    private Place homeAddress;
}
