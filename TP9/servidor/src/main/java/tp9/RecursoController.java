/*
 * Ignacio Ruiz - DNI 39.040.338
 */

package tp9;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RecursoController {

    @GetMapping("/datos")
    public Map<String, String> datos(@RequestAttribute String usuario, @RequestAttribute String rol) {
        return Map.of("mensaje", "Hola " + usuario + ", estos son datos protegidos", "rol", rol);
    }

    @GetMapping("/admin")
    public Map<String, String> admin(@RequestAttribute String usuario) {
        return Map.of("mensaje", "Bienvenido al panel de administracion, " + usuario);
    }
}
