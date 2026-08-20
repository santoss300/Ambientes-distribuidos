/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Cliente {

    private static final String HOST_POR_DEFECTO = "127.0.0.1";
    private static final int PUERTO_POR_DEFECTO = 5500;

    private static final String SEPARADOR = ";";

    public static void main(String[] args) {

        String host = args.length > 0 ? args[0] : HOST_POR_DEFECTO;
        int puerto = args.length > 1 ? Integer.parseInt(args[1]) : PUERTO_POR_DEFECTO;

        Scanner teclado = new Scanner(System.in);

        System.out.println("=========================================");
        System.out.println("  CLIENTE DE CALCULADORA DISTRIBUIDA");
        System.out.println("=========================================");
        System.out.println("Servidor destino: " + host + ":" + puerto);
        System.out.println();

        boolean continuar = true;

        while (continuar) {

            int numero1 = leerEntero(teclado, "Ingrese el primer numero entero : ");
            String operador = leerOperador(teclado);
            int numero2 = leerEntero(teclado, "Ingrese el segundo numero entero: ");

            String peticion = numero1 + SEPARADOR + operador + SEPARADOR + numero2;

            System.out.println();
            System.out.println("  -> Enviando al servidor: \"" + peticion + "\"");

            enviarPeticion(host, puerto, peticion);

            System.out.print("Desea realizar otra operacion? (s/n): ");
            String respuesta = teclado.nextLine().trim().toLowerCase();
            continuar = respuesta.startsWith("s");
            System.out.println();
        }

        System.out.println("Cliente finalizado.");
        teclado.close();
    }

    private static void enviarPeticion(String host, int puerto, String peticion) {

        try (Socket socket = new Socket(host, puerto);
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {

            salida.println(peticion);

            String resultado = entrada.readLine();

            if (resultado == null) {
                System.out.println("  <- El servidor cerro la conexion sin responder.");
            } else if (resultado.startsWith("ERROR:")) {
                System.out.println("  <- El servidor respondio: " + resultado);
            } else {
                System.out.println("  <- RESULTADO: " + resultado);
            }

        } catch (ConnectException e) {
            System.out.println();
            System.out.println("  !! NO SE PUDO CONECTAR CON EL SERVIDOR.");
            System.out.println("     Excepcion: " + e.getClass().getName());
            System.out.println("     Mensaje  : " + e.getMessage());
            System.out.println("     Verifique que el servidor este ejecutandose en "
                    + host + ":" + puerto);
            System.out.println();
            System.out.println("     --- Stack trace completo ---");
            e.printStackTrace(System.out);
            System.out.println();

        } catch (UnknownHostException e) {
            System.out.println("  !! Host desconocido: " + host);
            System.out.println("     Excepcion: " + e.getClass().getName());

        } catch (IOException e) {
            System.out.println("  !! Error de entrada/salida: " + e.getMessage());
            System.out.println("     Excepcion: " + e.getClass().getName());
        }
    }

    private static int leerEntero(Scanner teclado, String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = teclado.nextLine().trim();
            try {
                return Integer.parseInt(linea);
            } catch (NumberFormatException e) {
                System.out.println("  Valor invalido. Escriba un numero entero.");
            }
        }
    }

    private static String leerOperador(Scanner teclado) {
        while (true) {
            System.out.print("Ingrese la operacion (+, -, *, /)  : ");
            String linea = teclado.nextLine().trim();
            if (linea.equals("+") || linea.equals("-")
                    || linea.equals("*") || linea.equals("/")) {
                return linea;
            }
            System.out.println("  Operacion invalida. Use +, -, * o /.");
        }
    }
}
