package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.domain.request.RatingRequest;
import com.tustanovskyy.taxi.domain.response.RatingResponse;
import com.tustanovskyy.taxi.service.RatingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("ratings")
@CrossOrigin(origins = "*")
@AllArgsConstructor
@Slf4j
public class RatingResources {

    private final RatingService ratingService;

    @PostMapping
    public RatingResponse createRating(@RequestBody RatingRequest request,
                                        @AuthenticationPrincipal String phoneNumber) {
        log.info("Rating ride {} by {}", request.getRideId(), phoneNumber);
        return ratingService.createRating(request, phoneNumber);
    }
}
