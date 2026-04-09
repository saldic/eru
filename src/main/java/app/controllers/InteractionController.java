package app.controllers;

import app.dtos.internal.AuthenticatedUserDTO;
import app.dtos.requests.InteractionRequestDTO;
import app.dtos.responses.InteractionDTO;
import app.dtos.responses.UserContentInteractionDTO;
import app.entities.enums.ReactionType;
import app.exceptions.ApiException;
import app.services.InteractionService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class InteractionController {
    private static final Logger logger = LoggerFactory.getLogger(InteractionController.class);

    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    public void createOrUpdate(Context ctx) {
        Integer contentId = parseId(ctx.pathParam("id"));
        AuthenticatedUserDTO authenticatedUser = getAuthenticatedUser(ctx);
        InteractionRequestDTO request = ctx.bodyAsClass(InteractionRequestDTO.class);
        if (request == null || request.reactionType() == null) {
            throw ApiException.badRequest("Reaction type is required");
        }

        logger.info("Interaction request userId={} contentId={} reactionType={}",
                authenticatedUser.userId(), contentId, request.reactionType());

        InteractionDTO interaction = interactionService.createOrUpdate(
                authenticatedUser.userId(),
                contentId,
                request.reactionType()
        );
        ctx.status(200).json(interaction);
    }

    public void getByContentId(Context ctx) {
        Integer contentId = parseId(ctx.pathParam("id"));
        List<InteractionDTO> interactions = interactionService.getByContentId(contentId);
        ctx.status(200).json(interactions);
    }

    public void getMyInteractions(Context ctx) {
        AuthenticatedUserDTO authenticatedUser = getAuthenticatedUser(ctx);
        ReactionType reactionType = parseReactionType(ctx.queryParam("reactionType"));
        List<UserContentInteractionDTO> interactions = interactionService.getByUserId(authenticatedUser.userId(), reactionType);
        ctx.status(200).json(interactions);
    }

    private static AuthenticatedUserDTO getAuthenticatedUser(Context ctx) {
        AuthenticatedUserDTO user = ctx.attribute("user");
        if (user == null) {
            throw ApiException.unauthorized("No authenticated user found in request context");
        }
        return user;
    }

    private static Integer parseId(String rawId) {
        try {
            return Integer.valueOf(rawId);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Path parameter id must be a number");
        }
    }

    private static ReactionType parseReactionType(String rawReactionType) {
        if (rawReactionType == null || rawReactionType.isBlank()) {
            return null;
        }

        try {
            return ReactionType.valueOf(rawReactionType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Invalid reaction type: " + rawReactionType);
        }
    }
}
