/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class ParserMensajes {

    private ParserMensajes() {
    }

    public static String aJson(Transaccion t) {
        return "{\"id\":" + t.getIdTransaccion()
                + ",\"origen\":\"" + escapar(t.getOrigen()) + "\""
                + ",\"monto\":" + t.getMonto()
                + ",\"timestamp\":" + t.getTimestamp()
                + "}";
    }

    public static byte[] aJsonBytes(Transaccion t) {
        return aJson(t).getBytes(StandardCharsets.UTF_8);
    }

    public static Transaccion desdeJson(String json) {
        Map<String, String> campos = leerObjetoJson(json);
        return new Transaccion(
                Integer.parseInt(obligatorio(campos, "id")),
                obligatorio(campos, "origen"),
                Double.parseDouble(obligatorio(campos, "monto")),
                Long.parseLong(obligatorio(campos, "timestamp")));
    }

    public static Transaccion desdeJsonBytes(byte[] datos) {
        return desdeJson(new String(datos, StandardCharsets.UTF_8));
    }

    public static byte[] aBinario(Transaccion t) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(64);
        try (DataOutputStream salida = new DataOutputStream(buffer)) {
            salida.writeInt(t.getIdTransaccion());
            salida.writeUTF(t.getOrigen());
            salida.writeDouble(t.getMonto());
            salida.writeLong(t.getTimestamp());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return buffer.toByteArray();
    }

    public static Transaccion desdeBinario(byte[] datos) {
        try (DataInputStream entrada = new DataInputStream(new ByteArrayInputStream(datos))) {
            int id = entrada.readInt();
            String origen = entrada.readUTF();
            double monto = entrada.readDouble();
            long timestamp = entrada.readLong();
            return new Transaccion(id, origen, monto, timestamp);
        } catch (IOException e) {
            throw new IllegalArgumentException("Mensaje binario invalido: " + e.getMessage(), e);
        }
    }

    private static String escapar(String texto) {
        StringBuilder sb = new StringBuilder(texto.length() + 8);
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private static String obligatorio(Map<String, String> campos, String clave) {
        String valor = campos.get(clave);
        if (valor == null) {
            throw new IllegalArgumentException("Falta el campo \"" + clave + "\" en el JSON");
        }
        return valor;
    }

    private static Map<String, String> leerObjetoJson(String json) {
        Map<String, String> campos = new HashMap<>();
        int[] pos = {saltarEspacios(json, 0)};

        esperar(json, pos, '{');
        pos[0] = saltarEspacios(json, pos[0]);

        if (pos[0] < json.length() && json.charAt(pos[0]) == '}') {
            return campos;
        }

        while (true) {
            pos[0] = saltarEspacios(json, pos[0]);
            String clave = leerCadena(json, pos);
            pos[0] = saltarEspacios(json, pos[0]);
            esperar(json, pos, ':');
            pos[0] = saltarEspacios(json, pos[0]);

            String valor;
            if (pos[0] < json.length() && json.charAt(pos[0]) == '"') {
                valor = leerCadena(json, pos);
            } else {
                int inicio = pos[0];
                while (pos[0] < json.length() && ",} \t\r\n".indexOf(json.charAt(pos[0])) < 0) {
                    pos[0]++;
                }
                valor = json.substring(inicio, pos[0]);
            }
            campos.put(clave, valor);

            pos[0] = saltarEspacios(json, pos[0]);
            if (pos[0] >= json.length()) {
                throw new IllegalArgumentException("JSON incompleto");
            }
            char c = json.charAt(pos[0]++);
            if (c == '}') {
                return campos;
            }
            if (c != ',') {
                throw new IllegalArgumentException("Se esperaba ',' o '}' en la posicion " + (pos[0] - 1));
            }
        }
    }

    private static String leerCadena(String json, int[] pos) {
        esperar(json, pos, '"');
        StringBuilder sb = new StringBuilder();
        while (pos[0] < json.length()) {
            char c = json.charAt(pos[0]++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (pos[0] >= json.length()) {
                break;
            }
            char e = json.charAt(pos[0]++);
            switch (e) {
                case 'n':
                    sb.append('\n');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'b':
                    sb.append('\b');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'u':
                    if (pos[0] + 4 > json.length()) {
                        throw new IllegalArgumentException("Escape \\u incompleto");
                    }
                    sb.append((char) Integer.parseInt(json.substring(pos[0], pos[0] + 4), 16));
                    pos[0] += 4;
                    break;
                default:
                    sb.append(e);
            }
        }
        throw new IllegalArgumentException("Cadena JSON sin cerrar");
    }

    private static void esperar(String json, int[] pos, char esperado) {
        if (pos[0] >= json.length() || json.charAt(pos[0]) != esperado) {
            throw new IllegalArgumentException("Se esperaba '" + esperado + "' en la posicion " + pos[0]);
        }
        pos[0]++;
    }

    private static int saltarEspacios(String json, int pos) {
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }
        return pos;
    }
}
