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
    List<Ride> findByPlaceFrom_PointNearAndPlaceTo_PointNear(Point pointTo, Distance distanceTo, Point pointFrom, Distance distanceFrom);
    @Query("{placeTo : {point : { $geoNear : {$geometry :  {type: 'Point', coordinates: [?0, ?1]}, $maxDistance : ?2}}}}")
    List<Ride> findByPlaceTo_PointNear(double x, double y, Integer distanceTo);
    @Query("{placeFrom : {point : { $geoNear : {$geometry :  {type: 'Point', coordinates: [?0, ?1]}, $maxDistance : ?2}}}}")
    List<Ride> findByPlaceFrom_PointNear(double x, double y, Integer distanceFrom);

}
