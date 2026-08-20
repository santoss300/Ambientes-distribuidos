/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {

    private static final int PUERTO = 5500;

    private static final String SEPARADOR = ";";

    public static void main(String[] args) {

        try (ServerSocket servidor = new ServerSocket(PUERTO)) {

            System.out.println("=========================================");
            System.out.println("  SERVIDOR DE CALCULADORA DISTRIBUIDA");
            System.out.println("=========================================");
            System.out.println("Escuchando en el puerto " + PUERTO + "...");
            System.out.println("(Ctrl+C para detener)");
            System.out.println();

            int numeroDeCliente = 0;

            while (true) {

                Socket conexion = servidor.accept();

                numeroDeCliente++;
                System.out.println("[" + numeroDeCliente + "] Cliente conectado desde "
                        + conexion.getInetAddress().getHostAddress()
                        + ":" + conexion.getPort());

                try (Socket socket = conexion;
                     BufferedReader entrada = new BufferedReader(
                             new InputStreamReader(socket.getInputStream()));
                     PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)) {

                    String peticion = entrada.readLine();

                    if (peticion == null) {
                        System.out.println("    El cliente cerro la conexion sin enviar datos.");
                        continue;
                    }

                    System.out.println("    Peticion recibida: \"" + peticion + "\"");

                    String respuesta = procesar(peticion);

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
            return "ERROR: Formato invalido. Se esperaba numero1;operador;numero2";
        }

        int a;
        int b;
        try {
            a = Integer.parseInt(partes[0].trim());
            b = Integer.parseInt(partes[2].trim());
        } catch (NumberFormatException e) {
            return "ERROR: Los operandos deben ser numeros enteros";
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
                    return "ERROR: Division por cero";
                }
                return formatear((double) a / b);
            default:
                return "ERROR: Operador no soportado. Use + - * /";
        }
    }

    private static String formatear(double valor) {
        if (valor == Math.rint(valor) && !Double.isInfinite(valor)) {
            return String.valueOf((long) valor);
        }
        return String.valueOf(valor);
    }
}
