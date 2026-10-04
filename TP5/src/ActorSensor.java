/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.util.Random;

public class ActorSensor extends Actor {

    private final Actor procesador;
    private final int cantidad;
    private final Random random = new Random();

    public ActorSensor(String nombre, Actor procesador, int cantidad) {
        super(nombre);
        this.procesador = procesador;
        this.cantidad = cantidad;
    }

    @Override
    protected void recibir(Object mensaje) {
        if (mensaje.equals("EMPEZAR")) {
            double suma = 0;
            for (int i = 1; i <= cantidad; i++) {
                double temperatura = 15 + random.nextInt(200) / 10.0;
                procesador.enviar(new Lectura(getNombre(), i, temperatura));
                suma += temperatura;
                esperarUnPoco();
            }
            procesador.enviar(new FinSensor(getNombre(), cantidad, suma));
            detener();
        }
    }

    private void esperarUnPoco() {
        try {
            Thread.sleep(random.nextInt(4));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
