/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServidorChat {

    private static final int PUERTO_POR_DEFECTO = 5000;

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final List<AtencionCliente> clientes = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {

        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : PUERTO_POR_DEFECTO;

        try (ServerSocket servidor = new ServerSocket(puerto)) {

            System.out.println("=================================================");
            System.out.println("  SERVIDOR DE CHAT MULTIHILO (TCP)");
            System.out.println("=================================================");
            System.out.println("Escuchando en el puerto " + puerto + "...");
            System.out.println("(Ctrl+C para detener)");
            System.out.println();

            int numeroDeCliente = 0;

            while (true) {

                Socket socket = servidor.accept();

                numeroDeCliente++;

                AtencionCliente atencion = new AtencionCliente(socket, numeroDeCliente);
                Thread hilo = new Thread(atencion, "cliente-" + numeroDeCliente);
                hilo.start();

                log("Conexion aceptada desde "
                        + socket.getInetAddress().getHostAddress() + ":" + socket.getPort()
                        + " -> atendida por el hilo \"" + hilo.getName() + "\"");
            }

        } catch (IOException e) {
            System.err.println("No se pudo abrir el puerto " + puerto + ": " + e.getMessage());
            System.err.println("Verifique que no haya otro servidor ya ejecutandose.");
        }
    }

    private static void difundir(String mensaje, AtencionCliente remitente) {
        for (AtencionCliente cliente : clientes) {
            if (cliente != remitente) {
                cliente.enviar(mensaje);
            }
        }
    }

    private static synchronized void log(String texto) {
        System.out.println("[" + LocalTime.now().format(HORA) + "] " + texto);
    }

    private static class AtencionCliente implements Runnable {

        private final Socket socket;
        private PrintWriter salida;
        private String nombre;

        AtencionCliente(Socket socket, int numero) {
            this.socket = socket;
            this.nombre = "cliente-" + numero;
        }

        @Override
        public void run() {

            try (Socket s = socket;
                 BufferedReader entrada = new BufferedReader(
                         new InputStreamReader(s.getInputStream()));
                 PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {

                salida = out;

                salida.println("Bienvenido al chat. Escriba su nombre:");
                String nombreRecibido = entrada.readLine();
                if (nombreRecibido == null) {
                    return;
                }
                if (!nombreRecibido.isBlank()) {
                    nombre = nombreRecibido.trim();
                }

                clientes.add(this);

                salida.println("Hola " + nombre + "! Hay " + clientes.size()
                        + " usuario(s) conectado(s). Escriba /salir para irse.");
                difundir("*** " + nombre + " se unio al chat ***", this);
                log(nombre + " entro al chat (hilo \"" + Thread.currentThread().getName()
                        + "\"). Conectados: " + clientes.size());

                String linea;
                while ((linea = entrada.readLine()) != null) {

                    if (linea.equalsIgnoreCase("/salir")) {
                        break;
                    }
                    if (linea.isBlank()) {
                        continue;
                    }

                    log("Mensaje de " + nombre + ": \"" + linea + "\" -> reenviado a "
                            + (clientes.size() - 1) + " cliente(s)");
                    difundir("[" + nombre + "] " + linea, this);
                }

            } catch (IOException e) {
                log("Conexion con " + nombre + " cortada de golpe: " + e.getMessage());

            } finally {
                if (clientes.remove(this)) {
                    difundir("*** " + nombre + " salio del chat ***", this);
                }
                log(nombre + " se desconecto. Socket cerrado. Conectados: " + clientes.size());
            }
        }

        synchronized void enviar(String mensaje) {
            if (salida != null) {
                salida.println(mensaje);
            }
        }
    }
}
