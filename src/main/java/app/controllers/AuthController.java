package app.controllers;

import app.dtos.internal.AuthenticatedUserDTO;
import app.dtos.requests.AddRoleRequestDTO;
import app.dtos.requests.AuthRequestDTO;
import app.dtos.requests.RegisterRequestDTO;
import app.dtos.responses.AuthResponseDTO;
import app.dtos.responses.CurrentUserDTO;
import app.dtos.responses.UserDTO;
import app.exceptions.ApiException;
import app.security.AppRole;
import com.auth0.jwt.interfaces.DecodedJWT;
import app.services.AuthService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private static final String CTX_USER_KEY = "user";
    private static final String CTX_ALLOWED_ROLES_KEY = "allowed_roles";

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public void register(Context ctx) {
        RegisterRequestDTO request = ctx.bodyAsClass(RegisterRequestDTO.class);
        logger.info("Register request for username={}", request.username());
        AuthResponseDTO authResponse = authService.register(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.username(),
                request.password()
        );
        logger.info("Register success for username={} userId={}", authResponse.username(), authResponse.userId());
        ctx.status(201).json(authResponse);
    }

    public void login(Context ctx) {
        AuthRequestDTO request = ctx.bodyAsClass(AuthRequestDTO.class);
        logger.info("Login request for username={}", request.username());
        AuthResponseDTO authResponse = authService.login(request.username(), request.password());
        logger.info("Login success for username={} userId={}", authResponse.username(), authResponse.userId());
        ctx.status(200).json(authResponse);
    }

    public void addRole(Context ctx) {
        AddRoleRequestDTO request = ctx.bodyAsClass(AddRoleRequestDTO.class);
        logger.info("Add role request username={} role={}", request.username(), request.role());
        UserDTO updatedUser = authService.addRole(request.username(), request.role());
        ctx.status(200).json(updatedUser);
    }

    public void me(Context ctx) {
        AuthenticatedUserDTO authenticatedUser = getAuthenticatedUser(ctx);
        ctx.status(200).json(CurrentUserDTO.fromAuthenticatedUser(authenticatedUser));
    }

    public void logout(Context ctx) {
        String token = getToken(ctx);
        authService.logout(token);
        ctx.status(204);
    }

    public void authenticate(Context ctx) {
        if ("OPTIONS".equalsIgnoreCase(ctx.method().toString())) {
            return;
        }

        Set<String> allowedRoles = getAllowedRoles(ctx);
        if (isOpenEndpoint(allowedRoles)) {
            return;
        }

        AuthenticatedUserDTO authenticatedUser = validateAndGetUserFromToken(ctx);
        ctx.attribute(CTX_USER_KEY, authenticatedUser);
    }

    public void authorize(Context ctx) {
        Set<String> allowedRoles = getAllowedRoles(ctx);
        if (isOpenEndpoint(allowedRoles)) {
            return;
        }

        AuthenticatedUserDTO authenticatedUser = ctx.attribute(CTX_USER_KEY);
        if (authenticatedUser == null) {
            throw ApiException.forbidden("No authenticated user found in request context");
        }

        boolean hasRole = authenticatedUser.roles().stream().anyMatch(allowedRoles::contains);
        if (!hasRole) {
            throw ApiException.forbidden(
                    "User is not authorized. User roles: " + authenticatedUser.roles()
                            + ", required roles: " + allowedRoles
            );
        }
    }

    private AuthenticatedUserDTO validateAndGetUserFromToken(Context ctx) {
        String token = getToken(ctx);

        DecodedJWT decodedJWT;
        try {
            decodedJWT = authService.verifyToken(token);
        } catch (Exception e) {
            throw ApiException.unauthorized("Invalid or expired token");
        }

        String username = decodedJWT.getSubject();
        Integer userId = decodedJWT.getClaim("userId").asInt();
        List<String> rolesFromToken = decodedJWT.getClaim("roles").asList(String.class);

        if (username == null || username.isBlank() || userId == null) {
            throw ApiException.unauthorized("Invalid token payload");
        }

        Set<String> roles = new LinkedHashSet<>();
        if (rolesFromToken != null) {
            roles.addAll(rolesFromToken.stream()
                    .filter(role -> role != null && !role.isBlank())
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet()));
        }

        if (roles.isEmpty()) {
            throw ApiException.unauthorized("Token does not contain any roles");
        }

        return new AuthenticatedUserDTO(userId, username, roles);
    }

    private static String getToken(Context ctx) {
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            throw ApiException.unauthorized("Authorization header is missing");
        }

        String[] parts = authHeader.trim().split("\\s+");
        if (parts.length != 2 || !"Bearer".equalsIgnoreCase(parts[0]) || parts[1].isBlank()) {
            throw ApiException.unauthorized("Authorization header is malformed");
        }
        return parts[1];
    }

    private static boolean isOpenEndpoint(Set<String> allowedRoles) {
        return allowedRoles.isEmpty() || allowedRoles.contains(AppRole.ANYONE.name());
    }

    private static AuthenticatedUserDTO getAuthenticatedUser(Context ctx) {
        AuthenticatedUserDTO authenticatedUser = ctx.attribute(CTX_USER_KEY);
        if (authenticatedUser == null) {
            throw ApiException.unauthorized("No authenticated user found in request context");
        }
        return authenticatedUser;
    }

    private static Set<String> getAllowedRoles(Context ctx) {
        Set<?> rawAllowedRoles = ctx.attribute(CTX_ALLOWED_ROLES_KEY);
        if (rawAllowedRoles == null) {
            return Set.of();
        }

        return rawAllowedRoles.stream()
                .filter(Objects::nonNull)
                .map(role -> role.toString().toUpperCase())
                .collect(Collectors.toSet());
    }
}
