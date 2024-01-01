package com.tustanovskyy.taxi.repository;

import com.tustanovskyy.taxi.document.Ride;
import org.bson.types.ObjectId;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RideRepository extends MongoRepository<Ride, ObjectId> {
    List<Ride> findByPlaceFromCoordinatesNear(GeoJsonPoint location, Distance distance);
    List<Ride> findByPlaceToCoordinatesNear(GeoJsonPoint location, Distance distance);
    Collection<Ride> findByIdAndIsActive(String id, Boolean isActive);
    Collection<Ride> findByUserIdAndIsActive(String userId, Boolean isActive);

}
