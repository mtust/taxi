package com.tustanovskyy.taxi.mapper;

import com.tustanovskyy.taxi.document.Place;
import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.dto.PlaceDto;
import com.tustanovskyy.taxi.dto.RideDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.util.List;

@Mapper
public interface RideMapper {
    Ride rideDtoToRide(RideDto rideDto);
    List<Ride> rideDtosToRides(List<RideDto> rideDto);
    @Mapping(target = "coordinates", source = "point")
    Place placeDtoToPlace(PlaceDto placeDto);
    default GeoJsonPoint pointToCoordinates(Point point) {
        return new GeoJsonPoint(point);
    }

    RideDto rideToRideDto(Ride ride);
    List<RideDto> ridesToRideDtos(List<Ride> ride);
    @Mapping(target = "point", source = "coordinates")
    PlaceDto placeToPlaceDto(Place place);
    default Point coordinatesToPoint(GeoJsonPoint coordinates) {
        return new Point(coordinates);
    }
}
