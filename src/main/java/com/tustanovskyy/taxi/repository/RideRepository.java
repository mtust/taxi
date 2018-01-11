package com.tustanovskyy.taxi.repository;

import com.tustanovskyy.taxi.document.Ride;
import org.bson.types.ObjectId;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideRepository extends MongoRepository<Ride, ObjectId> {
    List<Ride> findByPointFromNearAndPointToNear(Point pointTo, Distance distanceTo, Point pointFrom, Distance distanceFrom);
    @Query("{pointTo : { $geoNear : {$geometry :  {type: 'Point', coordinates: [?0, ?1]}, $maxDistance : ?2}}}")
    List<Ride> findByPointToNear(double x, double y, Integer distanceTo);
    @Query("{pointFrom : { $geoNear : {$geometry :  {type: 'Point', coordinates: [?0, ?1]}, $maxDistance : ?2}}}")
    List<Ride> findByPointFromNear(double x, double y, Integer distanceFrom);

}
