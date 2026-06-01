package com.tustanovskyy.taxi.repository;

import com.tustanovskyy.taxi.document.Rating;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RatingRepository extends MongoRepository<Rating, String> {
    boolean existsByRaterIdAndRideId(String raterId, String rideId);
}
