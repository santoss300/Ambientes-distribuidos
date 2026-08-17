import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor de la Calculadora Distribuida.
 *
 * Trabajo Practico N 1 - Desarrollo de Aplicaciones para Ambientes Distribuidos
 * Tema: Arquitectura Cliente-Servidor y comunicacion mediante Sockets TCP.
 *
 * Modelo de atencion: SECUENCIAL EN BUCLE.
 *   Atiende un cliente completo, cierra esa conexion y recien ahi vuelve a
 *   quedar a la escucha del siguiente. No usa hilos.
 *
 * Protocolo de aplicacion (texto plano, una linea, terminada en salto de linea):
 *   Peticion  ->  "numero1;operador;numero2"      ejemplo: "15;+;30"
 *   Respuesta ->  el resultado                    ejemplo: "45"
 *                 o un mensaje de error controlado empezando con "ERROR:"
 */
public class Servidor {

    /** Puerto de escucha acordado con el cliente. */
    private static final int PUERTO = 5500;

    /** Separador de campos del protocolo. */
    private static final String SEPARADOR = ";";

    public static void main(String[] args) {

        // try-with-resources: el ServerSocket se cierra solo al salir del bloque.
        // Crear el ServerSocket es lo que efectivamente RESERVA el puerto 5500 en
        // el sistema operativo. Si otro proceso ya lo tiene tomado, aca se lanza
        // una BindException.
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {

            System.out.println("=========================================");
            System.out.println("  SERVIDOR DE CALCULADORA DISTRIBUIDA");
            System.out.println("=========================================");
            System.out.println("Escuchando en el puerto " + PUERTO + "...");
            System.out.println("(Ctrl+C para detener)");
            System.out.println();

            int numeroDeCliente = 0;

            // Bucle principal: el servidor nunca termina por si solo, queda
            // atendiendo clientes uno detras del otro.
            while (true) {

                // ---------------------------------------------------------
                // LINEA BLOQUEANTE N 1 (lado servidor)
                // accept() duerme el hilo hasta que un cliente completa el
                // handshake TCP contra este puerto. Devuelve un Socket ya
                // conectado, dedicado a ESE cliente.
                // ---------------------------------------------------------
                Socket conexion = servidor.accept();

                numeroDeCliente++;
                System.out.println("[" + numeroDeCliente + "] Cliente conectado desde "
                        + conexion.getInetAddress().getHostAddress()
                        + ":" + conexion.getPort());

                // El try-with-resources cierra el socket y sus streams al terminar.
                // Como el modelo es "una conexion por operacion", atendemos una
                // sola peticion y cerramos.
                try (Socket socket = conexion;
                     BufferedReader entrada = new BufferedReader(
                             new InputStreamReader(socket.getInputStream()));
                     PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)) {

                    // -----------------------------------------------------
                    // LINEA BLOQUEANTE N 2 (lado servidor)
                    // readLine() espera hasta que llegue una linea completa
                    // desde la red. Devuelve null si el cliente cerro la
                    // conexion sin mandar nada.
                    // -----------------------------------------------------
                    String peticion = entrada.readLine();

                    if (peticion == null) {
                        System.out.println("    El cliente cerro la conexion sin enviar datos.");
                        continue; // vuelve al accept()
                    }

                    System.out.println("    Peticion recibida: \"" + peticion + "\"");

                    // Se procesa la cadena y se obtiene la respuesta ya lista.
                    String respuesta = procesar(peticion);

                    System.out.println("    Respuesta enviada : \"" + respuesta + "\"");
                    System.out.println();

                    // println() sobre un PrintWriter con autoFlush=true empuja
                    // los bytes a la red inmediatamente, y agrega el salto de
                    // linea que el readLine() del cliente esta esperando.
                    salida.println(respuesta);

                } catch (IOException e) {
                    // Un error con UN cliente no debe tumbar al servidor:
                    // se informa y se sigue atendiendo a los demas.
                    System.out.println("    Error de E/S con el cliente: " + e.getMessage());
                    System.out.println();
                }
            }

        } catch (IOException e) {
            System.err.println("No se pudo abrir el puerto " + PUERTO + ": " + e.getMessage());
            System.err.println("Verifique que no haya otro servidor ya ejecutandose.");
        }
    }

    /**
     * Parsea la cadena del protocolo, ejecuta la operacion y devuelve
     * el texto de la respuesta.
     *
     * Nunca propaga excepciones: todo error se traduce a un mensaje
     * controlado que empieza con "ERROR:", para que el cliente siempre
     * reciba una respuesta y no quede colgado esperando.
     */
    private static String procesar(String peticion) {

        // -1 como limite evita que split() descarte campos vacios al final,
        // asi detectamos una peticion mal formada como "15;+;".
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
                // Caso pedido explicitamente por la consigna.
                // Se valida ANTES de dividir: si dejaramos que Java hiciera
                // 15/0 con enteros, saltaria una ArithmeticException y el
                // cliente se quedaria sin respuesta.
                if (b == 0) {
                    return "ERROR: Division por cero";
                }
                return formatear((double) a / b);
            default:
                return "ERROR: Operador no soportado. Use + - * /";
        }
    }

    /** Muestra 45 en vez de 45.0, pero conserva los decimales cuando importan (7.5). */
    private static String formatear(double valor) {
        if (valor == Math.rint(valor) && !Double.isInfinite(valor)) {
            return String.valueOf((long) valor);
        }
        return String.valueOf(valor);
    }
}
