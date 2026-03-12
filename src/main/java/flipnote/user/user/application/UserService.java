package flipnote.user.user.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.image.grpc.v1.ChangeImageRequest;
import flipnote.image.grpc.v1.ChangeImageResponse;
import flipnote.image.grpc.v1.GetUrlByReferenceRequest;
import flipnote.image.grpc.v1.GetUrlByReferenceResponse;
import flipnote.image.grpc.v1.ImageCommandServiceGrpc;
import flipnote.image.grpc.v1.Type;
import flipnote.user.auth.infrastructure.jwt.JwtProvider;
import flipnote.user.auth.infrastructure.redis.SessionInvalidationRepository;
import flipnote.user.global.error.ImageErrorCode;
import flipnote.user.global.exception.BizException;
import flipnote.user.user.domain.User;
import flipnote.user.user.domain.UserErrorCode;
import flipnote.user.user.domain.UserRepository;
import flipnote.user.user.presentation.dto.request.UpdateProfileRequest;
import flipnote.user.user.presentation.dto.response.MyInfoResponse;
import flipnote.user.user.presentation.dto.response.UserInfoResponse;
import flipnote.user.user.presentation.dto.response.UserUpdateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final SessionInvalidationRepository sessionInvalidationRepository;
	private final JwtProvider jwtProvider;
	private final ImageCommandServiceGrpc.ImageCommandServiceBlockingStub imageCommandServiceStub;

	public MyInfoResponse getMyInfo(Long userId) {
		User user = findActiveUser(userId);
		return MyInfoResponse.from(user);
	}

	public UserInfoResponse getUserInfo(Long userId) {
		User user = userRepository.findByIdAndStatus(userId, User.Status.ACTIVE)
			.orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND));
		return UserInfoResponse.from(user);
	}

	@Transactional
	public UserUpdateResponse updateProfile(Long userId, UpdateProfileRequest request) {
		User user = findActiveUser(userId);

		String profileImageUrl = null;
		Long imageRefId = null;
		if (request.getImageRefId() != null) {
            try {
                ChangeImageResponse changeImageResponse = imageCommandServiceStub.changeImage(
                    ChangeImageRequest.newBuilder()
                        .setReferenceType(Type.USER)
                        .setReferenceId(userId)
                        .setImageRefId(request.getImageRefId())
                        .build());

                profileImageUrl = changeImageResponse.getUrl();
                imageRefId = changeImageResponse.getImageRefId();
            } catch (Exception ex) {
				log.error("updateProfile", ex);
                throw new BizException(ImageErrorCode.IMAGE_SERVICE_ERROR);
            }
		}

		user.updateProfile(request.getNickname(), request.getPhone(), request.getSmsAgree(), profileImageUrl);
		return UserUpdateResponse.from(user, imageRefId);
	}

	@Transactional
	public void withdraw(Long userId) {
		User user = findActiveUser(userId);
		user.withdraw();
		sessionInvalidationRepository.invalidate(userId, jwtProvider.getRefreshTokenExpiration());
	}

	private User findActiveUser(Long userId) {
		return userRepository.findByIdAndStatus(userId, User.Status.ACTIVE)
			.orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND));
	}
}
