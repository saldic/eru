package app.routes;

import app.controllers.InteractionController;
import app.security.AppRole;
import io.javalin.router.JavalinDefaultRoutingApi;

public class InteractionRoutes {

    private InteractionRoutes() {
    }

    public static void register(JavalinDefaultRoutingApi routes, InteractionController interactionController) {
        routes.get("/interactions/me", interactionController::getMyInteractions, AppRole.USER);
        routes.post("/content/{id}/interactions", interactionController::createOrUpdate, AppRole.USER);
        routes.get("/content/{id}/interactions", interactionController::getByContentId, AppRole.ANYONE);
    }
}
