/*
 * Ignacio Ruiz - DNI 39.040.338
 */

public class Transaccion {

    int idTransaccion;
    String origen;
    double monto;
    long timestamp;

    public Transaccion(int idTransaccion, String origen, double monto, long timestamp) {
        this.idTransaccion = idTransaccion;
        this.origen = origen;
        this.monto = monto;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "id=" + idTransaccion + ", origen=" + origen
                + ", monto=" + monto + ", timestamp=" + timestamp;
    }
}
