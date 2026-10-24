package com.tustanovskyy.taxi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.request.EditUserRequest;
import com.tustanovskyy.taxi.domain.request.SignUpRequest;
import com.tustanovskyy.taxi.mapper.PlaceMapper;
import com.tustanovskyy.taxi.mapper.UserMapper;
import com.tustanovskyy.taxi.repository.ChatRepository;
import com.tustanovskyy.taxi.repository.MessageRepository;
import com.tustanovskyy.taxi.repository.RatingRepository;
import com.tustanovskyy.taxi.repository.RideRepository;
import com.tustanovskyy.taxi.repository.UserRepository;
import com.tustanovskyy.taxi.security.JwtTokenUtil;
import com.tustanovskyy.taxi.service.otp.OtpService;
import com.tustanovskyy.taxi.service.validatior.UserValidator;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Last name and email are optional at signup / profile edit - "not given" must be stored as null. */
class UserServiceOptionalFieldsTest {

    private static final String PHONE = "+380501234567";

    private UserRepository userRepository;
    private UserMapper userMapper;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userMapper = mock(UserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserService(userRepository, mock(OtpService.class), userMapper, mock(PlaceMapper.class),
                passwordEncoder, mock(JwtTokenUtil.class), mock(UserValidator.class), mock(RideRepository.class),
                mock(ChatRepository.class), mock(MessageRepository.class), mock(RatingRepository.class));

        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        // Mirror what the MapStruct mapper does, so the test sees exactly what reached it.
        when(userMapper.signUpRequestToUser(any())).thenAnswer(inv -> {
            SignUpRequest r = inv.getArgument(0);
            return new User().setFirstName(r.getFirstName()).setLastName(r.getLastName())
                    .setEmail(r.getEmail()).setPhoneNumber(r.getPhoneNumber());
        });
    }

    private SignUpRequest signUp(String lastName, String email) {
        return new SignUpRequest().setFirstName("Ivan").setLastName(lastName).setEmail(email)
                .setPhoneNumber(PHONE).setPassword("secret1").setPasswordRetry("secret1");
    }

    private User createdUser(SignUpRequest request) {
        userService.createUser(request, "203.0.113.7");
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void signUpWithoutLastNameAndEmailSucceeds() {
        User user = createdUser(signUp(null, null));
        assertEquals("Ivan", user.getFirstName());
        assertNull(user.getLastName());
        assertNull(user.getEmail());
    }

    @Test
    void blankLastNameAndEmailAreStoredAsNull() {
        User user = createdUser(signUp("   ", ""));
        assertNull(user.getLastName());
        assertNull(user.getEmail());
    }

    @Test
    void givenLastNameAndEmailAreTrimmedAndKept() {
        User user = createdUser(signUp(" Petrenko ", " ivan@example.com "));
        assertEquals("Petrenko", user.getLastName());
        assertEquals("ivan@example.com", user.getEmail());
    }

    @Test
    void editProfileCanClearLastNameAndEmail() {
        User existing = new User().setFirstName("Ivan").setLastName("Petrenko").setEmail("ivan@example.com");
        when(userRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.of(existing));

        User edited = userService.editUser(new EditUserRequest().setFirstName("Ivan").setLastName("").setEmail(" "), PHONE);

        assertNull(edited.getLastName());
        assertNull(edited.getEmail());
    }
}
