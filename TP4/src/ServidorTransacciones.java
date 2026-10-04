/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorTransacciones {

    private static final int PUERTO_POR_DEFECTO = 7000;

    static final byte FORMATO_JSON = 'J';
    static final byte FORMATO_BINARIO = 'B';

    private static Resultado ultimoJson;
    private static Resultado ultimoBinario;

    public static void main(String[] args) {

        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : PUERTO_POR_DEFECTO;

        calentar();

        try (ServerSocket servidor = new ServerSocket(puerto)) {

            System.out.println("=================================================");
            System.out.println("  SERVIDOR DE TRANSACCIONES (TCP)");
            System.out.println("=================================================");
            System.out.println("Escuchando en el puerto " + puerto + "...");
            System.out.println("(Ctrl+C para detener)");
            System.out.println();

            while (true) {

                Socket conexion = servidor.accept();

                try (Socket socket = conexion;
                     DataInputStream entrada = new DataInputStream(
                             new BufferedInputStream(socket.getInputStream()));
                     DataOutputStream salida = new DataOutputStream(
                             new BufferedOutputStream(socket.getOutputStream()))) {

                    System.out.println("Cliente conectado desde "
                            + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());

                    Resultado r = recibir(entrada);

                    salida.writeInt(r.cantidad);
                    salida.writeLong(r.bytesUtiles);
                    salida.flush();

                    imprimir(r);

                    if (r.formato == FORMATO_JSON) {
                        ultimoJson = r;
                    } else {
                        ultimoBinario = r;
                    }
                    if (ultimoJson != null && ultimoBinario != null) {
                        imprimirComparacion();
                        ultimoJson = null;
                        ultimoBinario = null;
                    }

                } catch (EOFException e) {
                    System.out.println("  !! El cliente corto la conexion antes de terminar.");
                    System.out.println();
                } catch (IOException | IllegalArgumentException e) {
                    System.out.println("  !! Error con el cliente: " + e.getMessage());
                    System.out.println();
                }
            }

        } catch (IOException e) {
            System.err.println("No se pudo abrir el puerto " + puerto + ": " + e.getMessage());
            System.err.println("Verifique que no haya otro servidor ya ejecutandose.");
        }
    }

    private static Resultado recibir(DataInputStream entrada) throws IOException {

        byte formato = entrada.readByte();
        if (formato != FORMATO_JSON && formato != FORMATO_BINARIO) {
            throw new IllegalArgumentException("Formato desconocido: " + (char) formato);
        }
        int cantidad = entrada.readInt();

        Resultado r = new Resultado(formato);
        long inicio = System.nanoTime();

        for (int i = 0; i < cantidad; i++) {

            int largo = entrada.readInt();
            byte[] datos = new byte[largo];
            entrada.readFully(datos);

            long t0 = System.nanoTime();
            Transaccion t = formato == FORMATO_JSON
                    ? ParserMensajes.desdeJsonBytes(datos)
                    : ParserMensajes.desdeBinario(datos);
            r.nanosUnmarshalling += System.nanoTime() - t0;

            if (i == 0) {
                r.primera = t;
            }
            r.cantidad++;
            r.bytesUtiles += largo;
            r.sumaMontos += t.getMonto();
        }

        r.nanosTotal = System.nanoTime() - inicio;
        return r;
    }

    private static void imprimir(Resultado r) {
        System.out.println("  Formato recibido          : " + nombre(r.formato));
        System.out.println("  Transacciones recibidas   : " + r.cantidad);
        System.out.println("  Carga util recibida       : " + r.bytesUtiles + " bytes");
        System.out.println("  Tiempo de unmarshalling   : " + ms(r.nanosUnmarshalling));
        System.out.println("  Tiempo total (leer+armar) : " + ms(r.nanosTotal));
        System.out.printf("  Suma de montos (control)  : %.2f%n", r.sumaMontos);
        System.out.println("  Primera transaccion       : " + r.primera);
        System.out.println();
    }

    private static void imprimirComparacion() {
        System.out.println("=================================================");
        System.out.println("  COMPARACION EN EL SERVIDOR");
        System.out.println("=================================================");
        System.out.printf("  %-22s %12s %12s%n", "", "JSON", "BINARIO");
        System.out.printf("  %-22s %12d %12d%n", "Bytes recibidos",
                ultimoJson.bytesUtiles, ultimoBinario.bytesUtiles);
        System.out.printf("  %-22s %12s %12s%n", "Unmarshalling",
                ms(ultimoJson.nanosUnmarshalling), ms(ultimoBinario.nanosUnmarshalling));
        System.out.printf("  %-22s %12s %12s%n", "Total (leer+armar)",
                ms(ultimoJson.nanosTotal), ms(ultimoBinario.nanosTotal));
        boolean mismosDatos = Math.abs(ultimoJson.sumaMontos - ultimoBinario.sumaMontos) < 0.001
                && ultimoJson.cantidad == ultimoBinario.cantidad;
        System.out.println("  Mismos datos en ambos formatos: " + (mismosDatos ? "SI" : "NO"));
        System.out.println("=================================================");
        System.out.println();
    }

    private static void calentar() {
        Transaccion t = new Transaccion(1, "Calentamiento", 1234.56, System.currentTimeMillis());
        byte[] json = ParserMensajes.aJsonBytes(t);
        byte[] bin = ParserMensajes.aBinario(t);
        for (int i = 0; i < 20000; i++) {
            ParserMensajes.desdeJsonBytes(json);
            ParserMensajes.desdeBinario(bin);
        }
    }

    static String nombre(byte formato) {
        return formato == FORMATO_JSON ? "JSON" : "BINARIO";
    }

    static String ms(long nanos) {
        return String.format("%.3f ms", nanos / 1_000_000.0);
    }

    private static class Resultado {
        final byte formato;
        int cantidad;
        long bytesUtiles;
        long nanosUnmarshalling;
        long nanosTotal;
        double sumaMontos;
        Transaccion primera;

        Resultado(byte formato) {
            this.formato = formato;
        }
    }
}
