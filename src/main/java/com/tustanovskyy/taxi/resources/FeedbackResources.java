package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.domain.request.FeedbackRequest;
import com.tustanovskyy.taxi.service.FeedbackService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("feedback")
@CrossOrigin(origins = "*")
@AllArgsConstructor
@Slf4j
public class FeedbackResources {

    private final FeedbackService feedbackService;

    @PostMapping
    public void submitFeedback(@RequestBody FeedbackRequest request,
                                @AuthenticationPrincipal String phoneNumber) {
        feedbackService.submitFeedback(request, phoneNumber);
    }
}
