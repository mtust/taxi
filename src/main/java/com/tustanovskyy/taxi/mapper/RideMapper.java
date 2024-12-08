package com.tustanovskyy.taxi.mapper;

import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.Place;
import com.tustanovskyy.taxi.domain.RideDetails;
import com.tustanovskyy.taxi.domain.request.RideRequest;
import com.tustanovskyy.taxi.domain.response.RideResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.util.Collection;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface RideMapper {

    Ride rideDtoToRide(RideRequest rideRequest);


    @Mapping(target = "coordinates", source = "point")
    com.tustanovskyy.taxi.document.Place placeDtoToPlace(Place place);

    RideResponse rideToRideDto(Ride ride);

    @Mapping(target = "id", source = "ride.id")
    @Mapping(target = "user", source = "user", nullValueCheckStrategy = NullValueCheckStrategy.ON_IMPLICIT_CONVERSION)
    RideDetails rideToRideDetailsDto(Ride ride, User user);

    Collection<RideResponse> ridesToRideDto(Collection<Ride> ride);

    @Mapping(target = "point", source = "coordinates")
    Place placeToPlaceDto(com.tustanovskyy.taxi.document.Place place);

    default Point coordinatesToPoint(GeoJsonPoint coordinates) {
        return new Point(coordinates);
    }

    default GeoJsonPoint pointToCoordinates(Point point) {
        return new GeoJsonPoint(point);
    }
}
