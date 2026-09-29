package com.recallx.recallx.dto.response;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data @AllArgsConstructor
public class JwtResponse {
    private String token;
    private Long id;
    private String username;
    private Long organizationId;
    private String role;
}
