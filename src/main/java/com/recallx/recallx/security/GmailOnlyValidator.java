package com.recallx.recallx.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class GmailOnlyValidator implements OAuth2TokenValidator<Jwt> {
    
    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        
        if (email != null && email.endsWith("@gmail.com")) {
            return OAuth2TokenValidatorResult.success();
        }
        
        OAuth2Error error = new OAuth2Error("invalid_token", "Only @gmail.com accounts are allowed.", null);
        return OAuth2TokenValidatorResult.failure(error);
    }
}
