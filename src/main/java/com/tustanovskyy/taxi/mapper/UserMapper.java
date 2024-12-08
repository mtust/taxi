package com.tustanovskyy.taxi.mapper;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.request.SignUpRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface UserMapper {

    User userDtoToUser(SignUpRequest signUpRequest);
}
