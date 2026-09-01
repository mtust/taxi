package com.tustanovskyy.taxi.mapper;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.request.SignUpRequest;
import com.tustanovskyy.taxi.domain.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * {@code uses = RideMapper.class} lets MapStruct reuse {@link RideMapper#placeToPlaceDto} to
 * convert {@code User.homeAddress} (document {@code Place}, GeoJSON-backed) into the domain
 * {@code Place} exposed on {@link UserResponse}.
 */
@Mapper(componentModel = "spring", uses = RideMapper.class, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", constant = "USER")
    User signUpRequestToUser(SignUpRequest signUpRequest);

    /**
     * Never fills phoneNumber, even though {@code User} has it - this is the one User->
     * UserResponse conversion used everywhere a user gets exposed to someone else (ride partners
     * via RideMapper#rideToRideDetailsDto, uses = UserMapper.class here for exactly that reason).
     * The one legitimate exception - showing a user their OWN phone number on their own profile -
     * is handled by UserService#createLoginResponse explicitly setting it on the result of this
     * method afterwards, rather than by relaxing this mapping.
     */
    @Mapping(target = "phoneNumber", ignore = true)
    UserResponse toUserResponse(User user);
}
