package com.costbuddy.auth;

public class AuthenticationRequiredException extends RuntimeException {

    public AuthenticationRequiredException(){
        super("authentication is required");
    }
}
