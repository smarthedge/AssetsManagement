package com.assetsmanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record LoginResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("token_type")
        String tokenType,

        UserResponse user
) {
    public LoginResponse(String accessToken, UserResponse user) {
        this(accessToken, "Bearer", user);
    }
}
