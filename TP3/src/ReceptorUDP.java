/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ReceptorUDP {

    private static final int PUERTO_POR_DEFECTO = 6000;
    private static final int TIMEOUT_MS = 5000;
    private static final int TAMANIO_BUFFER = 1024;

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {

        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : PUERTO_POR_DEFECTO;

        try (DatagramSocket socket = new DatagramSocket(puerto)) {

            socket.setSoTimeout(TIMEOUT_MS);

            System.out.println("=================================================");
            System.out.println("  RECEPTOR DE ALERTAS Y TELEMETRIA (UDP)");
            System.out.println("=================================================");
            System.out.println("Escuchando en el puerto " + puerto + "...");
            System.out.println("Timeout de espera: " + TIMEOUT_MS + " ms");
            System.out.println("(Ctrl+C para detener)");
            System.out.println();

            byte[] buffer = new byte[TAMANIO_BUFFER];
            int timeouts = 0;

            while (true) {

                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);

                try {
                    socket.receive(paquete);

                    String mensaje = new String(paquete.getData(), 0, paquete.getLength(),
                            StandardCharsets.UTF_8);

                    System.out.println("[" + hora() + "] Recibido de "
                            + paquete.getAddress().getHostAddress() + ":" + paquete.getPort()
                            + " -> " + mensaje);

                } catch (SocketTimeoutException e) {
                    timeouts++;
                    System.out.println("[" + hora() + "] ADVERTENCIA: no llego ningun datagrama en "
                            + (TIMEOUT_MS / 1000) + " s (timeout #" + timeouts
                            + "). Sigo escuchando...");
                }
            }

        } catch (IOException e) {
            System.err.println("No se pudo abrir el puerto " + puerto + ": " + e.getMessage());
        }
    }

    private static String hora() {
        return LocalTime.now().format(HORA);
    }
}
