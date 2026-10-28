/*
 * Ignacio Ruiz - DNI 39.040.338
 */

package tp9;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Servidor {

    public static void main(String[] args) {
        SpringApplication.run(Servidor.class, args);
        System.out.println("Servidor HTTPS escuchando en https://localhost:8443");
    }
}
