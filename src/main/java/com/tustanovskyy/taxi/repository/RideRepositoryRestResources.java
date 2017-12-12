package com.tustanovskyy.taxi.repository;

import com.tustanovskyy.taxi.document.Ride;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "ride")
public interface RideRepositoryRestResources extends CrudRepository<Ride, String>{
}
