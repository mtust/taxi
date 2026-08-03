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

    UserResponse toUserResponse(User user);
}
