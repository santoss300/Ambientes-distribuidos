/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;

public class ClienteResiliente {

    private static final String HOST_POR_DEFECTO = "127.0.0.1";
    private static final int PUERTO_POR_DEFECTO = 5500;
    private static final String PETICION_POR_DEFECTO = "15;+;30";

    private static final int MAX_INTENTOS = 5;
    private static final long BASE_MS = 1000L;
    private static final int JITTER_MAX_MS = 500;
    private static final int TIMEOUT_MS = 5000;

    private static final Random RANDOM = new Random();

    private static int intentosRealizados = 0;
    private static boolean exito = false;
    private static String resultadoFinal = null;
    private static long tiempoTotalMs = 0L;
    private static long tiempoEsperandoMs = 0L;

    public static void main(String[] args) {

        String host = args.length > 0 ? args[0] : HOST_POR_DEFECTO;
        int puerto = args.length > 1 ? Integer.parseInt(args[1]) : PUERTO_POR_DEFECTO;
        String peticion = args.length > 2 ? args[2] : PETICION_POR_DEFECTO;

        System.out.println("=================================================");
        System.out.println("  CLIENTE RESILIENTE - CALCULADORA DISTRIBUIDA");
        System.out.println("=================================================");
        System.out.println("Servidor destino : " + host + ":" + puerto);
        System.out.println("Peticion         : \"" + peticion + "\"");
        System.out.println("Politica         : backoff exponencial + jitter");
        System.out.println("                   espera = " + BASE_MS + " * 2^(intento-1)"
                + " + Random(0, " + JITTER_MAX_MS + ") ms");
        System.out.println("Maximo de intentos: " + MAX_INTENTOS);
        System.out.println();

        long inicio = System.currentTimeMillis();

        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {

            intentosRealizados = intento;

            System.out.println("--- Intento " + intento + " de " + MAX_INTENTOS + " ---");

            long inicioIntento = System.currentTimeMillis();
            Respuesta respuesta = enviarPeticion(host, puerto, peticion);
            long duracionIntento = System.currentTimeMillis() - inicioIntento;

            System.out.println("    Duracion del intento: " + duracionIntento + " ms");

            if (respuesta.exitosa) {
                exito = true;
                resultadoFinal = respuesta.detalle;
                System.out.println("    <- RESULTADO: " + respuesta.detalle);
                System.out.println();
                break;
            }

            resultadoFinal = respuesta.detalle;
            System.out.println("    !! Fallo: " + respuesta.detalle);

            if (!respuesta.transitoria) {
                System.out.println("    !! Es un fallo PERMANENTE: reintentar no tiene sentido.");
                System.out.println();
                break;
            }

            if (intento == MAX_INTENTOS) {
                System.out.println("    !! Se agotaron los " + MAX_INTENTOS + " intentos.");
                System.out.println();
                break;
            }

            long espera = calcularEspera(intento);
            System.out.println("    .. Esperando " + espera + " ms antes de reintentar.");
            System.out.println();

            dormir(espera);
            tiempoEsperandoMs += espera;
        }

        tiempoTotalMs = System.currentTimeMillis() - inicio;

        mostrarMetricas();
    }

    private static long calcularEspera(int intento) {
        long backoffExponencial = BASE_MS * (long) Math.pow(2, intento - 1);
        int jitter = RANDOM.nextInt(JITTER_MAX_MS + 1);
        System.out.println("       backoff = " + backoffExponencial
                + " ms  |  jitter = " + jitter + " ms");
        return backoffExponencial + jitter;
    }

    private static Respuesta enviarPeticion(String host, int puerto, String peticion) {

        try (Socket socket = new Socket()) {

            socket.connect(new InetSocketAddress(host, puerto), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            System.out.println("    -> Enviando: \"" + peticion + "\"");
            salida.println(peticion);

            String linea = entrada.readLine();

            if (linea == null) {
                return Respuesta.fallo(
                        "El servidor cerro la conexion sin responder", true);
            }

            if (linea.startsWith("ERROR: 503")) {
                return Respuesta.fallo(linea, true);
            }

            if (linea.startsWith("ERROR:")) {
                return Respuesta.fallo(linea, false);
            }

            return Respuesta.exito(linea);

        } catch (IOException e) {
            return Respuesta.fallo(
                    e.getClass().getName() + ": " + e.getMessage(), true);
        }
    }

    private static void dormir(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void mostrarMetricas() {
        System.out.println("=================================================");
        System.out.println("  METRICAS DE RESILIENCIA");
        System.out.println("=================================================");
        System.out.println("Estado final de la peticion : "
                + (exito ? "EXITO" : "FALLO DEFINITIVO"));
        System.out.println("Detalle                     : " + resultadoFinal);
        System.out.println("Cantidad de intentos        : " + intentosRealizados);
        System.out.println("Tiempo total acumulado      : " + tiempoTotalMs + " ms");
        System.out.println("  - esperando (backoff)     : " + tiempoEsperandoMs + " ms");
        System.out.println("  - comunicacion efectiva   : "
                + (tiempoTotalMs - tiempoEsperandoMs) + " ms");
        System.out.println("=================================================");
    }

    private static class Respuesta {

        final boolean exitosa;
        final boolean transitoria;
        final String detalle;

        private Respuesta(boolean exitosa, boolean transitoria, String detalle) {
            this.exitosa = exitosa;
            this.transitoria = transitoria;
            this.detalle = detalle;
        }

        static Respuesta exito(String detalle) {
            return new Respuesta(true, false, detalle);
        }

        static Respuesta fallo(String detalle, boolean transitoria) {
            return new Respuesta(false, transitoria, detalle);
        }
    }
}
