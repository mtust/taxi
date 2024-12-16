package com.tustanovskyy.taxi.mapper;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.request.SignUpRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    User signUpRequestToUser(SignUpRequest signUpRequest);
}
