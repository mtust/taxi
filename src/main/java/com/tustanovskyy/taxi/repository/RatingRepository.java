package com.tustanovskyy.taxi.repository;

import com.tustanovskyy.taxi.document.Rating;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RatingRepository extends MongoRepository<Rating, String> {
    boolean existsByRaterIdAndRideId(String raterId, String rideId);

    /**
     * Purge helpers used once a soft-deleted account's retention window has elapsed (see
     * UserService#purgeUserData): ratings the user gave to others, and ratings others gave about
     * the user, are both personal data tied to that user and must not survive account deletion.
     */
    long deleteByRaterId(String raterId);

    long deleteByRatedUserId(String ratedUserId);
}
