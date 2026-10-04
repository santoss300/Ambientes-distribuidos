/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Scanner;

public class Cliente {

    private static String host;
    private static ServicioRemoto servicio;

    public static void main(String[] args) {

        host = args.length > 0 ? args[0] : "127.0.0.1";
        Scanner teclado = new Scanner(System.in);

        System.out.println("=================================================");
        System.out.println("  CLIENTE RMI DE PROCESAMIENTO");
        System.out.println("=================================================");

        if (!conectar()) {
            return;
        }

        while (true) {
            System.out.println();
            System.out.println("1) Calcular estadisticas de una lista de numeros");
            System.out.println("2) Validar un CUIT");
            System.out.println("3) Filtrar textos");
            System.out.println("0) Salir");
            System.out.print("Opcion: ");

            if (!teclado.hasNextLine()) {
                break;
            }
            String opcion = teclado.nextLine().trim();
            if (opcion.equals("0")) {
                break;
            }

            try {
                switch (opcion) {
                    case "1":
                        System.out.print("Ingrese los numeros separados por espacios: ");
                        String[] partes = teclado.nextLine().trim().split("\\s+");
                        double[] numeros = new double[partes.length];
                        for (int i = 0; i < partes.length; i++) {
                            numeros[i] = Double.parseDouble(partes[i]);
                        }
                        System.out.println("-> " + servicio.calcularEstadisticas(numeros));
                        break;

                    case "2":
                        System.out.print("Ingrese el CUIT (ej. 20-12345678-6): ");
                        String cuit = teclado.nextLine();
                        boolean valido = servicio.validarCuit(cuit);
                        System.out.println("-> El CUIT " + cuit + (valido ? " es VALIDO" : " es INVALIDO"));
                        break;

                    case "3":
                        System.out.print("Ingrese los textos separados por comas: ");
                        String[] textos = teclado.nextLine().split(",");
                        for (int i = 0; i < textos.length; i++) {
                            textos[i] = textos[i].trim();
                        }
                        System.out.print("Ingrese el patron a buscar: ");
                        String patron = teclado.nextLine().trim();
                        String[] encontrados = servicio.filtrarTextos(textos, patron);
                        System.out.println("-> Coinciden " + encontrados.length + ": " + Arrays.toString(encontrados));
                        break;

                    default:
                        System.out.println("Opcion invalida.");
                }

            } catch (NumberFormatException e) {
                System.out.println("!! Solo se pueden ingresar numeros.");

            } catch (RemoteException e) {
                System.out.println("!! Error remoto: " + e.getClass().getName());
                System.out.println("   " + String.valueOf(e.getMessage()).split(";")[0]);
                System.out.println("   Intentando reconectar...");
                conectar();
            }
        }

        System.out.println("Chau!");
    }

    private static boolean conectar() {
        try {
            Registry registro = LocateRegistry.getRegistry(host, Servidor.PUERTO);
            servicio = (ServicioRemoto) registro.lookup(Servidor.NOMBRE_SERVICIO);
            System.out.println(servicio.conectar(System.getProperty("user.name")));
            return true;

        } catch (RemoteException | NotBoundException e) {
            System.out.println("!! No se pudo conectar con el servidor en " + host + ":" + Servidor.PUERTO);
            System.out.println("   (" + e.getClass().getName() + ")");
            return false;
        }
    }
}
