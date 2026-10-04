/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;

public class ClienteTransacciones {

    private static final String HOST = "127.0.0.1";
    private static final int PUERTO = 7000;
    private static final int CANTIDAD = 1000;

    public static void main(String[] args) throws IOException {

        Transaccion[] transacciones = new Transaccion[CANTIDAD];
        String[] origenes = {"NodoA", "NodoB", "NodoC"};
        Random random = new Random();

        for (int i = 0; i < CANTIDAD; i++) {
            String origen = origenes[random.nextInt(origenes.length)];
            double monto = Math.round(random.nextDouble() * 100000) / 100.0;
            transacciones[i] = new Transaccion(101 + i, origen, monto, System.currentTimeMillis());
        }

        System.out.println("=========================================");
        System.out.println("  CLIENTE DE TRANSACCIONES");
        System.out.println("=========================================");
        System.out.println("Ejemplo en JSON: " + ParserMensajes.aJson(transacciones[0]));
        System.out.println();

        long[] json = enviar(transacciones, "JSON");
        long[] binario = enviar(transacciones, "BINARIO");

        System.out.println("=========================================");
        System.out.println("  RESUMEN");
        System.out.println("=========================================");
        System.out.printf("JSON    : %6d bytes  |  %6.2f ms%n", json[0], json[1] / 1_000_000.0);
        System.out.printf("BINARIO : %6d bytes  |  %6.2f ms%n", binario[0], binario[1] / 1_000_000.0);
        System.out.printf("El binario ocupa un %.0f%% menos que JSON%n",
                100.0 * (json[0] - binario[0]) / json[0]);
    }

    private static long[] enviar(Transaccion[] transacciones, String formato) throws IOException {

        long totalBytes = 0;
        long inicio = System.nanoTime();

        try (Socket socket = new Socket(HOST, PUERTO);
             DataOutputStream salida = new DataOutputStream(socket.getOutputStream())) {

            salida.writeUTF(formato);
            salida.writeInt(transacciones.length);

            for (Transaccion t : transacciones) {
                byte[] datos;
                if (formato.equals("JSON")) {
                    datos = ParserMensajes.aJson(t).getBytes("UTF-8");
                } else {
                    datos = ParserMensajes.aBinario(t);
                }
                salida.writeInt(datos.length);
                salida.write(datos);
                totalBytes += datos.length;
            }
            salida.flush();
        }

        long tiempo = System.nanoTime() - inicio;

        System.out.println("Formato " + formato + ": " + transacciones.length + " transacciones enviadas");
        System.out.println("  Bytes enviados               : " + totalBytes);
        System.out.printf("  Tiempo serializacion + envio : %.2f ms%n", tiempo / 1_000_000.0);
        System.out.println();

        return new long[] {totalBytes, tiempo};
    }
}
