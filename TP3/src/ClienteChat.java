/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;

public class ClienteChat {

    private static final String HOST_POR_DEFECTO = "127.0.0.1";
    private static final int PUERTO_POR_DEFECTO = 5000;

    public static void main(String[] args) {

        String host = args.length > 0 ? args[0] : HOST_POR_DEFECTO;
        int puerto = args.length > 1 ? Integer.parseInt(args[1]) : PUERTO_POR_DEFECTO;

        System.out.println("=================================================");
        System.out.println("  CLIENTE DE CHAT (TCP)");
        System.out.println("=================================================");
        System.out.println("Conectando a " + host + ":" + puerto + "...");

        try (Socket socket = new Socket(host, puerto);
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Conectado.");
            System.out.println();

            Thread receptor = new Thread(() -> {
                try {
                    String mensaje;
                    while ((mensaje = entrada.readLine()) != null) {
                        System.out.println(mensaje);
                    }
                    System.out.println("El servidor cerro la conexion.");
                } catch (IOException e) {
                    System.out.println("Conexion cerrada.");
                }
            }, "receptor");
            receptor.setDaemon(true);
            receptor.start();

            String linea;
            while ((linea = teclado.readLine()) != null) {
                salida.println(linea);
                if (linea.equalsIgnoreCase("/salir")) {
                    break;
                }
            }

            System.out.println("Saliste del chat.");

        } catch (ConnectException e) {
            System.err.println("No se pudo conectar: el servidor no esta disponible en "
                    + host + ":" + puerto);
        } catch (IOException e) {
            System.err.println("Error de comunicacion: " + e.getMessage());
        }
    }
}
