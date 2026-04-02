package flipnote.user.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.image.grpc.v1.ActivateImageRequest;
import flipnote.image.grpc.v1.ActivateImageResponse;
import flipnote.image.grpc.v1.ChangeImageRequest;
import flipnote.image.grpc.v1.ChangeImageResponse;
import flipnote.image.grpc.v1.ImageCommandServiceGrpc;
import flipnote.image.grpc.v1.Type;
import flipnote.user.domain.AuthErrorCode;
import flipnote.user.domain.ImageErrorCode;
import flipnote.user.domain.TokenClaims;
import flipnote.user.domain.UserErrorCode;
import flipnote.user.domain.common.BizException;
import flipnote.user.domain.entity.User;
import flipnote.user.domain.repository.UserRepository;
import flipnote.user.infrastructure.jwt.JwtProvider;
import flipnote.user.infrastructure.redis.SessionInvalidationRepository;
import flipnote.user.interfaces.http.dto.request.UpdateProfileRequest;
import flipnote.user.interfaces.http.dto.response.MyInfoResponse;
import flipnote.user.interfaces.http.dto.response.UserInfoResponse;
import flipnote.user.interfaces.http.dto.response.UserUpdateResponse;
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
		if (request.getImageRefId() != null) {
			try {
				if (User.DEFAULT_PROFILE_IMAGE_URL.equals(user.getProfileImageUrl())) {
					ActivateImageResponse activateImageResponse = imageCommandServiceStub.activateImage(
						ActivateImageRequest.newBuilder()
							.setReferenceType(Type.USER)
							.setReferenceId(userId)
							.setImageRefId(request.getImageRefId())
							.build());

					profileImageUrl = activateImageResponse.getUrl();
				} else {
					ChangeImageResponse changeImageResponse = imageCommandServiceStub.changeImage(
						ChangeImageRequest.newBuilder()
							.setReferenceType(Type.USER)
							.setReferenceId(userId)
							.setImageRefId(request.getImageRefId())
							.build());

					profileImageUrl = changeImageResponse.getUrl();
				}
			} catch (Exception ex) {
				log.error("updateProfile", ex);
				throw new BizException(ImageErrorCode.IMAGE_SERVICE_ERROR);
			}
		}

		user.updateProfile(request.getNickname(), request.getPhone(), request.getSmsAgree(), profileImageUrl);
		return UserUpdateResponse.from(user, request.getImageRefId());
	}

	@Transactional
	public void withdraw(Long userId) {
		User user = findActiveUser(userId);
		user.withdraw();
		sessionInvalidationRepository.invalidate(userId, jwtProvider.getRefreshTokenExpiration());
	}

	public Optional<User> findActiveUserById(Long userId) {
		return userRepository.findByIdAndStatus(userId, User.Status.ACTIVE);
	}

	public List<User> findActiveUsersByIds(List<Long> userIds) {
		return userRepository.findByIdInAndStatus(userIds, User.Status.ACTIVE);
	}

	public Optional<User> findActiveUserByEmail(String email) {
		return userRepository.findByEmailAndStatus(email, User.Status.ACTIVE);
	}

	public User findUserByToken(String token) {
		if (!jwtProvider.isTokenValid(token)) {
			throw new BizException(AuthErrorCode.INVALID_TOKEN);
		}
		TokenClaims claims = jwtProvider.extractClaims(token);
		return findActiveUser(claims.userId());
	}

	private User findActiveUser(Long userId) {
		return userRepository.findByIdAndStatus(userId, User.Status.ACTIVE)
			.orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND));
	}
}
