package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.document.Feedback;
import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.request.FeedbackRequest;
import com.tustanovskyy.taxi.exception.ErrorCode;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserService userService;

    public void submitFeedback(FeedbackRequest request, String phoneNumber) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new ValidationException(ErrorCode.FEEDBACK_MESSAGE_EMPTY, "Feedback message is required");
        }
        User user = userService.findByPhoneNumber(phoneNumber);

        Feedback feedback = Feedback.builder()
                .userId(user.getId())
                .userFirstName(user.getFirstName())
                .userLastName(user.getLastName())
                .userPhoneNumber(user.getPhoneNumber())
                .userEmail(user.getEmail())
                .type(request.getType())
                .message(request.getMessage())
                .build();

        feedbackRepository.save(feedback);
        log.info("Received {} feedback from user {}", request.getType(), user.getId());
    }
}
