package com.tustanovskyy.taxi.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.geo.Point;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Place {
    private Point point;
    private String name;
    private Integer distance;
}
