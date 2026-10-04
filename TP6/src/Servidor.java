/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Servidor {

    public static final int PUERTO = 1099;
    public static final String NOMBRE_SERVICIO = "ServicioProcesamiento";

    public static void main(String[] args) {
        try {
            ServicioRemotoImpl servicio = new ServicioRemotoImpl();

            Registry registro = LocateRegistry.createRegistry(PUERTO);
            registro.rebind(NOMBRE_SERVICIO, servicio);

            System.out.println("=================================================");
            System.out.println("  SERVIDOR RMI DE PROCESAMIENTO");
            System.out.println("=================================================");
            System.out.println("Registro RMI iniciado en el puerto " + PUERTO);
            System.out.println("Servicio publicado como \"" + NOMBRE_SERVICIO + "\"");
            System.out.println("Esperando clientes... (Ctrl+C para detener)");
            System.out.println();

        } catch (Exception e) {
            System.out.println("No se pudo iniciar el servidor: " + e.getMessage());
        }
    }
}
