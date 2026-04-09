package app.routes;

import app.controllers.AuthController;
import app.security.AppRole;
import io.javalin.router.JavalinDefaultRoutingApi;

public class AuthRoutes {

    private AuthRoutes() {
    }

    public static void register(JavalinDefaultRoutingApi routes, AuthController authController) {
        routes.post("/auth/register", authController::register, AppRole.ANYONE);
        routes.post("/auth/login", authController::login, AppRole.ANYONE);
        routes.get("/auth/me", authController::me, AppRole.USER);
        routes.post("/auth/logout", authController::logout, AppRole.USER);
        routes.post("/auth/roles", authController::addRole, AppRole.ADMIN);
    }
}
