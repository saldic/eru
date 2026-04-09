package app.support;

import app.controllers.AuthController;
import app.controllers.ContentController;
import app.controllers.InteractionController;
import app.exceptions.ApiException;
import app.routes.AuthRoutes;
import app.routes.ContentRoutes;
import app.routes.InteractionRoutes;
import app.routes.Routes;
import io.javalin.Javalin;
import io.javalin.util.legacy.LegacyAccessManagerKt;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class TestApiFactory {

    private TestApiFactory() {
    }

    public static Javalin createApp(
            AuthController authController,
            ContentController contentController,
            InteractionController interactionController
    ) {
        Javalin app = Javalin.create(config -> {
            config.router.contextPath = Routes.API_CONTEXT_PATH;
            config.routes.exception(ApiException.class, (e, ctx) -> {
                ctx.status(e.getCode()).json(Map.of(
                        "errorCode", e.getErrorCode().name(),
                        "message", e.getMessage()
                ));
            });
            config.routes.exception(IllegalStateException.class, (e, ctx) -> {
                ctx.status(500).json(Map.of(
                        "errorCode", "INTERNAL_ERROR",
                        "message", e.getMessage()
                ));
            });
            config.routes.exception(Exception.class, (e, ctx) -> {
                ctx.status(500).json(Map.of(
                        "errorCode", "INTERNAL_ERROR",
                        "message", "Internal server error"
                ));
            });

            AuthRoutes.register(config.routes, authController);
            ContentRoutes.register(config.routes, contentController);
            InteractionRoutes.register(config.routes, interactionController);
        });

        LegacyAccessManagerKt.legacyAccessManager(app, (handler, ctx, routeRoles) -> {
            Set<String> allowedRoles = routeRoles.stream()
                    .map(role -> role.toString().toUpperCase())
                    .collect(Collectors.toSet());
            ctx.attribute("allowed_roles", allowedRoles);
            authController.authenticate(ctx);
            authController.authorize(ctx);
            try {
                handler.handle(ctx);
            } catch (Exception e) {
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new RuntimeException(e);
            }
        });

        return app;
    }
}
