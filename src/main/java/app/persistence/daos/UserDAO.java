package app.persistence.daos;

import app.config.hibernate.HibernateConfig;
import app.entities.Role;
import app.entities.User;
import app.exceptions.ApiException;
import app.exceptions.DatabaseException;
import app.exceptions.enums.DatabaseErrorType;
import app.persistence.interfaces.IDAO;
import app.persistence.interfaces.ISecurityDAO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;

import java.util.List;
import java.util.Optional;

public class UserDAO implements IDAO<User, Integer>, ISecurityDAO {
    private final EntityManagerFactory emf;

    public UserDAO() {
        this(HibernateConfig.getEntityManagerFactory());
    }

    public UserDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public User create(User user) {
        if (user == null) {
            throw new DatabaseException("User cannot be null", DatabaseErrorType.VALIDATION);
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
            return user;
        } catch (PersistenceException e) {
            throw new DatabaseException("Create user failed", DatabaseErrorType.TRANSACTION_FAILURE, e);
        } catch (RuntimeException e) {
            throw new DatabaseException("Create user failed", DatabaseErrorType.UNKNOWN, e);
        }
    }

    @Override
    public Optional<User> getById(Integer id) {
        if (id == null) {
            throw new DatabaseException("User id cannot be null", DatabaseErrorType.VALIDATION);
        }

        try (EntityManager em = emf.createEntityManager()) {
            User user = em.find(User.class, id);
            return Optional.ofNullable(user);
        } catch (RuntimeException e) {
            throw new DatabaseException("Get user by id failed", DatabaseErrorType.UNKNOWN, e);
        }
    }

    @Override
    public List<User> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT u FROM User u", User.class).getResultList();
        } catch (RuntimeException e) {
            throw new DatabaseException("Get all users failed", DatabaseErrorType.UNKNOWN, e);
        }
    }

    @Override
    public User update(User user) {
        if (user == null) {
            throw new DatabaseException("User cannot be null", DatabaseErrorType.VALIDATION);
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            User merged = em.merge(user);
            em.getTransaction().commit();
            return merged;
        } catch (PersistenceException e) {
            throw new DatabaseException("Update user failed", DatabaseErrorType.TRANSACTION_FAILURE, e);
        } catch (RuntimeException e) {
            throw new DatabaseException("Update user failed", DatabaseErrorType.UNKNOWN, e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        if (id == null) {
            throw new DatabaseException("User id cannot be null", DatabaseErrorType.VALIDATION);
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            User user = em.find(User.class, id);
            if (user == null) {
                em.getTransaction().rollback();
                return false;
            }
            em.remove(user);
            em.getTransaction().commit();
            return true;
        } catch (PersistenceException e) {
            throw new DatabaseException("Delete user failed", DatabaseErrorType.TRANSACTION_FAILURE, e);
        } catch (RuntimeException e) {
            throw new DatabaseException("Delete user failed", DatabaseErrorType.UNKNOWN, e);
        }
    }

    public Optional<User> getByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new DatabaseException("Email cannot be blank", DatabaseErrorType.VALIDATION);
        }

        try (EntityManager em = emf.createEntityManager()) {
            List<User> users = em.createQuery(
                            "SELECT u FROM User u WHERE u.email = :email",
                            User.class
                    )
                    .setParameter("email", email)
                    .setMaxResults(1)
                    .getResultList();
            return users.stream().findFirst();
        } catch (RuntimeException e) {
            throw new DatabaseException("Get user by email failed", DatabaseErrorType.UNKNOWN, e);
        }
    }

    @Override
    public User getVerifiedUser(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw ApiException.badRequest("Username and password are required");
        }

        User user = findByUsername(username)
                .orElseThrow(() -> ApiException.badRequest("Invalid username or password"));

        if (!user.verifyPassword(password)) {
            throw ApiException.badRequest("Invalid username or password");
        }

        return user;
    }

    @Override
    public User createUser(String firstName, String lastName, String email, String username, String password) {
        if (firstName == null || firstName.isBlank()
                || lastName == null || lastName.isBlank()
                || email == null || email.isBlank()
                || username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw ApiException.badRequest("First name, last name, email, username and password are required");
        }

        String normalizedUsername = username.trim();
        String normalizedFirstName = firstName.trim();
        String normalizedLastName = lastName.trim();
        String normalizedEmail = email.trim().toLowerCase();

        if (!normalizedEmail.contains("@")) {
            throw ApiException.badRequest("Email must be valid");
        }

        if (findByUsername(normalizedUsername).isPresent()) {
            throw ApiException.badRequest("Username already exists");
        }

        if (getByEmail(normalizedEmail).isPresent()) {
            throw ApiException.badRequest("Email already exists");
        }

        User user = new User(normalizedUsername, password);
        user.setFirstName(normalizedFirstName);
        user.setLastName(normalizedLastName);
        user.setEmail(normalizedEmail);
        String defaultRoleName = "USER";

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            List<Role> roles = em.createQuery(
                            "SELECT r FROM Role r WHERE lower(r.name) = lower(:role)",
                            Role.class
                    )
                    .setParameter("role", defaultRoleName)
                    .setMaxResults(1)
                    .getResultList();

            Role defaultRole;
            if (roles.isEmpty()) {
                defaultRole = new Role(defaultRoleName);
                em.persist(defaultRole);
            } else {
                defaultRole = roles.get(0);
            }

            user.addRole(defaultRole);
            em.persist(user);
            em.getTransaction().commit();
            return user;
        } catch (PersistenceException e) {
            throw new DatabaseException("Create user failed", DatabaseErrorType.TRANSACTION_FAILURE, e);
        } catch (RuntimeException e) {
            throw new DatabaseException("Create user failed", DatabaseErrorType.UNKNOWN, e);
        }
    }

    @Override
    public Role createRole(String role) {
        if (role == null || role.isBlank()) {
            throw ApiException.badRequest("Role cannot be blank");
        }
        String normalizedRole = role.trim().toUpperCase();

        try (EntityManager em = emf.createEntityManager()) {
            List<Role> roles = em.createQuery(
                            "SELECT r FROM Role r WHERE lower(r.name) = lower(:role)",
                            Role.class
                    )
                    .setParameter("role", normalizedRole)
                    .setMaxResults(1)
                    .getResultList();

            if (!roles.isEmpty()) {
                return roles.get(0);
            }

            em.getTransaction().begin();
            Role created = new Role(normalizedRole);
            em.persist(created);
            em.getTransaction().commit();
            return created;
        } catch (PersistenceException e) {
            throw new DatabaseException("Create role failed", DatabaseErrorType.TRANSACTION_FAILURE, e);
        } catch (RuntimeException e) {
            throw new DatabaseException("Create role failed", DatabaseErrorType.UNKNOWN, e);
        }
    }

    @Override
    public User addUserRole(String username, String role) {
        if (username == null || username.isBlank() || role == null || role.isBlank()) {
            throw ApiException.badRequest("Username and role are required");
        }

        String normalizedRole = role.trim().toUpperCase();

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            List<User> users = em.createQuery(
                            "SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username",
                            User.class
                    )
                    .setParameter("username", username.trim())
                    .setMaxResults(1)
                    .getResultList();

            if (users.isEmpty()) {
                em.getTransaction().rollback();
                throw ApiException.badRequest("User not found: " + username);
            }

            User user = users.get(0);

            List<Role> roles = em.createQuery(
                            "SELECT r FROM Role r WHERE lower(r.name) = lower(:role)",
                            Role.class
                    )
                    .setParameter("role", normalizedRole)
                    .setMaxResults(1)
                    .getResultList();

            Role existingRole;
            if (roles.isEmpty()) {
                existingRole = new Role(normalizedRole);
                em.persist(existingRole);
            } else {
                existingRole = roles.get(0);
            }

            user.addRole(existingRole);
            User merged = em.merge(user);
            em.getTransaction().commit();
            return merged;
        } catch (PersistenceException e) {
            throw new DatabaseException("Add user role failed", DatabaseErrorType.TRANSACTION_FAILURE, e);
        } catch (RuntimeException e) {
            throw new DatabaseException("Add user role failed", DatabaseErrorType.UNKNOWN, e);
        }
    }

    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        try (EntityManager em = emf.createEntityManager()) {
            List<User> users = em.createQuery(
                            "SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username",
                            User.class
                    )
                    .setParameter("username", username.trim())
                    .setMaxResults(1)
                    .getResultList();
            return users.stream().findFirst();
        } catch (RuntimeException e) {
            throw new DatabaseException("Get user by username failed", DatabaseErrorType.UNKNOWN, e);
        }
    }
}
