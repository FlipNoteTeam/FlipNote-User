package flipnote.user.domain.user.grpc;

import flipnote.user.domain.user.domain.User;
import flipnote.user.domain.user.domain.UserRepository;
import flipnote.user.grpc.GetUserRequest;
import flipnote.user.grpc.GetUserResponse;
import flipnote.user.grpc.GetUsersRequest;
import flipnote.user.grpc.GetUsersResponse;
import flipnote.user.grpc.UserQueryServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class GrpcUserQueryService extends UserQueryServiceGrpc.UserQueryServiceImplBase {

    private final UserRepository userRepository;

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        try {
            User user = userRepository.findByIdAndStatus(request.getUserId(), User.Status.ACTIVE)
                    .orElse(null);

            if (user == null) {
                responseObserver.onError(
                        Status.NOT_FOUND.withDescription("사용자를 찾을 수 없습니다.").asRuntimeException()
                );
                return;
            }

            responseObserver.onNext(toResponse(user));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC getUser error. userId: {}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal error").asRuntimeException());
        }
    }

    @Override
    public void getUsers(GetUsersRequest request, StreamObserver<GetUsersResponse> responseObserver) {
        try {
            List<Long> userIds = request.getUserIdsList();
            List<User> users = userRepository.findByIdInAndStatus(userIds, User.Status.ACTIVE);

            GetUsersResponse response = GetUsersResponse.newBuilder()
                    .addAllUsers(users.stream().map(this::toResponse).toList())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC getUsers error. userIds: {}", request.getUserIdsList(), e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal error").asRuntimeException());
        }
    }

    private GetUserResponse toResponse(User user) {
        return GetUserResponse.newBuilder()
                .setId(user.getId())
                .setEmail(user.getEmail())
                .setNickname(user.getNickname())
                .setProfileImageUrl(user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "")
                .build();
    }
}
