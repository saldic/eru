package app.services;

import app.dtos.responses.InteractionDTO;
import app.dtos.responses.UserContentInteractionDTO;
import app.entities.Content;
import app.entities.Role;
import app.entities.User;
import app.entities.UserInteraction;
import app.entities.enums.ContentType;
import app.entities.enums.ReactionType;
import app.exceptions.ApiException;
import app.persistence.daos.ContentDAO;
import app.persistence.daos.UserDAO;
import app.persistence.daos.UserInteractionDAO;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InteractionServiceTest {

    @Test
    void createOrUpdateShouldCreateNewInteraction() {
        FakeUserDAO userDAO = new FakeUserDAO();
        FakeContentDAO contentDAO = new FakeContentDAO();
        FakeInteractionDAO interactionDAO = new FakeInteractionDAO();
        userDAO.seed(user(1, "student1"));
        contentDAO.seed(content(10));

        InteractionService service = new InteractionService(interactionDAO, userDAO, contentDAO);

        InteractionDTO interaction = service.createOrUpdate(1, 10, ReactionType.BOOKMARK);

        assertEquals(1, interaction.id());
        assertEquals(1, interaction.userId());
        assertEquals(10, interaction.contentId());
        assertEquals(ReactionType.BOOKMARK, interaction.reactionType());
    }

    @Test
    void createOrUpdateShouldUpdateExistingInteractionForSameUserAndContent() {
        FakeUserDAO userDAO = new FakeUserDAO();
        FakeContentDAO contentDAO = new FakeContentDAO();
        FakeInteractionDAO interactionDAO = new FakeInteractionDAO();
        User user = user(1, "student1");
        Content content = content(10);
        userDAO.seed(user);
        contentDAO.seed(content);
        interactionDAO.seed(interaction(7, user, content, ReactionType.LIKE));

        InteractionService service = new InteractionService(interactionDAO, userDAO, contentDAO);

        InteractionDTO interaction = service.createOrUpdate(1, 10, ReactionType.BOOKMARK);

        assertEquals(7, interaction.id());
        assertEquals(ReactionType.BOOKMARK, interaction.reactionType());
        assertEquals(1, interactionDAO.getByContentId(10).size());
    }

    @Test
    void getByContentIdShouldThrowWhenContentDoesNotExist() {
        InteractionService service = new InteractionService(
                new FakeInteractionDAO(),
                new FakeUserDAO(),
                new FakeContentDAO()
        );

        ApiException exception = assertThrows(ApiException.class, () -> service.getByContentId(99));

        assertEquals(404, exception.getCode());
        assertEquals("Content not found with id 99", exception.getMessage());
    }

    @Test
    void getByUserIdShouldReturnSortedFilteredInteractions() {
        FakeUserDAO userDAO = new FakeUserDAO();
        FakeContentDAO contentDAO = new FakeContentDAO();
        FakeInteractionDAO interactionDAO = new FakeInteractionDAO();
        User user = user(1, "student1");
        Content olderContent = content(10);
        Content newerContent = content(11);
        userDAO.seed(user);
        contentDAO.seed(olderContent);
        contentDAO.seed(newerContent);
        interactionDAO.seed(interaction(1, user, olderContent, ReactionType.LIKE, LocalDateTime.now().minusDays(1)));
        interactionDAO.seed(interaction(2, user, newerContent, ReactionType.BOOKMARK, LocalDateTime.now()));

        InteractionService service = new InteractionService(interactionDAO, userDAO, contentDAO);

        List<UserContentInteractionDTO> interactions = service.getByUserId(1, ReactionType.BOOKMARK);

        assertEquals(1, interactions.size());
        assertEquals(2, interactions.get(0).id());
        assertEquals(ReactionType.BOOKMARK, interactions.get(0).reactionType());
        assertEquals(11, interactions.get(0).content().id());
    }

    private static User user(Integer id, String username) {
        User user = new User(username, "secret123");
        user.setId(id);
        user.setRoles(Set.of(new Role("USER")));
        return user;
    }

    private static Content content(Integer id) {
        return Content.builder()
                .id(id)
                .title("Sample")
                .body("Sample body")
                .contentType(ContentType.FACT)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static UserInteraction interaction(Integer id, User user, Content content, ReactionType reactionType) {
        return interaction(id, user, content, reactionType, LocalDateTime.now());
    }

    private static UserInteraction interaction(
            Integer id,
            User user,
            Content content,
            ReactionType reactionType,
            LocalDateTime createdAt
    ) {
        return UserInteraction.builder()
                .id(id)
                .user(user)
                .content(content)
                .reactionType(reactionType)
                .createdAt(createdAt)
                .build();
    }

    private static class FakeUserDAO extends UserDAO {
        private final Map<Integer, User> storage = new LinkedHashMap<>();

        FakeUserDAO() {
            super((EntityManagerFactory) null);
        }

        void seed(User user) {
            storage.put(user.getId(), user);
        }

        @Override
        public Optional<User> getById(Integer id) {
            return Optional.ofNullable(storage.get(id));
        }
    }

    private static class FakeContentDAO extends ContentDAO {
        private final Map<Integer, Content> storage = new LinkedHashMap<>();

        FakeContentDAO() {
            super((EntityManagerFactory) null);
        }

        void seed(Content content) {
            storage.put(content.getId(), content);
        }

        @Override
        public Optional<Content> getById(Integer id) {
            return Optional.ofNullable(storage.get(id));
        }
    }

    private static class FakeInteractionDAO extends UserInteractionDAO {
        private final Map<Integer, UserInteraction> storage = new LinkedHashMap<>();
        private int nextId = 1;

        FakeInteractionDAO() {
            super((EntityManagerFactory) null);
        }

        void seed(UserInteraction interaction) {
            storage.put(interaction.getId(), interaction);
            nextId = Math.max(nextId, interaction.getId() + 1);
        }

        @Override
        public UserInteraction create(UserInteraction interaction) {
            interaction.setId(nextId++);
            if (interaction.getCreatedAt() == null) {
                interaction.setCreatedAt(LocalDateTime.now());
            }
            storage.put(interaction.getId(), interaction);
            return interaction;
        }

        @Override
        public UserInteraction update(UserInteraction interaction) {
            storage.put(interaction.getId(), interaction);
            return interaction;
        }

        @Override
        public List<UserInteraction> getByContentId(Integer contentId) {
            return storage.values().stream()
                    .filter(interaction -> interaction.getContent().getId().equals(contentId))
                    .toList();
        }

        @Override
        public Optional<UserInteraction> getByUserAndContent(Integer userId, Integer contentId) {
            return storage.values().stream()
                    .filter(interaction -> interaction.getUser().getId().equals(userId))
                    .filter(interaction -> interaction.getContent().getId().equals(contentId))
                    .findFirst();
        }

        @Override
        public List<UserInteraction> getByUserId(Integer userId) {
            return storage.values().stream()
                    .filter(interaction -> interaction.getUser().getId().equals(userId))
                    .toList();
        }
    }
}
