package com.tustanovskyy.taxi.repository;

import com.tustanovskyy.taxi.document.Ride;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.geo.Distance;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RideRepository extends MongoRepository<Ride, ObjectId> {

    /**
     * Geo-candidates whose search window (scheduledTo) hasn't elapsed yet - see
     * RideService#schedulesOverlap for the actual time-window intersection check applied on top.
     */
    List<Ride> findByIsActiveAndScheduledToAfterAndPlaceFromCoordinatesNear(Boolean isActive,
                                                                   LocalDateTime scheduledTo,
                                                                   GeoJsonPoint location,
                                                                   Distance distance);

    List<Ride> findByIsActiveAndScheduledToAfterAndPlaceToCoordinatesNear(Boolean isActive,
                                                                   LocalDateTime scheduledTo,
                                                                   GeoJsonPoint location,
                                                                   Distance distance);

    Collection<Ride> findByIdAndIsActive(String id, Boolean isActive);

    Collection<Ride> findByUserIdAndIsActiveOrderByDateDesc(String userId, Boolean isActive);

    /**
     * All rides ever created by a user, regardless of status - used to purge a user's data once
     * their soft-deleted account's retention window has elapsed (see UserService#purgeUserData).
     */
    List<Ride> findByUserId(String userId);

    /** Rides whose search window has ended - see RideService#deactivateOldRides. */
    List<Ride> findByIsActiveTrueAndScheduledToBefore(LocalDateTime date);

}
