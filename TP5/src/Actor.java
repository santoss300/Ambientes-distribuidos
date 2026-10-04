/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class Actor {

    private final String nombre;
    private final BlockingQueue<Object> buzon = new LinkedBlockingQueue<>();
    private boolean activo = true;

    public Actor(String nombre) {
        this.nombre = nombre;
    }

    public void iniciar() {
        new Thread(this::atenderBuzon, nombre).start();
    }

    public void enviar(Object mensaje) {
        buzon.add(mensaje);
    }

    public String getNombre() {
        return nombre;
    }

    protected void detener() {
        activo = false;
    }

    protected abstract void recibir(Object mensaje);

    private void atenderBuzon() {
        while (activo) {
            try {
                Object mensaje = buzon.take();
                recibir(mensaje);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
