package com.ecommerce.project.security.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseCookie;

@AllArgsConstructor
@Data
public class AuthenticationResult {
    private final UserInfoResponse response;
    private final ResponseCookie jwtCookie;
}
