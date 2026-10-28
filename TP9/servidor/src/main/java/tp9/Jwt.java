/*
 * Ignacio Ruiz - DNI 39.040.338
 */

package tp9;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class Jwt {

    private final SecretKey clave;
    private final long segundos;

    public Jwt(@Value("${jwt.secreto}") String secreto, @Value("${jwt.segundos}") long segundos) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.segundos = segundos;
    }

    public String crear(String usuario, String rol) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(usuario)
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + segundos * 1000))
                .signWith(clave)
                .compact();
    }

    public Claims validar(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
