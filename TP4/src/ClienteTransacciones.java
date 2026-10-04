/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ClienteTransacciones {

    private static final String HOST_POR_DEFECTO = "127.0.0.1";
    private static final int PUERTO_POR_DEFECTO = 7000;
    private static final int CANTIDAD_POR_DEFECTO = 1000;

    private static final String[] ORIGENES = {"NodoA", "NodoB", "NodoC", "SucursalCentro", "CajeroNorte"};

    public static void main(String[] args) {

        String host = args.length > 0 ? args[0] : HOST_POR_DEFECTO;
        int puerto = args.length > 1 ? Integer.parseInt(args[1]) : PUERTO_POR_DEFECTO;
        int cantidad = args.length > 2 ? Integer.parseInt(args[2]) : CANTIDAD_POR_DEFECTO;

        System.out.println("=================================================");
        System.out.println("  CLIENTE DE TRANSACCIONES (TCP)");
        System.out.println("=================================================");
        System.out.println("Servidor destino : " + host + ":" + puerto);
        System.out.println("Transacciones    : " + cantidad + " (las mismas en los dos formatos)");
        System.out.println();

        List<Transaccion> lote = generar(cantidad);

        Transaccion ejemplo = lote.get(0);
        byte[] ejemploBinario = ParserMensajes.aBinario(ejemplo);
        System.out.println("Ejemplo: " + ejemplo);
        System.out.println("  JSON    (" + ParserMensajes.aJsonBytes(ejemplo).length + " bytes): "
                + ParserMensajes.aJson(ejemplo));
        System.out.println("  Binario (" + ejemploBinario.length + " bytes): " + hex(ejemploBinario));
        System.out.println();

        calentar(lote);

        try {
            Medicion json = enviar(host, puerto, lote, ServidorTransacciones.FORMATO_JSON);
            Medicion binario = enviar(host, puerto, lote, ServidorTransacciones.FORMATO_BINARIO);
            imprimirComparacion(json, binario);

        } catch (ConnectException e) {
            System.err.println("No se pudo conectar: el servidor no esta disponible en "
                    + host + ":" + puerto);
        } catch (IOException e) {
            System.err.println("Error de comunicacion: " + e.getMessage());
        }
    }

    private static Medicion enviar(String host, int puerto, List<Transaccion> lote, byte formato)
            throws IOException {

        Medicion m = new Medicion(lote.size());

        try (Socket socket = new Socket(host, puerto);
             DataOutputStream salida = new DataOutputStream(
                     new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream entrada = new DataInputStream(
                     new BufferedInputStream(socket.getInputStream()))) {

            System.out.println("--- Enviando rafaga en formato " + ServidorTransacciones.nombre(formato) + " ---");

            long inicio = System.nanoTime();

            salida.writeByte(formato);
            salida.writeInt(lote.size());

            for (Transaccion t : lote) {

                long t0 = System.nanoTime();
                byte[] datos = formato == ServidorTransacciones.FORMATO_JSON
                        ? ParserMensajes.aJsonBytes(t)
                        : ParserMensajes.aBinario(t);
                long t1 = System.nanoTime();

                salida.writeInt(datos.length);
                salida.write(datos);
                long t2 = System.nanoTime();

                m.nanosSerializacion += t1 - t0;
                m.nanosEnvio += t2 - t1;
                m.bytesUtiles += datos.length;
            }

            long t3 = System.nanoTime();
            salida.flush();
            m.nanosEnvio += System.nanoTime() - t3;
            m.nanosSerializacionYEnvio = System.nanoTime() - inicio;

            int confirmadas = entrada.readInt();
            long bytesConfirmados = entrada.readLong();
            m.nanosHastaConfirmacion = System.nanoTime() - inicio;

            m.bytesConEncabezados = 1 + 4 + 4L * lote.size() + m.bytesUtiles;

            System.out.println("  Carga util enviada           : " + m.bytesUtiles + " bytes");
            System.out.println("  Con encabezados de largo     : " + m.bytesConEncabezados + " bytes");
            System.out.println("  Tiempo de serializacion      : " + ServidorTransacciones.ms(m.nanosSerializacion));
            System.out.println("  Tiempo de envio              : " + ServidorTransacciones.ms(m.nanosEnvio));
            System.out.println("  Serializacion + envio        : " + ServidorTransacciones.ms(m.nanosSerializacionYEnvio));
            System.out.println("  Hasta confirmacion servidor  : " + ServidorTransacciones.ms(m.nanosHastaConfirmacion));
            System.out.println("  Servidor confirmo            : " + confirmadas + " transacciones, "
                    + bytesConfirmados + " bytes"
                    + (confirmadas == lote.size() && bytesConfirmados == m.bytesUtiles ? "  [OK]" : "  [NO COINCIDE]"));
            System.out.println();
        }

        return m;
    }

    private static void imprimirComparacion(Medicion json, Medicion bin) {
        System.out.println("=================================================");
        System.out.println("  COMPARACION JSON vs BINARIO");
        System.out.println("=================================================");
        System.out.printf("  %-24s %12s %12s%n", "", "JSON", "BINARIO");
        System.out.printf("  %-24s %12d %12d%n", "Carga util (bytes)", json.bytesUtiles, bin.bytesUtiles);
        System.out.printf("  %-24s %12d %12d%n", "Bytes por transaccion",
                json.bytesUtiles / json.cantidad, bin.bytesUtiles / bin.cantidad);
        System.out.printf("  %-24s %12s %12s%n", "Serializacion",
                ServidorTransacciones.ms(json.nanosSerializacion), ServidorTransacciones.ms(bin.nanosSerializacion));
        System.out.printf("  %-24s %12s %12s%n", "Serializacion + envio",
                ServidorTransacciones.ms(json.nanosSerializacionYEnvio), ServidorTransacciones.ms(bin.nanosSerializacionYEnvio));
        System.out.println("-------------------------------------------------");
        System.out.printf("  El binario ocupa un %.1f%% menos que JSON%n",
                100.0 * (json.bytesUtiles - bin.bytesUtiles) / json.bytesUtiles);
        System.out.printf("  JSON ocupa %.2f veces lo que ocupa el binario%n",
                (double) json.bytesUtiles / bin.bytesUtiles);
        System.out.printf("  Serializar en JSON tardo %.2f veces lo que el binario%n",
                (double) json.nanosSerializacion / bin.nanosSerializacion);
        System.out.println("=================================================");
    }

    private static List<Transaccion> generar(int cantidad) {
        Random random = new Random(42);
        long base = 1_700_000_000_000L;
        List<Transaccion> lote = new ArrayList<>(cantidad);
        for (int i = 0; i < cantidad; i++) {
            int id = 101 + i;
            String origen = ORIGENES[random.nextInt(ORIGENES.length)];
            double monto = Math.round(random.nextDouble() * 10_000_000) / 100.0;
            long timestamp = base + i * 137L + random.nextInt(100);
            lote.add(new Transaccion(id, origen, monto, timestamp));
        }
        return lote;
    }

    private static void calentar(List<Transaccion> lote) {
        for (int r = 0; r < 20; r++) {
            for (Transaccion t : lote) {
                ParserMensajes.aJsonBytes(t);
                ParserMensajes.aBinario(t);
            }
        }
    }

    private static String hex(byte[] datos) {
        StringBuilder sb = new StringBuilder();
        for (byte b : datos) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    private static class Medicion {
        final int cantidad;
        long bytesUtiles;
        long bytesConEncabezados;
        long nanosSerializacion;
        long nanosEnvio;
        long nanosSerializacionYEnvio;
        long nanosHastaConfirmacion;

        Medicion(int cantidad) {
            this.cantidad = cantidad;
        }
    }
}
