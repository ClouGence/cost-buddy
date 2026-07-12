package com.costbuddy.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserContext {

    private static final String CURRENT_USER_ATTRIBUTE       = CurrentUser.class.getName();
    private static final String MOTHERBOARD_TOKEN_ATTRIBUTE  = "costbuddy.motherboardToken";
    private static final String SESSION_EXPIRES_AT_ATTRIBUTE = "costbuddy.sessionExpiresAt";

    public Optional<CurrentUser> find(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }
        Object expiresAt = session.getAttribute(SESSION_EXPIRES_AT_ATTRIBUTE);
        if (expiresAt instanceof Instant instant && !Instant.now().isBefore(instant)) {
            session.invalidate();
            return Optional.empty();
        }
        Object value = session.getAttribute(CURRENT_USER_ATTRIBUTE);
        if (value instanceof CurrentUser currentUser) {
            return Optional.of(currentUser);
        }
        return Optional.empty();
    }

    public CurrentUser require(HttpServletRequest request) {
        return find(request).orElseThrow(AuthenticationRequiredException::new);
    }

    public void establish(HttpServletRequest request, CurrentUser currentUser, String motherboardToken, Long expiresIn) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(CURRENT_USER_ATTRIBUTE, currentUser);
        session.setAttribute(MOTHERBOARD_TOKEN_ATTRIBUTE, motherboardToken);
        if (expiresIn != null && expiresIn > 0) {
            session.setAttribute(SESSION_EXPIRES_AT_ATTRIBUTE, Instant.now().plusSeconds(expiresIn));
            session.setMaxInactiveInterval((int) Math.min(expiresIn, Integer.MAX_VALUE));
        }
    }

    public void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
