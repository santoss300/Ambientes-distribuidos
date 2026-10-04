/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorTransacciones {

    private static final int PUERTO = 7000;

    public static void main(String[] args) {

        try (ServerSocket servidor = new ServerSocket(PUERTO)) {

            System.out.println("=========================================");
            System.out.println("  SERVIDOR DE TRANSACCIONES");
            System.out.println("=========================================");
            System.out.println("Escuchando en el puerto " + PUERTO + "...");
            System.out.println();

            while (true) {

                try (Socket socket = servidor.accept();
                     DataInputStream entrada = new DataInputStream(socket.getInputStream())) {

                    String formato = entrada.readUTF();
                    int cantidad = entrada.readInt();

                    long totalBytes = 0;
                    Transaccion ultima = null;
                    long inicio = System.nanoTime();

                    for (int i = 0; i < cantidad; i++) {
                        int largo = entrada.readInt();
                        byte[] datos = new byte[largo];
                        entrada.readFully(datos);
                        totalBytes += largo;

                        if (formato.equals("JSON")) {
                            ultima = ParserMensajes.desdeJson(new String(datos, "UTF-8"));
                        } else {
                            ultima = ParserMensajes.desdeBinario(datos);
                        }
                    }

                    double ms = (System.nanoTime() - inicio) / 1_000_000.0;

                    System.out.println("Formato " + formato);
                    System.out.println("  Transacciones recibidas : " + cantidad);
                    System.out.println("  Bytes recibidos         : " + totalBytes);
                    System.out.printf("  Tiempo de recepcion     : %.2f ms%n", ms);
                    System.out.println("  Ultima transaccion      : " + ultima);
                    System.out.println();

                } catch (IOException e) {
                    System.out.println("Error con el cliente: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.println("No se pudo abrir el puerto " + PUERTO + ": " + e.getMessage());
        }
    }
}
