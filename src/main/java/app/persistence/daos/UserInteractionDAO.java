package app.persistence.daos;

import app.config.hibernate.HibernateConfig;
import app.entities.UserInteraction;
import app.entities.enums.ReactionType;
import app.exceptions.DatabaseException;
import app.exceptions.enums.DatabaseErrorType;
import app.persistence.interfaces.IDAO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;

import java.util.List;
import java.util.Optional;

public class UserInteractionDAO implements IDAO<UserInteraction, Integer> {
    private final EntityManagerFactory emf;

    public UserInteractionDAO() {
        this(HibernateConfig.getEntityManagerFactory());
    }

    public UserInteractionDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public UserInteraction create(UserInteraction interaction) {
        if (interaction == null) {
            throw new DatabaseException("Interaction cannot be null", DatabaseErrorType.VALIDATION);
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(interaction);
            em.getTransaction().commit();
            return interaction;
        } catch (PersistenceException e) {
            throw new DatabaseException("Create interaction failed", DatabaseErrorType.TRANSACTION_FAILURE, e);
        } catch (RuntimeException e) {
            throw new DatabaseException("Create interaction failed", DatabaseErrorType.UNKNOWN, e);
        }
    }

    @Override
    public Optional<UserInteraction> getById(Integer id) {
        if (id == null) {
            throw new DatabaseException("Interaction id cannot be null", DatabaseErrorType.VALIDATION);
        }

        try (EntityManager em = emf.createEntityManager()) {
            List<UserInteraction> interactions = em.createQuery(
                            "SELECT ui FROM UserInteraction ui " +
                                    "JOIN FETCH ui.user " +
                                    "JOIN FETCH ui.content " +
                                    "WHERE ui.id = :id",
                            UserInteraction.class
                    )
                    .setParameter("id", id)
                    .setMaxResults(1)
                    .getResultList();
            return interactions.stream().findFirst();
        } catch (RuntimeException e) {
            throw new DatabaseException("Get interaction by id failed", DatabaseErrorType.UNKNOWN, e);
        }
    }

    @Override
    public List<UserInteraction> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT ui FROM UserInteraction ui " +
                                    "JOIN FETCH ui.user " +
                                    "JOIN FETCH ui.content",
                            UserInteraction.class
                    )
                    .getResultList();
        } catch (RuntimeException e) {
            throw new DatabaseException("Get all interactions failed", DatabaseErrorType.UNKNOWN, e);
        }
    }

    @Override
    public UserInteraction update(UserInteraction interaction) {
        if (interaction == null) {
            throw new DatabaseException("Interaction cannot be null", DatabaseErrorType.VALIDATION);
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            UserInteraction merged = em.merge(interaction);
            em.getTransaction().commit();
            return merged;
        } catch (PersistenceException e) {
            throw new DatabaseException("Update interaction failed", DatabaseErrorType.TRANSACTION_FAILURE, e);
        } catch (RuntimeException e) {
            throw new DatabaseException("Update interaction failed", DatabaseErrorType.UNKNOWN, e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        if (id == null) {
            throw new DatabaseException("Interaction id cannot be null", DatabaseErrorType.VALIDATION);
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            UserInteraction interaction = em.find(UserInteraction.class, id);
            if (interaction == null) {
                em.getTransaction().rollback();
                return false;
            }
            em.remove(interaction);
            em.getTransaction().commit();
            return true;
        } catch (PersistenceException e) {
            throw new DatabaseException("Delete interaction failed", DatabaseErrorType.TRANSACTION_FAILURE, e);
        } catch (RuntimeException e) {
            throw new DatabaseException("Delete interaction failed", DatabaseErrorType.UNKNOWN, e);
        }
    }

    public List<UserInteraction> getByUserId(Integer userId) {
        if (userId == null) {
            throw new DatabaseException("User id cannot be null", DatabaseErrorType.VALIDATION);
        }

        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT ui FROM UserInteraction ui " +
                                    "JOIN FETCH ui.user " +
                                    "JOIN FETCH ui.content " +
                                    "WHERE ui.user.id = :userId",
                            UserInteraction.class
                    )
                    .setParameter("userId", userId)
                    .getResultList();
        } catch (RuntimeException e) {
            throw new DatabaseException("Get interactions by user id failed", DatabaseErrorType.UNKNOWN, e);
        }
    }

    public List<UserInteraction> getByContentId(Integer contentId) {
        if (contentId == null) {
            throw new DatabaseException("Content id cannot be null", DatabaseErrorType.VALIDATION);
        }

        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT ui FROM UserInteraction ui " +
                                    "JOIN FETCH ui.user " +
                                    "JOIN FETCH ui.content " +
                                    "WHERE ui.content.id = :contentId",
                            UserInteraction.class
                    )
                    .setParameter("contentId", contentId)
                    .getResultList();
        } catch (RuntimeException e) {
            throw new DatabaseException("Get interactions by content id failed", DatabaseErrorType.UNKNOWN, e);
        }
    }

    public Optional<UserInteraction> getByUserAndContentAndReactionType(
            Integer userId,
            Integer contentId,
            ReactionType reactionType
    ) {
        if (userId == null || contentId == null || reactionType == null) {
            throw new DatabaseException("User id, content id and reaction type cannot be null", DatabaseErrorType.VALIDATION);
        }

        try (EntityManager em = emf.createEntityManager()) {
            List<UserInteraction> interactions = em.createQuery(
                            "SELECT ui FROM UserInteraction ui " +
                                    "JOIN FETCH ui.user " +
                                    "JOIN FETCH ui.content " +
                                    "WHERE ui.user.id = :userId " +
                                    "AND ui.content.id = :contentId " +
                                    "AND ui.reactionType = :reactionType",
                            UserInteraction.class
                    )
                    .setParameter("userId", userId)
                    .setParameter("contentId", contentId)
                    .setParameter("reactionType", reactionType)
                    .setMaxResults(1)
                    .getResultList();

            return interactions.stream().findFirst();
        } catch (RuntimeException e) {
            throw new DatabaseException("Get interaction by user, content and reaction type failed", DatabaseErrorType.UNKNOWN, e);
        }
    }
}
