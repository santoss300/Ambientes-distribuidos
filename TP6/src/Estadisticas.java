/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.io.Serializable;

public class Estadisticas implements Serializable {

    private static final long serialVersionUID = 1L;

    private final double promedio;
    private final double maximo;
    private final double minimo;
    private final double desviacion;

    public Estadisticas(double promedio, double maximo, double minimo, double desviacion) {
        this.promedio = promedio;
        this.maximo = maximo;
        this.minimo = minimo;
        this.desviacion = desviacion;
    }

    @Override
    public String toString() {
        return String.format("promedio = %.2f | maximo = %.2f | minimo = %.2f | desviacion estandar = %.2f",
                promedio, maximo, minimo, desviacion);
    }
}
