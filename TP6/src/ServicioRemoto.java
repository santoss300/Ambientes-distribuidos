/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServicioRemoto extends Remote {

    String conectar(String nombreCliente) throws RemoteException;

    Estadisticas calcularEstadisticas(double[] numeros) throws RemoteException;

    boolean validarCuit(String cuit) throws RemoteException;

    String[] filtrarTextos(String[] textos, String patron) throws RemoteException;
}
