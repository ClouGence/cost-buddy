package com.costbuddy.controller;

import com.costbuddy.auth.AuthenticationFlowException;
import com.costbuddy.auth.OAuthLoginService;
import com.costbuddy.motherboard.MotherboardGatewayException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(prefix = "motherboard", name = "enabled", havingValue = "true")
public class AuthenticationController {

    private static final Logger     LOGGER = LoggerFactory.getLogger(AuthenticationController.class);

    private final OAuthLoginService oAuthLoginService;

    public AuthenticationController(OAuthLoginService oAuthLoginService){
        this.oAuthLoginService = oAuthLoginService;
    }

    @GetMapping("/login/{provider}")
    public ResponseEntity<Void> login(@PathVariable String provider, HttpServletRequest request) {
        return redirect(oAuthLoginService.begin(provider, request));
    }

    @GetMapping("/callback/{provider}")
    public ResponseEntity<Void> callback(@PathVariable String provider, @RequestParam(required = false) String code, @RequestParam(required = false) String state,
                                         @RequestParam(required = false) String error, HttpServletRequest request) {
        try {
            oAuthLoginService.complete(provider, code, state, error, request);
            return redirect(oAuthLoginService.successRedirect());
        } catch (AuthenticationFlowException exception) {
            LOGGER.warn("OAuth callback failed: code={}, message={}", exception.getCode(), exception.getMessage());
            return redirect(oAuthLoginService.failureRedirect(exception.getCode()));
        } catch (MotherboardGatewayException exception) {
            LOGGER.error("Motherboard SSO callback failed: type={}, upstreamCode={}", exception.getFailureType(), exception.getUpstreamCode(), exception);
            return redirect(oAuthLoginService.failureRedirect("MOTHERBOARD_SSO_FAILED"));
        }
    }

    private ResponseEntity<Void> redirect(URI location) {
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }
}
