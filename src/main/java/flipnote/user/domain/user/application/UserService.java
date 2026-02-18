package flipnote.user.domain.user.application;

import flipnote.user.domain.user.domain.*;
import flipnote.user.domain.user.infrastructure.JwtProvider;
import flipnote.user.domain.user.infrastructure.SessionInvalidationRepository;
import flipnote.user.domain.user.presentation.dto.request.UpdateProfileRequest;
import flipnote.user.domain.user.presentation.dto.response.MyInfoResponse;
import flipnote.user.domain.user.presentation.dto.response.UserInfoResponse;
import flipnote.user.domain.user.presentation.dto.response.UserUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final SessionInvalidationRepository sessionInvalidationRepository;
    private final JwtProvider jwtProvider;

    public MyInfoResponse getMyInfo(Long userId) {
        User user = findActiveUser(userId);
        return MyInfoResponse.from(user);
    }

    public UserInfoResponse getUserInfo(Long userId) {
        User user = userRepository.findByIdAndStatus(userId, User.Status.ACTIVE)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        return UserInfoResponse.from(user);
    }

    @Transactional
    public UserUpdateResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findActiveUser(userId);
        user.updateProfile(request.getNickname(), request.getPhone(),
                Boolean.TRUE.equals(request.getSmsAgree()), null);
        return UserUpdateResponse.from(user);
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = findActiveUser(userId);
        user.withdraw();
        sessionInvalidationRepository.invalidate(userId, jwtProvider.getRefreshTokenExpiration());
    }

    private User findActiveUser(Long userId) {
        return userRepository.findByIdAndStatus(userId, User.Status.ACTIVE)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }
}
