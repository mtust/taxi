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

/**
 * {@code uses = UserMapper.class} makes {@code User -> UserResponse} (for {@link RideDetails#user}
 * below) go through {@link UserMapper#toUserResponse} - the one place that mapping is defined -
 * instead of MapStruct generating a second, independent copy of it here that wouldn't inherit
 * that method's {@code phoneNumber} exclusion (a ride's owner is a prospective partner from the
 * caller's point of view, not the caller, so their phone number must not be exposed this way).
 */
@Mapper(componentModel = "spring", uses = UserMapper.class, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface RideMapper {

    @Mapping(target = "passengerCount", source = "passengerCount", defaultValue = "1")
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
