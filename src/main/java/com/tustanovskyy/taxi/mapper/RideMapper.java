package com.tustanovskyy.taxi.mapper;

import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.RideDetails;
import com.tustanovskyy.taxi.domain.request.RideRequest;
import com.tustanovskyy.taxi.domain.response.RideResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.Collection;

/**
 * {@code uses = {PlaceMapper.class, UserMapper.class}}: PlaceMapper for the Place<->Place-DTO
 * conversion nested Ride fields need (moved out of this interface so UserMapper - which needs the
 * same conversion for User.homeAddress - can depend on it too, see PlaceMapper's own doc); and
 * UserMapper so {@code User -> UserResponse} (for {@link RideDetails#user} below) goes through
 * {@link UserMapper#toUserResponse} - the one place that mapping is defined - instead of
 * MapStruct generating a second, independent copy of it here that wouldn't inherit that method's
 * {@code phoneNumber} exclusion (a ride's owner is a prospective partner from the caller's point
 * of view, not the caller, so their phone number must not be exposed this way).
 */
@Mapper(componentModel = "spring", uses = {PlaceMapper.class, UserMapper.class}, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface RideMapper {

    @Mapping(target = "passengerCount", source = "passengerCount", defaultValue = "1")
    Ride rideDtoToRide(RideRequest rideRequest);

    RideResponse rideToRideDto(Ride ride);

    @Mapping(target = "id", source = "ride.id")
    @Mapping(target = "user", source = "user", nullValueCheckStrategy = NullValueCheckStrategy.ON_IMPLICIT_CONVERSION)
    RideDetails rideToRideDetailsDto(Ride ride, User user);

    Collection<RideResponse> ridesToRideDto(Collection<Ride> ride);
}
