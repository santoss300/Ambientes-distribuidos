/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorInestable {

    private static final int PUERTO = 5500;

    private static final String SEPARADOR = ";";

    private static final int FALLOS_A_SIMULAR = 3;

    public static void main(String[] args) {

        int fallosASimular = args.length > 0
                ? Integer.parseInt(args[0])
                : FALLOS_A_SIMULAR;

        try (ServerSocket servidor = new ServerSocket(PUERTO)) {

            System.out.println("=================================================");
            System.out.println("  SERVIDOR INESTABLE - CALCULADORA DISTRIBUIDA");
            System.out.println("=================================================");
            System.out.println("Escuchando en el puerto " + PUERTO + "...");
            System.out.println("Fallos transitorios a simular: " + fallosASimular);
            System.out.println("(Ctrl+C para detener)");
            System.out.println();

            int numeroDePeticion = 0;

            while (true) {

                Socket conexion = servidor.accept();

                numeroDePeticion++;

                System.out.println("[" + numeroDePeticion + "] Cliente conectado desde "
                        + conexion.getInetAddress().getHostAddress()
                        + ":" + conexion.getPort());

                try (Socket socket = conexion;
                     BufferedReader entrada = new BufferedReader(
                             new InputStreamReader(socket.getInputStream()));
                     PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)) {

                    String peticion = entrada.readLine();

                    if (peticion == null) {
                        System.out.println("    El cliente cerro la conexion sin enviar datos.");
                        System.out.println();
                        continue;
                    }

                    System.out.println("    Peticion recibida: \"" + peticion + "\"");

                    String respuesta;

                    if (numeroDePeticion <= fallosASimular) {
                        respuesta = "ERROR: 503 Servicio no disponible (fallo transitorio simulado)";
                        System.out.println("    >> SOBRECARGA SIMULADA: se rechaza la peticion.");
                    } else {
                        respuesta = procesar(peticion);
                    }

                    System.out.println("    Respuesta enviada : \"" + respuesta + "\"");
                    System.out.println();

                    salida.println(respuesta);

                } catch (IOException e) {
                    System.out.println("    Error de E/S con el cliente: " + e.getMessage());
                    System.out.println();
                }
            }

        } catch (IOException e) {
            System.err.println("No se pudo abrir el puerto " + PUERTO + ": " + e.getMessage());
            System.err.println("Verifique que no haya otro servidor ya ejecutandose.");
        }
    }

    private static String procesar(String peticion) {

        String[] partes = peticion.split(SEPARADOR, -1);

        if (partes.length != 3) {
            return "ERROR: 400 Formato invalido. Se esperaba numero1;operador;numero2";
        }

        int a;
        int b;
        try {
            a = Integer.parseInt(partes[0].trim());
            b = Integer.parseInt(partes[2].trim());
        } catch (NumberFormatException e) {
            return "ERROR: 400 Los operandos deben ser numeros enteros";
        }

        String operador = partes[1].trim();

        switch (operador) {
            case "+":
                return formatear((double) a + b);
            case "-":
                return formatear((double) a - b);
            case "*":
                return formatear((double) a * b);
            case "/":
                if (b == 0) {
                    return "ERROR: 400 Division por cero";
                }
                return formatear((double) a / b);
            default:
                return "ERROR: 400 Operador no soportado. Use + - * /";
        }
    }

    private static String formatear(double valor) {
        if (valor == Math.rint(valor) && !Double.isInfinite(valor)) {
            return String.valueOf((long) valor);
        }
        return String.valueOf(valor);
    }
}
