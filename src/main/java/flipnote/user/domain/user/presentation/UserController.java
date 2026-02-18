package flipnote.user.domain.user.presentation;

import flipnote.user.domain.user.application.UserService;
import flipnote.user.domain.user.presentation.dto.request.UpdateProfileRequest;
import flipnote.user.domain.user.presentation.dto.response.MyInfoResponse;
import flipnote.user.domain.user.presentation.dto.response.UserInfoResponse;
import flipnote.user.domain.user.presentation.dto.response.UserUpdateResponse;
import flipnote.user.global.constants.HttpConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<MyInfoResponse> getMyInfo(
            @RequestHeader(HttpConstants.USER_ID_HEADER) Long userId) {
        MyInfoResponse response = userService.getMyInfo(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserInfoResponse> getUserInfo(@PathVariable Long userId) {
        UserInfoResponse response = userService.getUserInfo(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<UserUpdateResponse> updateProfile(
            @RequestHeader(HttpConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserUpdateResponse response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> withdraw(
            @RequestHeader(HttpConstants.USER_ID_HEADER) Long userId) {
        userService.withdraw(userId);
        return ResponseEntity.noContent().build();
    }
}
