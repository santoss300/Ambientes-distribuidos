/*
 * Ignacio Ruiz - DNI 39.040.338
 */

package tp9;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class LoginController {

    private final Map<String, String> claves = Map.of("ana", "1234", "beto", "abcd");
    private final Map<String, String> roles = Map.of("ana", "admin", "beto", "usuario");
    private final Jwt jwt;

    public LoginController(Jwt jwt) {
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> datos) {
        String usuario = datos.get("usuario");
        String clave = datos.get("clave");

        if (usuario == null || clave == null || !clave.equals(claves.get(usuario))) {
            System.out.println("401 POST /login: usuario o clave incorrectos (" + usuario + ")");
            return ResponseEntity.status(401).body(Map.of("error", "Usuario o clave incorrectos"));
        }

        String rol = roles.get(usuario);
        System.out.println("OK  POST /login: token emitido para " + usuario + " (" + rol + ")");
        return ResponseEntity.ok(Map.of("token", jwt.crear(usuario, rol)));
    }
}
