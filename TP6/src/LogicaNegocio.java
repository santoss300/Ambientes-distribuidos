/*
 * Ignacio Ruiz - DNI 39.040.338
 */

import java.util.ArrayList;
import java.util.List;

public class LogicaNegocio {

    public static Estadisticas calcularEstadisticas(double[] numeros) {
        if (numeros == null || numeros.length == 0) {
            throw new IllegalArgumentException("La lista de numeros esta vacia");
        }

        double suma = 0;
        double maximo = numeros[0];
        double minimo = numeros[0];
        for (double n : numeros) {
            suma += n;
            maximo = Math.max(maximo, n);
            minimo = Math.min(minimo, n);
        }
        double promedio = suma / numeros.length;

        double sumaCuadrados = 0;
        for (double n : numeros) {
            sumaCuadrados += (n - promedio) * (n - promedio);
        }
        double desviacion = Math.sqrt(sumaCuadrados / numeros.length);

        return new Estadisticas(promedio, maximo, minimo, desviacion);
    }

    public static boolean validarCuit(String cuit) {
        String digitos = cuit.replace("-", "").trim();
        if (!digitos.matches("\\d{11}")) {
            return false;
        }

        int[] pesos = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += (digitos.charAt(i) - '0') * pesos[i];
        }

        int verificador = 11 - (suma % 11);
        if (verificador == 11) {
            verificador = 0;
        }
        if (verificador == 10) {
            return false;
        }
        return verificador == digitos.charAt(10) - '0';
    }

    public static String[] filtrarTextos(String[] textos, String patron) {
        List<String> resultado = new ArrayList<>();
        for (String texto : textos) {
            if (texto.toLowerCase().contains(patron.toLowerCase())) {
                resultado.add(texto);
            }
        }
        return resultado.toArray(new String[0]);
    }
}
