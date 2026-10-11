/*
 * Ignacio Ruiz - DNI 39.040.338
 */

package tp7;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Servidor {

    public static void main(String[] args) {
        SpringApplication.run(Servidor.class, args);
        System.out.println("=================================================");
        System.out.println("  API REST de productos en http://localhost:8080");
        System.out.println("  GET  /productos");
        System.out.println("  GET  /productos/{id}");
        System.out.println("  POST /productos");
        System.out.println("=================================================");
    }
}
