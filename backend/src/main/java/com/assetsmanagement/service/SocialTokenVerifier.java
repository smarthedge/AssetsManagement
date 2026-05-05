package com.assetsmanagement.service;

import com.assetsmanagement.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Set;

@Component
public class SocialTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(SocialTokenVerifier.class);
    private static final Set<String> ALLOWED_PROVIDERS = Set.of("google", "github", "facebook", "microsoft");

    record SocialUserInfo(String providerAccountId, String email, String name) {}

    private final RestClient restClient;

    public SocialTokenVerifier(RestClient restClient) {
        this.restClient = restClient;
    }

    public SocialUserInfo verify(String provider, String token) {
        if (!ALLOWED_PROVIDERS.contains(provider)) {
            throw new BadRequestException("Unsupported provider: " + provider);
        }
        return switch (provider) {
            case "google" -> verifyGoogle(token);
            case "github" -> verifyGitHub(token);
            case "facebook" -> verifyFacebook(token);
            case "microsoft" -> verifyMicrosoft(token);
            default -> throw new BadRequestException("Unsupported provider: " + provider);
        };
    }

    private SocialUserInfo verifyGoogle(String token) {
        try {
            JsonNode response = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", token)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.has("sub")) {
                throw new BadRequestException("Invalid Google token.");
            }
            String emailVerified = response.path("email_verified").asText("false");
            if (!"true".equals(emailVerified)) {
                throw new BadRequestException("Google account email is not verified.");
            }
            return new SocialUserInfo(
                    response.get("sub").asText(),
                    response.path("email").asText(),
                    response.path("name").asText("")
            );
        } catch (RestClientException ex) {
            log.warn("Google token verification failed: {}", ex.getMessage());
            throw new BadRequestException("Invalid Google token.");
        }
    }

    private SocialUserInfo verifyGitHub(String token) {
        try {
            JsonNode response = restClient.get()
                    .uri("https://api.github.com/user")
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.has("id")) {
                throw new BadRequestException("Invalid GitHub token.");
            }
            String email = response.path("email").asText(null);
            if (email == null || email.isBlank()) {
                email = fetchGitHubPrimaryEmail(token);
            }
            return new SocialUserInfo(
                    String.valueOf(response.get("id").asLong()),
                    email,
                    response.path("name").asText("")
            );
        } catch (RestClientException ex) {
            log.warn("GitHub token verification failed: {}", ex.getMessage());
            throw new BadRequestException("Invalid GitHub token.");
        }
    }

    private String fetchGitHubPrimaryEmail(String token) {
        try {
            JsonNode emails = restClient.get()
                    .uri("https://api.github.com/user/emails")
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .body(JsonNode.class);
            if (emails != null && emails.isArray()) {
                for (JsonNode entry : emails) {
                    if (entry.path("primary").asBoolean(false) && entry.path("verified").asBoolean(false)) {
                        return entry.path("email").asText();
                    }
                }
            }
        } catch (RestClientException ex) {
            log.warn("GitHub email fetch failed: {}", ex.getMessage());
        }
        throw new BadRequestException("Unable to retrieve email from GitHub. Ensure email permission was granted.");
    }

    private SocialUserInfo verifyFacebook(String token) {
        try {
            JsonNode response = restClient.get()
                    .uri("https://graph.facebook.com/me?fields=id,email,name&access_token={token}", token)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.has("id")) {
                throw new BadRequestException("Invalid Facebook token.");
            }
            String email = response.path("email").asText(null);
            if (email == null || email.isBlank()) {
                throw new BadRequestException("Unable to retrieve email from Facebook. Ensure email permission was granted.");
            }
            return new SocialUserInfo(
                    response.get("id").asText(),
                    email,
                    response.path("name").asText("")
            );
        } catch (RestClientException ex) {
            log.warn("Facebook token verification failed: {}", ex.getMessage());
            throw new BadRequestException("Invalid Facebook token.");
        }
    }

    private SocialUserInfo verifyMicrosoft(String token) {
        try {
            JsonNode response = restClient.get()
                    .uri("https://graph.microsoft.com/v1.0/me")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.has("id")) {
                throw new BadRequestException("Invalid Microsoft token.");
            }
            String email = response.path("mail").asText(null);
            if (email == null || email.isBlank()) {
                email = response.path("userPrincipalName").asText(null);
            }
            if (email == null || email.isBlank()) {
                throw new BadRequestException("Unable to retrieve email from Microsoft account.");
            }
            return new SocialUserInfo(
                    response.get("id").asText(),
                    email,
                    response.path("displayName").asText("")
            );
        } catch (RestClientException ex) {
            log.warn("Microsoft token verification failed: {}", ex.getMessage());
            throw new BadRequestException("Invalid Microsoft token.");
        }
    }
}
