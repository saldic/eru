package app.services;

import app.dtos.responses.AuthResponseDTO;
import app.dtos.responses.UserDTO;
import app.entities.Role;
import app.entities.User;
import app.persistence.interfaces.ISecurityDAO;
import app.security.JwtUtil;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    @Test
    void registerShouldReturnJwtContainingUserClaims() {
        JwtUtil jwtUtil = new JwtUtil("test-secret");
        AuthService service = new AuthService(new FakeSecurityDAO(), jwtUtil);

        AuthResponseDTO response = service.register(
                "Student",
                "One",
                "student1@example.com",
                "student1",
                "secret123"
        );
        DecodedJWT decodedJWT = service.verifyToken(response.token());

        assertEquals(1, response.userId());
        assertEquals("student1", response.username());
        assertEquals("student1", decodedJWT.getSubject());
        assertEquals(1, decodedJWT.getClaim("userId").asInt());
        assertTrue(decodedJWT.getClaim("roles").asList(String.class).contains("USER"));
    }

    @Test
    void addRoleShouldReturnUpdatedUserDto() {
        JwtUtil jwtUtil = new JwtUtil("test-secret");
        AuthService service = new AuthService(new FakeSecurityDAO(), jwtUtil);

        UserDTO user = service.addRole("student1", "ADMIN");

        assertEquals("student1", user.username());
        assertEquals("student1@example.com", user.email());
        assertEquals("Student", user.firstName());
        assertEquals("One", user.lastName());
    }

    @Test
    void loginShouldReturnJwtContainingUserClaims() {
        JwtUtil jwtUtil = new JwtUtil("test-secret");
        AuthService service = new AuthService(new FakeSecurityDAO(), jwtUtil);

        AuthResponseDTO response = service.login("student1", "secret123");
        DecodedJWT decodedJWT = service.verifyToken(response.token());

        assertEquals(2, response.userId());
        assertEquals("student1", response.username());
        assertEquals("student1", decodedJWT.getSubject());
        assertEquals(2, decodedJWT.getClaim("userId").asInt());
        assertTrue(decodedJWT.getClaim("roles").asList(String.class).contains("USER"));
    }

    @Test
    void logoutShouldRevokeIssuedToken() {
        JwtUtil jwtUtil = new JwtUtil("test-secret");
        AuthService service = new AuthService(new FakeSecurityDAO(), jwtUtil);

        AuthResponseDTO response = service.login("student1", "secret123");
        service.logout(response.token());

        assertThrows(JWTVerificationException.class, () -> service.verifyToken(response.token()));
    }

    private static class FakeSecurityDAO implements ISecurityDAO {

        @Override
        public User getVerifiedUser(String username, String password) {
            return createTestUser(2, username, Set.of("USER"));
        }

        @Override
        public User createUser(String firstName, String lastName, String email, String username, String password) {
            return createTestUser(1, username, Set.of("USER"));
        }

        @Override
        public Role createRole(String role) {
            return new Role(role);
        }

        @Override
        public User addUserRole(String username, String role) {
            return createTestUser(1, username, Set.of("USER", role));
        }

        private static User createTestUser(Integer id, String username, Set<String> roles) {
            User user = new User(username, "ignored-password");
            user.setId(id);
            user.setFirstName("Student");
            user.setLastName("One");
            user.setEmail(username + "@example.com");
            user.setRoles(new HashSet<>());
            for (String role : roles) {
                user.addRole(new Role(role));
            }
            return user;
        }
    }
}
