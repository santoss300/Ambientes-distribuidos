/*
 * Ignacio Ruiz - DNI 39.040.338
 */

public class Main {

    private static final int CANTIDAD_SENSORES = 5;
    private static final int LECTURAS_POR_SENSOR = 100;

    public static void main(String[] args) {

        System.out.println("=================================================");
        System.out.println("  MODELO DE ACTORES - SENSORES Y PROCESADOR");
        System.out.println("=================================================");
        System.out.println();

        System.out.println("1) CREAR (spawn)");
        ActorProcesador procesador = new ActorProcesador("Procesador", CANTIDAD_SENSORES);
        procesador.iniciar();
        System.out.println("   Creado " + procesador.getNombre() + " con su buzon");

        ActorSensor[] sensores = new ActorSensor[CANTIDAD_SENSORES];
        for (int i = 0; i < CANTIDAD_SENSORES; i++) {
            sensores[i] = new ActorSensor("Sensor-" + (i + 1), procesador, LECTURAS_POR_SENSOR);
            sensores[i].iniciar();
            System.out.println("   Creado " + sensores[i].getNombre() + " con su buzon");
        }
        System.out.println();

        System.out.println("2) ENVIAR (send): " + CANTIDAD_SENSORES + " sensores x "
                + LECTURAS_POR_SENSOR + " lecturas = "
                + (CANTIDAD_SENSORES * LECTURAS_POR_SENSOR) + " mensajes al mismo tiempo");
        System.out.println();

        System.out.println("3) DESIGNAR (cambio de estado): el procesador atiende de a un mensaje");
        System.out.println("   y actualiza su contador y promedio antes de pasar al siguiente");
        System.out.println();

        for (ActorSensor sensor : sensores) {
            sensor.enviar("EMPEZAR");
        }
    }
}
