package co.sillon;

import org.springframework.security.oauth2.jwt.Jwt;

record Actor(long id, long salon, boolean dueno) {
    static Actor de(Jwt jwt) {
        return new Actor(Long.parseLong(jwt.getSubject()), ((Number)jwt.getClaims().get("salon")).longValue(),
                "DUENO".equals(jwt.getClaimAsString("scope")));
    }
}
