/*
 * Ignacio Ruiz - DNI 39.040.338
 */

public final class Lectura {

    private final String sensor;
    private final int numero;
    private final double temperatura;

    public Lectura(String sensor, int numero, double temperatura) {
        this.sensor = sensor;
        this.numero = numero;
        this.temperatura = temperatura;
    }

    public String getSensor() {
        return sensor;
    }

    public int getNumero() {
        return numero;
    }

    public double getTemperatura() {
        return temperatura;
    }
}
