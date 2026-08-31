package com.tustanovskyy.taxi.domain.request;

import com.tustanovskyy.taxi.document.Feedback;
import lombok.Data;

@Data
public class FeedbackRequest {
    private Feedback.FeedbackType type;
    private String message;
}
