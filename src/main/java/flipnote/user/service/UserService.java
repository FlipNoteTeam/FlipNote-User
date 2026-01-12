package flipnote.user.service;

import flipnote.user.dto.LoginRequest;
import flipnote.user.dto.LoginResponse;
import flipnote.user.dto.SignupRequest;
import flipnote.user.dto.UserResponse;
import flipnote.user.entity.User;
import flipnote.user.exception.UserException;
import flipnote.user.repository.UserRepository;
import flipnote.user.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw UserException.emailAlreadyExists();
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(UserException::invalidCredentials);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw UserException.invalidCredentials();
        }

        String token = jwtProvider.generateToken(user);
        return LoginResponse.of(token, jwtProvider.getExpiration() / 1000);
    }

    public UserResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserException::userNotFound);

        return UserResponse.from(user);
    }
}
