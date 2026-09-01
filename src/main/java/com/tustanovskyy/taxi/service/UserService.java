package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.Place;
import com.tustanovskyy.taxi.domain.Role;
import com.tustanovskyy.taxi.domain.request.EditUserRequest;
import com.tustanovskyy.taxi.domain.request.RecoveryPasswordRequest;
import com.tustanovskyy.taxi.domain.request.SignUpRequest;
import com.tustanovskyy.taxi.domain.response.LoginResponse;
import com.tustanovskyy.taxi.exception.ErrorCode;
import com.tustanovskyy.taxi.exception.SmsRateLimitException;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.document.Chat;
import com.tustanovskyy.taxi.mapper.RideMapper;
import com.tustanovskyy.taxi.mapper.UserMapper;
import com.tustanovskyy.taxi.repository.ChatRepository;
import com.tustanovskyy.taxi.repository.MessageRepository;
import com.tustanovskyy.taxi.repository.RatingRepository;
import com.tustanovskyy.taxi.repository.RideRepository;
import com.tustanovskyy.taxi.repository.UserRepository;
import com.tustanovskyy.taxi.security.JwtTokenUtil;
import com.tustanovskyy.taxi.service.validatior.UserValidator;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SmsService smsService;
    private final SmsRateLimiter smsRateLimiter;
    private final UserMapper userMapper;
    private final RideMapper rideMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserValidator userValidator;
    private final RideRepository rideRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final RatingRepository ratingRepository;

    @Value("${taxi.user.deletion.retention-days:30}")
    private int deletionRetentionDays;

    public User createUser(SignUpRequest request) {
        userValidator.validateSignUpRequest(request);
        // phoneNumber is uniquely indexed, so a never-verified record from a previous, abandoned
        // signup attempt (validator already confirmed !registrationCompleted for it) has to be
        // updated in place rather than inserted again - a fresh insert would collide on the index.
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .map(existing -> fillSignUpFields(existing, request))
                .orElseGet(() -> userMapper.signUpRequestToUser(request));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        var created = userRepository.save(user);
        sendUserPhoneVerification(created);
        return created;
    }

    private User fillSignUpFields(User user, SignUpRequest request) {
        return user
                .setFirstName(request.getFirstName())
                .setLastName(request.getLastName())
                .setSex(request.getSex())
                .setLanguage(request.getLanguage())
                .setEmail(request.getEmail());
    }

    public User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND, "User not found"));
    }

    public User findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND, "User not found"));
    }

    /**
     * Fetches multiple users in a single query, keyed by id. Used to avoid N+1 lookups
     * when mapping lists of entities (e.g. chat participants) to responses.
     */
    public Map<String, User> findUsersByIds(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return StreamSupport.stream(userRepository.findAllById(userIds).spliterator(), false)
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    public LoginResponse validateCode(String code, String phoneNumber) {
        User user = getUserByPhoneNumber(phoneNumber);
        if (!smsService.checkVerification(phoneNumber, code)) {
            throw new ValidationException(ErrorCode.INVALID_VERIFICATION_CODE, "Invalid verification code");
        }

        if (!user.isRegistrationCompleted()) {
            user.setRegistrationCompleted(true);
            user.setLastTimePhoneVerified(LocalDateTime.now());
            user = userRepository.save(user);
        }

        return createLoginResponse(user);
    }

    public LoginResponse login(String phoneNumber, String password) {
        // Deliberately not using getUserByPhoneNumber/USER_NOT_FOUND here: an unknown phone
        // number must look identical to a wrong password, otherwise the endpoint can be used
        // to enumerate which phone numbers are registered.
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ValidationException(ErrorCode.INVALID_CREDENTIALS, "Invalid phone number or password"));
        // Password checked before the registration-completed check below, so only someone who
        // actually knows the password can trigger a verification resend - not anyone who happens
        // to know/guess a registered-but-unverified phone number.
        userValidator.validateLogin(user, password, passwordEncoder);

        if (!user.isRegistrationCompleted()) {
            // Picks up an abandoned signup right where it left off - the FE routes this error to
            // VerificationScreen, same as a fresh signup. If a code was already sent recently the
            // resend is just skipped (rate limiter); an earlier code may still be valid, and the
            // user can resend manually from that screen once the cooldown clears.
            try {
                sendUserPhoneVerification(user);
            } catch (SmsRateLimitException e) {
                log.info("Skipping verification resend on login for {} - rate limited", phoneNumber);
            }
            throw new ValidationException(ErrorCode.PHONE_NOT_VERIFIED, "Phone number not verified");
        }

        return createLoginResponse(user);
    }

    public LoginResponse recoveryPassword(RecoveryPasswordRequest request) {
        userValidator.validateRecoveryPasswordRequest(request);
        User user = getUserByPhoneNumber(request.getPhoneNumber());

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPasswordForgot(false);
        return createLoginResponse(userRepository.save(user));
    }

    public boolean forgotPassword(String phoneNumber) {
        User user = getUserByPhoneNumber(phoneNumber);
        log.info("user with number {} forgot password", phoneNumber);
        user.setPasswordForgot(true);
        sendUserPhoneVerification(user);
        return true;
    }

    private LoginResponse createLoginResponse(User user) {
        return new LoginResponse()
                .setToken(jwtTokenUtil.generateToken(user.getPhoneNumber()))
                .setUser(userMapper.toUserResponse(user));
    }

    public User getUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND, "User not found"));
    }

    public User addHomeAddress(Place homeAddress, String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .map(user -> user.setHomeAddress(rideMapper.placeDtoToPlace(homeAddress)))
                .map(userRepository::save)
                .orElseThrow(() -> new ValidationException(ErrorCode.ADD_HOME_ADDRESS_FAILED, "failed to add home address"));
    }

    public void sendUserPhoneVerification(String phoneNumber) {
        sendUserPhoneVerification(getUserByPhoneNumber(phoneNumber));
    }

    /**
     * Sends a verification SMS to the given user, enforcing an escalating cooldown
     * (1 / 5 / 10 minutes) based on the user's recent SMS send history to prevent spam.
     */
    public void sendUserPhoneVerification(User user) {
        long secondsRemaining = smsRateLimiter.secondsUntilNextAllowedSend(user.getSmsSentAt());
        if (secondsRemaining > 0) {
            throw new SmsRateLimitException(secondsRemaining);
        }
        String status = smsService.sendVerification(user.getPhoneNumber());
        log.info("status {} of sms sending to {}", status, user.getPhoneNumber());
        user.setSmsSentAt(smsRateLimiter.registerSend(user.getSmsSentAt()));
        userRepository.save(user);
    }

    private static String getRandomNumberString() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    public User editUser(EditUserRequest editUserRequest, String phoneNumber) {
        User user = this.getUserByPhoneNumber(phoneNumber);
        user.setFirstName(editUserRequest.getFirstName());
        user.setLastName(editUserRequest.getLastName());
        user.setEmail(editUserRequest.getEmail());
        return userRepository.save(user);
    }

    public void updatePushToken(String phoneNumber, String token) {
        User user = this.getUserByPhoneNumber(phoneNumber);
        user.setPushToken(token);
        userRepository.save(user);
    }

    public void clearPushToken(String phoneNumber) {
        updatePushToken(phoneNumber, null);
    }

    /**
     * Soft-deletes a user's account: identifying info (name, email, phone, password, home
     * address) is wiped immediately and the account is flagged as deleted, which also blocks
     * further login/use of the account since every authenticated request resolves the current
     * user by the (now-changed) phone number. The account document itself, and their
     * rides/chats/messages/ratings, are kept for a retention window ({@link #deletionRetentionDays}
     * days) as a safety margin - e.g. in case the deletion was a mistake, coerced, or data is
     * needed for an active dispute - and are permanently purged afterwards by
     * {@link #purgeDeletedAccounts()}.
     */
    public void deleteUser(String phoneNumber) {
        User user = this.getUserByPhoneNumber(phoneNumber);
        String userId = user.getId();

        user.setFirstName("Deleted");
        user.setLastName("user");
        user.setEmail(null);
        user.setHomeAddress(null);
        user.setPhoneNumber("deleted-" + userId);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setSmsSentAt(null);
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Soft-deleted user {}; identifying info wiped, full purge scheduled after {} days",
                userId, deletionRetentionDays);
    }

    /**
     * Permanently purges accounts that were soft-deleted more than {@link #deletionRetentionDays}
     * days ago, along with every ride, chat, message, and rating tied to them - completing the
     * deletion promised in the Privacy Policy once the safety retention window has elapsed.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void purgeDeletedAccounts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(deletionRetentionDays);
        List<User> dueForPurge = userRepository.findByDeletedTrueAndDeletedAtBefore(cutoff);
        if (dueForPurge.isEmpty()) {
            return;
        }
        log.info("Purging {} account(s) soft-deleted before {}", dueForPurge.size(), cutoff);
        dueForPurge.forEach(this::purgeUserData);
    }

    /**
     * Not wrapped in {@code @Transactional} - it's invoked via self-reference from
     * {@link #purgeDeletedAccounts()} so Spring's proxy wouldn't apply it anyway. Deletion order
     * (rides/chats/messages/ratings, then the user document last) keeps this idempotent: if the
     * job is interrupted partway through, the user document still matches the purge query on the
     * next scheduled run and the remaining steps simply no-op/retry.
     */
    void purgeUserData(User user) {
        String userId = user.getId();

        rideRepository.deleteAll(rideRepository.findByUserId(userId));

        List<Chat> chats = chatRepository.findByParticipantIdsContaining(userId);
        if (!chats.isEmpty()) {
            List<String> chatIds = chats.stream().map(Chat::getId).toList();
            messageRepository.deleteByChatIdIn(chatIds);
            chatRepository.deleteAll(chats);
        }

        ratingRepository.deleteByRaterId(userId);
        ratingRepository.deleteByRatedUserId(userId);

        userRepository.deleteById(userId);
        log.info("Purged user {} and their remaining rides/chats/messages/ratings", userId);
    }

    public void checkAccess(String phoneNumber, String userId) {
        var currentUser = this.getUserByPhoneNumber(phoneNumber);
        if (!currentUser.getId().equals(userId) || Role.ADMIN.equals(currentUser.getRole())) {
            throw new ValidationException(ErrorCode.ACCESS_DENIED, "Access denied");
        }
    }
}

