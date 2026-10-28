/*
 * Ignacio Ruiz - DNI 39.040.338
 */

package tp9;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class FiltroJwt extends OncePerRequestFilter {

    private final Jwt jwt;

    public FiltroJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest pedido) {
        return pedido.getRequestURI().equals("/login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest pedido, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String encabezado = pedido.getHeader("Authorization");
        if (encabezado == null || !encabezado.startsWith("Bearer ")) {
            rechazar(pedido, respuesta, 401, "Falta el token");
            return;
        }

        Claims datos;
        try {
            datos = jwt.validar(encabezado.substring(7));
        } catch (ExpiredJwtException e) {
            rechazar(pedido, respuesta, 401, "El token esta vencido");
            return;
        } catch (SignatureException e) {
            rechazar(pedido, respuesta, 401, "La firma no coincide, el token fue alterado");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            rechazar(pedido, respuesta, 401, "Token invalido");
            return;
        }

        String usuario = datos.getSubject();
        String rol = datos.get("rol", String.class);
        if (pedido.getRequestURI().startsWith("/admin") && !"admin".equals(rol)) {
            rechazar(pedido, respuesta, 403, "El rol " + rol + " no tiene permiso para esta ruta");
            return;
        }

        System.out.println("OK  " + pedido.getMethod() + " " + pedido.getRequestURI() + " (" + usuario + ", " + rol + ")");
        pedido.setAttribute("usuario", usuario);
        pedido.setAttribute("rol", rol);
        cadena.doFilter(pedido, respuesta);
    }

    private void rechazar(HttpServletRequest pedido, HttpServletResponse respuesta, int codigo, String mensaje)
            throws IOException {
        System.out.println(codigo + " " + pedido.getMethod() + " " + pedido.getRequestURI() + ": " + mensaje);
        respuesta.setStatus(codigo);
        respuesta.setContentType("application/json");
        respuesta.getWriter().write("{\"error\":\"" + mensaje + "\"}");
    }
}
