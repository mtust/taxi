package com.tustanovskyy.taxi.mapper;

import com.tustanovskyy.taxi.domain.Place;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

/**
 * Place <-> Place DTO conversion (the GeoJSON-backed document shape <-> the flat lat/lng shape
 * exposed over the API). Split out of RideMapper so both RideMapper and UserMapper (User.
 * homeAddress uses this same shape) can depend on it without depending on EACH OTHER - RideMapper
 * also needs UserMapper (for RideDetails#user, see RideMapper's own @Mapper doc), and Spring Boot
 * 2.6+ rejects circular bean references by default, so that pair can only be one-directional.
 */
@Mapper(componentModel = "spring")
public interface PlaceMapper {

    @Mapping(target = "coordinates", source = "point")
    com.tustanovskyy.taxi.document.Place placeDtoToPlace(Place place);

    @Mapping(target = "point", source = "coordinates")
    Place placeToPlaceDto(com.tustanovskyy.taxi.document.Place place);

    default Point coordinatesToPoint(GeoJsonPoint coordinates) {
        return new Point(coordinates);
    }

    default GeoJsonPoint pointToCoordinates(Point point) {
        return new GeoJsonPoint(point);
    }
}
