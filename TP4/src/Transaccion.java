/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.util.Objects;

public class Transaccion {

    private final int idTransaccion;
    private final String origen;
    private final double monto;
    private final long timestamp;

    public Transaccion(int idTransaccion, String origen, double monto, long timestamp) {
        this.idTransaccion = idTransaccion;
        this.origen = origen;
        this.monto = monto;
        this.timestamp = timestamp;
    }

    public int getIdTransaccion() {
        return idTransaccion;
    }

    public String getOrigen() {
        return origen;
    }

    public double getMonto() {
        return monto;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Transaccion)) {
            return false;
        }
        Transaccion otra = (Transaccion) o;
        return idTransaccion == otra.idTransaccion
                && Double.compare(monto, otra.monto) == 0
                && timestamp == otra.timestamp
                && Objects.equals(origen, otra.origen);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idTransaccion, origen, monto, timestamp);
    }

    @Override
    public String toString() {
        return "Transaccion{id=" + idTransaccion + ", origen=" + origen
                + ", monto=" + monto + ", timestamp=" + timestamp + "}";
    }
}
