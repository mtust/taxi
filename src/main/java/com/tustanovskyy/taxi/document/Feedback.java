package com.tustanovskyy.taxi.document;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Feedback {
    @Id
    private String id;

    private String userId;

    // Snapshotted at submission time rather than only storing userId - keeps feedback reviewable
    // as-was even if the account is later renamed or soft-deleted (see UserService#deleteUser).
    private String userFirstName;
    private String userLastName;
    private String userPhoneNumber;
    private String userEmail;

    private FeedbackType type;
    private String message;

    @CreatedDate
    private LocalDateTime createdDate;

    public enum FeedbackType {
        BUG, SUGGESTION
    }
}
