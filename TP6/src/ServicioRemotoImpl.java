/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class ServicioRemotoImpl extends UnicastRemoteObject implements ServicioRemoto {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ServicioRemotoImpl() throws RemoteException {
        super();
    }

    @Override
    public String conectar(String nombreCliente) throws RemoteException {
        log("Se conecto el cliente \"" + nombreCliente + "\"");
        return "Hola " + nombreCliente + ", estas conectado al servidor de procesamiento.";
    }

    @Override
    public Estadisticas calcularEstadisticas(double[] numeros) throws RemoteException {
        log("calcularEstadisticas(" + Arrays.toString(numeros) + ")");
        try {
            return LogicaNegocio.calcularEstadisticas(numeros);
        } catch (IllegalArgumentException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public boolean validarCuit(String cuit) throws RemoteException {
        boolean valido = LogicaNegocio.validarCuit(cuit);
        log("validarCuit(\"" + cuit + "\") -> " + (valido ? "valido" : "invalido"));
        return valido;
    }

    @Override
    public String[] filtrarTextos(String[] textos, String patron) throws RemoteException {
        String[] resultado = LogicaNegocio.filtrarTextos(textos, patron);
        log("filtrarTextos(" + textos.length + " textos, patron \"" + patron + "\") -> "
                + resultado.length + " coincidencias");
        return resultado;
    }

    private void log(String texto) {
        String cliente;
        try {
            cliente = RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            cliente = "local";
        }
        System.out.println("[" + LocalTime.now().format(HORA) + "] [" + cliente + "] " + texto);
    }
}
