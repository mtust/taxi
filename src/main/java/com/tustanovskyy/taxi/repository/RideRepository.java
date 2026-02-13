package com.tustanovskyy.taxi.repository;

import com.tustanovskyy.taxi.document.Ride;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.geo.Distance;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RideRepository extends MongoRepository<Ride, ObjectId> {

    List<Ride> findByIsActiveAndDateAfterAndPlaceFromCoordinatesNear(Boolean isActive,
                                                                   LocalDateTime date,
                                                                   GeoJsonPoint location,
                                                                   Distance distance);

    List<Ride> findByIsActiveAndDateAfterAndPlaceToCoordinatesNear(Boolean isActive,
                                                                   LocalDateTime date,
                                                                   GeoJsonPoint location,
                                                                   Distance distance);

    Collection<Ride> findByIdAndIsActive(String id, Boolean isActive);

    Collection<Ride> findByUserIdAndIsActiveOrderByDateDesc(String userId, Boolean isActive);

    @Query("""
        {'placeFrom.coordinates': { $near: { $geometry: ?2, $maxDistance: ?3 } },
        'isActive': ?0, 'date': { $gt: ?1 } }
        """)
    List<Ride> findActiveByFromCoordinatesNear(Boolean isActive,
                                           LocalDateTime date,
                                           GeoJsonPoint coordinates,
                                           double maxDistance);

    @Query("""
        {'placeTo.coordinates': { $near: { $geometry: ?2, $maxDistance: ?3 } },
        'isActive': ?0, 'date': { $gt: ?1 } }
        """)
    List<Ride> findActiveByToCoordinatesNear(Boolean isActive,
                                               LocalDateTime date,
                                               GeoJsonPoint coordinates,
                                               double maxDistance);

}
