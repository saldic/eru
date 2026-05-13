package app.services;

import app.dtos.responses.InteractionDTO;
import app.dtos.responses.UserContentInteractionDTO;
import app.entities.Content;
import app.entities.User;
import app.entities.UserInteraction;
import app.entities.enums.ReactionType;
import app.exceptions.ApiException;
import app.persistence.daos.ContentDAO;
import app.persistence.daos.UserDAO;
import app.persistence.daos.UserInteractionDAO;

import java.util.List;
import java.util.Comparator;

public class InteractionService {
    private final UserInteractionDAO interactionDAO;
    private final UserDAO userDAO;
    private final ContentDAO contentDAO;

    public InteractionService(UserInteractionDAO interactionDAO, UserDAO userDAO, ContentDAO contentDAO) {
        this.interactionDAO = interactionDAO;
        this.userDAO = userDAO;
        this.contentDAO = contentDAO;
    }

    public InteractionDTO createOrUpdate(Integer userId, Integer contentId, ReactionType reactionType) {
        if (reactionType == null) {
            throw ApiException.badRequest("Reaction type is required");
        }

        User user = userDAO.getById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found with id " + userId));
        Content content = contentDAO.getById(contentId)
                .orElseThrow(() -> ApiException.notFound("Content not found with id " + contentId));

        UserInteraction interaction = interactionDAO.getByUserAndContentAndReactionType(userId, contentId, reactionType)
                .orElseGet(() -> UserInteraction.builder()
                        .user(user)
                        .content(content)
                        .reactionType(reactionType)
                        .build());

        interaction.setUser(user);
        interaction.setContent(content);

        UserInteraction saved = interaction.getId() == null
                ? interactionDAO.create(interaction)
                : interactionDAO.update(interaction);

        return InteractionDTO.fromEntity(saved);
    }

    public List<InteractionDTO> getByContentId(Integer contentId) {
        ensureContentExists(contentId);
        return interactionDAO.getByContentId(contentId).stream()
                .map(InteractionDTO::fromEntity)
                .toList();
    }

    public List<UserContentInteractionDTO> getByUserId(Integer userId, ReactionType reactionType) {
        ensureUserExists(userId);

        return interactionDAO.getByUserId(userId).stream()
                .filter(interaction -> reactionType == null || interaction.getReactionType() == reactionType)
                .sorted(Comparator.comparing(UserInteraction::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(UserContentInteractionDTO::fromEntity)
                .toList();
    }

    private void ensureContentExists(Integer contentId) {
        contentDAO.getById(contentId)
                .orElseThrow(() -> ApiException.notFound("Content not found with id " + contentId));
    }

    private void ensureUserExists(Integer userId) {
        userDAO.getById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found with id " + userId));
    }
}
