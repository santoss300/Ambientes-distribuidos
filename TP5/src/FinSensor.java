/*
 * Ignacio Ruiz - DNI 39.040.338
 */

public final class FinSensor {

    private final String sensor;
    private final int cantidadEnviada;
    private final double sumaEnviada;

    public FinSensor(String sensor, int cantidadEnviada, double sumaEnviada) {
        this.sensor = sensor;
        this.cantidadEnviada = cantidadEnviada;
        this.sumaEnviada = sumaEnviada;
    }

    public String getSensor() {
        return sensor;
    }

    public int getCantidadEnviada() {
        return cantidadEnviada;
    }

    public double getSumaEnviada() {
        return sumaEnviada;
    }
}
