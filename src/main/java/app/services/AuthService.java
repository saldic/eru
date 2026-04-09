package app.services;

import app.dtos.responses.AuthResponseDTO;
import app.dtos.responses.UserDTO;
import app.entities.User;
import app.persistence.interfaces.ISecurityDAO;
import app.security.JwtUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final ISecurityDAO securityDAO;
    private final JwtUtil jwtUtil;

    public AuthService(ISecurityDAO securityDAO, JwtUtil jwtUtil) {
        this.securityDAO = securityDAO;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponseDTO register(String firstName, String lastName, String email, String username, String password) {
        User createdUser = securityDAO.createUser(firstName, lastName, email, username, password);
        String token = jwtUtil.generateToken(createdUser);
        logger.debug("Generated register token for userId={}", createdUser.getId());
        return new AuthResponseDTO(token, createdUser.getId(), createdUser.getUsername());
    }

    public AuthResponseDTO login(String username, String password) {
        User verifiedUser = securityDAO.getVerifiedUser(username, password);
        String token = jwtUtil.generateToken(verifiedUser);
        logger.debug("Generated login token for userId={}", verifiedUser.getId());
        return new AuthResponseDTO(token, verifiedUser.getId(), verifiedUser.getUsername());
    }

    public UserDTO addRole(String username, String role) {
        User updatedUser = securityDAO.addUserRole(username, role);
        return UserDTO.fromEntity(updatedUser);
    }

    public void logout(String token) {
        jwtUtil.revokeToken(token);
    }

    public DecodedJWT verifyToken(String token) {
        return jwtUtil.verifyToken(token);
    }
}
