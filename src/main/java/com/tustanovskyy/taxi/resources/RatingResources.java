package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.domain.request.RatingRequest;
import com.tustanovskyy.taxi.domain.response.RatingResponse;
import com.tustanovskyy.taxi.service.ChatService;
import com.tustanovskyy.taxi.service.RatingService;
import java.util.List;
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
    private final ChatService chatService;

    @PostMapping
    public RatingResponse createRating(@RequestBody RatingRequest request,
                                        @AuthenticationPrincipal String phoneNumber) {
        log.info("Rating ride {} by {}", request.getRideId(), phoneNumber);
        RatingResponse response = ratingService.createRating(request, phoneNumber);

        // Posted into the shared chat so the rating (and any comment) is visible to both
        // participants there - otherwise it's private feedback the rated user might never see.
        chatService.sendSystemMessageToParticipants(
                List.of(response.getRaterId(), response.getRatedUserId()),
                chatService.buildRatingSubmittedContent(
                        response.getRaterId(), response.getRatedUserId(), response.getScore(), response.getComment()));

        return response;
    }
}
