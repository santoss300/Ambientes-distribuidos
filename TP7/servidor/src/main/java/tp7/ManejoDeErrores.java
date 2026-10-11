/*
 * Ignacio Ruiz - DNI 39.040.338
 */

package tp7;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@RestControllerAdvice
public class ManejoDeErrores {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> errorConCodigo(ResponseStatusException e) {
        return responder(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason());
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<Map<String, Object>> jsonInvalido(JsonProcessingException e) {
        return responder(HttpStatus.BAD_REQUEST, "El JSON enviado no es valido");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> idInvalido(MethodArgumentTypeMismatchException e) {
        return responder(HttpStatus.BAD_REQUEST, "El id tiene que ser un numero");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> rutaInexistente(NoResourceFoundException e) {
        return responder(HttpStatus.NOT_FOUND, "Esa ruta no existe");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> metodoNoPermitido(HttpRequestMethodNotSupportedException e) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Metodo " + e.getMethod() + " no permitido en esta ruta");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> errorInterno(Exception e) {
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    private ResponseEntity<Map<String, Object>> responder(HttpStatus estado, String mensaje) {
        System.out.println("  ERROR " + estado.value() + ": " + mensaje);
        return ResponseEntity.status(estado).body(Map.of("estado", estado.value(), "error", mensaje));
    }
}
