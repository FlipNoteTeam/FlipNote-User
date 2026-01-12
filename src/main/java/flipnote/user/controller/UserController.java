package flipnote.user.controller;

import flipnote.user.dto.LoginRequest;
import flipnote.user.dto.LoginResponse;
import flipnote.user.dto.SignupRequest;
import flipnote.user.dto.UserResponse;
import flipnote.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        UserResponse response = userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(@RequestHeader("X-User-Id") Long userId) {
        UserResponse response = userService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getProfile(@PathVariable Long id) {
        UserResponse response = userService.getProfile(id);
        return ResponseEntity.ok(response);
    }
}
