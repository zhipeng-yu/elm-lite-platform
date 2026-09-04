package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.entity.User;
import com.elmlite.platform.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegistrationRequest request) {
        User user = userService.register(request.username(), request.password(), request.displayName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(UserResponse.from(user)));
    }

    @GetMapping("/me")
    ApiResponse<UserResponse> getCurrent(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(UserResponse.from(userService.getCurrent(Long.parseLong(jwt.getSubject()))));
    }

    @PatchMapping("/me")
    ApiResponse<UserResponse> updateCurrent(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.updateDisplayName(Long.parseLong(jwt.getSubject()), request.displayName());
        return ApiResponse.success(UserResponse.from(user));
    }

    public record RegistrationRequest(
            @NotBlank(message = "用户名不能为空")
            @Size(max = 50, message = "用户名长度不能超过50位")
            String username,

            @NotBlank(message = "密码不能为空")
            @Size(min = 8, max = 72, message = "密码长度必须为8至72位")
            String password,

            @NotBlank(message = "昵称不能为空")
            @Size(max = 50, message = "昵称长度不能超过50位")
            String displayName) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "昵称不能为空")
            @Size(max = 50, message = "昵称长度不能超过50位")
            String displayName) {
    }

    public record UserResponse(Long id, String username, String displayName) {

        static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getNickname());
        }
    }
}
