package app.dtos.responses;

import app.entities.UserInteraction;
import app.entities.enums.ReactionType;

import java.time.LocalDateTime;

public record UserContentInteractionDTO(
        Integer id,
        ReactionType reactionType,
        LocalDateTime createdAt,
        ContentDTO content
) {
    public static UserContentInteractionDTO fromEntity(UserInteraction interaction) {
        return new UserContentInteractionDTO(
                interaction.getId(),
                interaction.getReactionType(),
                interaction.getCreatedAt(),
                ContentDTO.fromEntity(interaction.getContent())
        );
    }
}
