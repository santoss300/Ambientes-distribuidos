/* Ignacio Ruiz - DNI 39.040.338 */
package tp8;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Main {

    static final String PRINCIPAL = "jdbc:postgresql://localhost:5432/tp8?connectTimeout=2";
    static final String REPLICA = "jdbc:postgresql://localhost:5433/tp8?connectTimeout=2";
    static final String USUARIO = "postgres";
    static final String CLAVE = "postgres";

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("caida")) {
            pruebaCaida();
        } else {
            pruebaPropagacion();
        }
    }

    static void pruebaPropagacion() throws Exception {
        System.out.println("=== Prueba de propagacion ===");
        System.out.println("Escrituras -> principal (puerto 5432)");
        System.out.println("Lecturas   -> replica   (puerto 5433)");
        System.out.println();

        String[] nombres = {"Ana", "Bruno", "Carla", "Diego", "Elena"};
        try (Connection replica = DriverManager.getConnection(REPLICA, USUARIO, CLAVE)) {
            for (String nombre : nombres) {
                int id = insertar(PRINCIPAL, nombre);
                long inicio = System.nanoTime();
                int intentos = 1;
                while (!existe(replica, id)) {
                    intentos++;
                }
                double ms = (System.nanoTime() - inicio) / 1_000_000.0;
                System.out.printf("INSERT id=%d (%s) en el principal -> visto en la replica a los %.1f ms (%d consultas)%n", id, nombre, ms, intentos);
            }
        }

        System.out.println();
        System.out.println("SELECT en la replica:");
        try (Connection con = DriverManager.getConnection(REPLICA, USUARIO, CLAVE);
             ResultSet rs = con.createStatement().executeQuery("SELECT id, nombre FROM alumnos ORDER BY id")) {
            while (rs.next()) {
                System.out.println("  " + rs.getInt("id") + " - " + rs.getString("nombre"));
            }
        }

        System.out.println();
        System.out.println("Intento de INSERT directo en la replica:");
        try {
            insertar(REPLICA, "Intruso");
            System.out.println("  Se pudo escribir (no deberia pasar)");
        } catch (SQLException e) {
            System.out.println("  Rechazado: " + e.getMessage());
        }
    }

    static void pruebaCaida() throws Exception {
        System.out.println("=== Prueba de caida del principal ===");
        String escribirEn = PRINCIPAL;
        int numero = 1;

        for (int i = 0; i < 20; i++) {
            String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String nodo = escribirEn.equals(PRINCIPAL) ? "principal" : "replica";

            try {
                int id = insertar(escribirEn, "Alumno " + numero);
                System.out.println(hora + "  ESCRITURA ok en " + nodo + " (id=" + id + ")");
                numero++;
            } catch (SQLException e) {
                System.out.println(hora + "  ESCRITURA fallo en " + nodo + ": " + e.getMessage().split("\\. ")[0]);
                if (escribirEn.equals(PRINCIPAL)) {
                    try {
                        int id = insertar(REPLICA, "Alumno " + numero);
                        escribirEn = REPLICA;
                        numero++;
                        System.out.println(hora + "  -> La replica acepta escrituras, ahora es el principal (id=" + id + ")");
                    } catch (SQLException e2) {
                        System.out.println(hora + "  -> La replica sigue siendo de solo lectura");
                    }
                }
            }

            try {
                System.out.println(hora + "  LECTURA en replica: " + contar(REPLICA) + " alumnos");
            } catch (SQLException e) {
                System.out.println(hora + "  LECTURA fallo en replica: " + e.getMessage());
            }

            Thread.sleep(2000);
        }
    }

    static int insertar(String url, String nombre) throws SQLException {
        try (Connection con = DriverManager.getConnection(url, USUARIO, CLAVE);
             PreparedStatement ps = con.prepareStatement("INSERT INTO alumnos (nombre) VALUES (?) RETURNING id")) {
            ps.setString(1, nombre);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    static boolean existe(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM alumnos WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeQuery().next();
        }
    }

    static int contar(String url) throws SQLException {
        try (Connection con = DriverManager.getConnection(url, USUARIO, CLAVE);
             ResultSet rs = con.createStatement().executeQuery("SELECT COUNT(*) FROM alumnos")) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
