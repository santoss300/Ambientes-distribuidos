/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class EmisorUDP {

    private static final String HOST_POR_DEFECTO = "127.0.0.1";
    private static final int PUERTO_POR_DEFECTO = 6000;
    private static final long INTERVALO_POR_DEFECTO_MS = 2000L;
    private static final int CANTIDAD_POR_DEFECTO = 0;

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final String[] NIVELES = {"INFO", "WARN", "CRITICO"};

    private static final Random RANDOM = new Random();

    public static void main(String[] args) throws InterruptedException {

        String host = args.length > 0 ? args[0] : HOST_POR_DEFECTO;
        int puerto = args.length > 1 ? Integer.parseInt(args[1]) : PUERTO_POR_DEFECTO;
        long intervaloMs = args.length > 2 ? Long.parseLong(args[2]) : INTERVALO_POR_DEFECTO_MS;
        int cantidad = args.length > 3 ? Integer.parseInt(args[3]) : CANTIDAD_POR_DEFECTO;

        System.out.println("=================================================");
        System.out.println("  EMISOR DE ALERTAS (UDP)");
        System.out.println("=================================================");
        System.out.println("Destino   : " + host + ":" + puerto);
        System.out.println("Intervalo : " + intervaloMs + " ms");
        System.out.println("Cantidad  : " + (cantidad > 0 ? cantidad : "sin limite (Ctrl+C)"));
        System.out.println();

        try (DatagramSocket socket = new DatagramSocket()) {

            InetAddress destino = InetAddress.getByName(host);

            for (int n = 1; cantidad == 0 || n <= cantidad; n++) {

                String nivel = NIVELES[RANDOM.nextInt(NIVELES.length)];
                int temperatura = 40 + RANDOM.nextInt(50);
                String mensaje = "ALERTA #" + n + " | " + nivel
                        + " | temperatura=" + temperatura + "C";

                byte[] datos = mensaje.getBytes(StandardCharsets.UTF_8);
                DatagramPacket paquete = new DatagramPacket(datos, datos.length, destino, puerto);
                socket.send(paquete);

                System.out.println("[" + LocalTime.now().format(HORA) + "] Enviado: " + mensaje);

                if (cantidad == 0 || n < cantidad) {
                    Thread.sleep(intervaloMs);
                }
            }

            System.out.println();
            System.out.println("Emisor finalizado.");

        } catch (IOException e) {
            System.err.println("Error al enviar: " + e.getMessage());
        }
    }
}
