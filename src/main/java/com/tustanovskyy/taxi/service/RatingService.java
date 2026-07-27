package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.document.Rating;
import com.tustanovskyy.taxi.domain.request.RatingRequest;
import com.tustanovskyy.taxi.domain.response.RatingResponse;
import com.tustanovskyy.taxi.exception.ErrorCode;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.repository.RatingRepository;
import com.tustanovskyy.taxi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public RatingResponse createRating(RatingRequest request, String phoneNumber) {
        var rater = userService.findByPhoneNumber(phoneNumber);

        if (ratingRepository.existsByRaterIdAndRideId(rater.getId(), request.getRideId())) {
            throw new ValidationException(ErrorCode.ALREADY_RATED, "You have already rated this ride");
        }
        if (request.getScore() < 1 || request.getScore() > 5) {
            throw new ValidationException(ErrorCode.INVALID_RATING_SCORE, "Score must be between 1 and 5");
        }

        var rating = Rating.builder()
                .rideId(request.getRideId())
                .raterId(rater.getId())
                .ratedUserId(request.getRatedUserId())
                .score(request.getScore())
                .comment(request.getComment())
                .build();

        var saved = ratingRepository.save(rating);

        userRepository.findById(request.getRatedUserId()).ifPresent(user -> {
            int count = user.getRatingCount() != null ? user.getRatingCount() : 0;
            double avg = user.getAverageRating() != null ? user.getAverageRating() : 0.0;
            user.setAverageRating((avg * count + request.getScore()) / (count + 1));
            user.setRatingCount(count + 1);
            userRepository.save(user);
        });

        return RatingResponse.builder()
                .id(saved.getId())
                .rideId(saved.getRideId())
                .raterId(saved.getRaterId())
                .ratedUserId(saved.getRatedUserId())
                .score(saved.getScore())
                .comment(saved.getComment())
                .createdDate(saved.getCreatedDate())
                .build();
    }
}
