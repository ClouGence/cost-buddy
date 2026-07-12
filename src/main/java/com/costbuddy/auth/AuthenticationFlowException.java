package com.costbuddy.auth;

import lombok.Getter;

@Getter
public class AuthenticationFlowException extends RuntimeException {

    private final String code;

    public AuthenticationFlowException(String code, String message){
        super(message);
        this.code = code;
    }
}
