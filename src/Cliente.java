import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Cliente de la Calculadora Distribuida.
 *
 * Trabajo Practico N 1 - Desarrollo de Aplicaciones para Ambientes Distribuidos
 * Tema: Arquitectura Cliente-Servidor y comunicacion mediante Sockets TCP.
 *
 * Modelo de conexion: UNA CONEXION POR OPERACION.
 *   Por cada calculo se abre un socket, se envia la peticion, se lee la
 *   respuesta y se cierra. El ciclo de vida completo del socket queda a la
 *   vista en cada iteracion.
 *
 * Uso:
 *   java Cliente                       -> se conecta a 127.0.0.1:5500
 *   java Cliente <host>                -> se conecta a <host>:5500
 *   java Cliente <host> <puerto>       -> se conecta a <host>:<puerto>
 *
 * El host es parametrizable justamente para poder ejecutar el servidor en
 * otra notebook de la red (ver INFORME.md, pregunta 3).
 */
public class Cliente {

    private static final String HOST_POR_DEFECTO = "127.0.0.1";
    private static final int PUERTO_POR_DEFECTO = 5500;

    /** Separador de campos del protocolo, debe coincidir con el del servidor. */
    private static final String SEPARADOR = ";";

    public static void main(String[] args) {

        // Host y puerto se toman de los argumentos si vienen; si no, valores por defecto.
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

            // ---------- 1. Se piden los datos al usuario por consola ----------
            int numero1 = leerEntero(teclado, "Ingrese el primer numero entero : ");
            String operador = leerOperador(teclado);
            int numero2 = leerEntero(teclado, "Ingrese el segundo numero entero: ");

            // ---------- 2. Se empaquetan en el formato del protocolo ----------
            // Los datos dejan de ser variables en memoria y pasan a ser una
            // cadena de texto capaz de viajar por la red. Esto es serializar.
            String peticion = numero1 + SEPARADOR + operador + SEPARADOR + numero2;

            System.out.println();
            System.out.println("  -> Enviando al servidor: \"" + peticion + "\"");

            // ---------- 3. Se abre la conexion y se hace el intercambio ----------
            enviarPeticion(host, puerto, peticion);

            // ---------- 4. Se pregunta si se desea hacer otra operacion ----------
            System.out.print("Desea realizar otra operacion? (s/n): ");
            String respuesta = teclado.nextLine().trim().toLowerCase();
            continuar = respuesta.startsWith("s");
            System.out.println();
        }

        System.out.println("Cliente finalizado.");
        teclado.close();
    }

    /**
     * Abre un socket, envia la peticion, espera la respuesta, la imprime y cierra.
     * Todo el manejo de errores de red esta concentrado aca.
     */
    private static void enviarPeticion(String host, int puerto, String peticion) {

        // El try-with-resources garantiza el cierre del socket y de los streams,
        // incluso si se lanza una excepcion en el medio.
        try (Socket socket = new Socket(host, puerto);   // <-- aqui ocurre el handshake TCP
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {

            // Envio de la peticion. println() agrega el salto de linea que el
            // readLine() del servidor necesita para dar la linea por terminada.
            salida.println(peticion);

            // -------------------------------------------------------------
            // LINEA BLOQUEANTE (lado cliente)
            // readLine() detiene la ejecucion hasta que llegue la respuesta
            // del servidor por la red. El programa no consume CPU aqui:
            // el hilo queda dormido esperando un evento de red.
            // -------------------------------------------------------------
            String resultado = entrada.readLine();

            if (resultado == null) {
                System.out.println("  <- El servidor cerro la conexion sin responder.");
            } else if (resultado.startsWith("ERROR:")) {
                System.out.println("  <- El servidor respondio: " + resultado);
            } else {
                System.out.println("  <- RESULTADO: " + resultado);
            }

        } catch (ConnectException e) {
            // Caso de la pregunta 1 del Ejercicio 2: el servidor no esta levantado.
            // TCP responde el intento de conexion con un RST y Java lo traduce a
            // esta excepcion (subclase de SocketException -> IOException).
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
            // El nombre de host no se pudo resolver a una direccion IP.
            System.out.println("  !! Host desconocido: " + host);
            System.out.println("     Excepcion: " + e.getClass().getName());

        } catch (IOException e) {
            // Cualquier otro fallo de red durante el intercambio.
            System.out.println("  !! Error de entrada/salida: " + e.getMessage());
            System.out.println("     Excepcion: " + e.getClass().getName());
        }
    }

    /** Pide un entero por consola y no avanza hasta recibir uno valido. */
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

    /** Pide el operador por consola y valida que sea uno de los cuatro admitidos. */
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
