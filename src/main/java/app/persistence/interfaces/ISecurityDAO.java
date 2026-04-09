package app.persistence.interfaces;

import app.entities.Role;
import app.entities.User;

public interface ISecurityDAO {
    User getVerifiedUser(String username, String password); // used for login
    User createUser(String firstName, String lastName, String email, String username, String password); // used for register
    Role createRole(String role);
    User addUserRole(String username, String role);
}
