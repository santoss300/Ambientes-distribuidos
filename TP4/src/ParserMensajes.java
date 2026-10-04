/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ParserMensajes {

    public static String aJson(Transaccion t) {
        return "{\"id\":" + t.idTransaccion
                + ",\"origen\":\"" + t.origen + "\""
                + ",\"monto\":" + t.monto
                + ",\"timestamp\":" + t.timestamp + "}";
    }

    public static Transaccion desdeJson(String json) {
        String limpio = json.replace("{", "").replace("}", "").replace("\"", "");
        String[] campos = limpio.split(",");

        int id = Integer.parseInt(campos[0].split(":")[1]);
        String origen = campos[1].split(":")[1];
        double monto = Double.parseDouble(campos[2].split(":")[1]);
        long timestamp = Long.parseLong(campos[3].split(":")[1]);

        return new Transaccion(id, origen, monto, timestamp);
    }

    public static byte[] aBinario(Transaccion t) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream salida = new DataOutputStream(bytes);
        salida.writeInt(t.idTransaccion);
        salida.writeUTF(t.origen);
        salida.writeDouble(t.monto);
        salida.writeLong(t.timestamp);
        return bytes.toByteArray();
    }

    public static Transaccion desdeBinario(byte[] datos) throws IOException {
        DataInputStream entrada = new DataInputStream(new ByteArrayInputStream(datos));
        int id = entrada.readInt();
        String origen = entrada.readUTF();
        double monto = entrada.readDouble();
        long timestamp = entrada.readLong();
        return new Transaccion(id, origen, monto, timestamp);
    }
}
