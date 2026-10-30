package project.smartbanking.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessExpirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-expiration-ms}") long accessExpirationMs) {
        // Khóa phải dài ít nhất 32 byte. Chuỗi mẫu trong .env.example ai cũng biết, dùng nó thì
        // bất kỳ ai cũng tự ký được token quyền ADMIN, nên từ chối khởi động.
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình app.jwt.secret (biến môi trường JWT_SECRET)");
        }
        if (secret.length() < 32 || secret.startsWith("doi-chuoi-nay")) {
            throw new IllegalStateException("app.jwt.secret phải là chuỗi ngẫu nhiên riêng, dài tối thiểu 32 ký tự");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
    }

    public String generateAccessToken(UserPrincipal principal) {
        Date now = new Date();
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(String.valueOf(principal.getId()))
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpirationMs))
                .signWith(key)
                .compact();
    }

    // Ném JwtException nếu token sai chữ ký, hết hạn hoặc hỏng
    public Long extractUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationMs / 1000;
    }
}