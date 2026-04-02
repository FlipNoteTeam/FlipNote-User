package flipnote.user.interfaces.grpc;

import flipnote.user.application.UserService;
import flipnote.user.domain.entity.User;
import flipnote.user.grpc.GetUserRequest;
import flipnote.user.grpc.GetUserResponse;
import flipnote.user.grpc.GetUsersRequest;
import flipnote.user.grpc.GetUsersResponse;
import flipnote.user.grpc.UserQueryServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GrpcUserQueryService extends UserQueryServiceGrpc.UserQueryServiceImplBase {

    private final UserService userService;

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        User user = userService.findActiveUserById(request.getUserId())
                .orElseThrow(() -> Status.NOT_FOUND
                        .withDescription("사용자를 찾을 수 없습니다.")
                        .asRuntimeException());

        responseObserver.onNext(toResponse(user));
        responseObserver.onCompleted();
    }

    @Override
    public void getUsers(GetUsersRequest request, StreamObserver<GetUsersResponse> responseObserver) {
        List<Long> userIds = request.getUserIdsList();
        List<User> users = userService.findActiveUsersByIds(userIds);

        GetUsersResponse response = GetUsersResponse.newBuilder()
                .addAllUsers(users.stream().map(this::toResponse).toList())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
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
