/*
 * Ignacio Ruiz - DNI 39.040.338
 */

public class ActorProcesador extends Actor {

    private final int sensoresEsperados;

    private int contador = 0;
    private double suma = 0;
    private double promedio = 0;

    private int sensoresTerminados = 0;
    private int cantidadInformada = 0;
    private double sumaInformada = 0;

    public ActorProcesador(String nombre, int sensoresEsperados) {
        super(nombre);
        this.sensoresEsperados = sensoresEsperados;
    }

    @Override
    protected void recibir(Object mensaje) {
        if (mensaje instanceof Lectura) {
            procesarLectura((Lectura) mensaje);
        } else if (mensaje instanceof FinSensor) {
            procesarFin((FinSensor) mensaje);
        }
    }

    private void procesarLectura(Lectura lectura) {
        contador++;
        suma += lectura.getTemperatura();
        promedio = suma / contador;

        System.out.printf("Mensaje %03d | %s lectura #%03d | %4.1f C | contador = %3d | promedio = %.2f C%n",
                contador, lectura.getSensor(), lectura.getNumero(), lectura.getTemperatura(),
                contador, promedio);
    }

    private void procesarFin(FinSensor fin) {
        sensoresTerminados++;
        cantidadInformada += fin.getCantidadEnviada();
        sumaInformada += fin.getSumaEnviada();

        System.out.println("            >> " + fin.getSensor() + " termino de enviar ("
                + fin.getCantidadEnviada() + " lecturas)");

        if (sensoresTerminados == sensoresEsperados) {
            mostrarResultado();
            detener();
        }
    }

    private void mostrarResultado() {
        boolean cantidadOk = contador == cantidadInformada;
        boolean sumaOk = Math.abs(suma - sumaInformada) < 0.001;

        System.out.println();
        System.out.println("=================================================");
        System.out.println("  RESULTADO FINAL");
        System.out.println("=================================================");
        System.out.println("Lecturas enviadas por los sensores : " + cantidadInformada);
        System.out.println("Lecturas procesadas (contador)     : " + contador);
        System.out.printf("Promedio final                     : %.2f C%n", promedio);
        System.out.println("Se perdio alguna lectura?          : " + (cantidadOk && sumaOk ? "NO" : "SI"));
        System.out.println(cantidadOk && sumaOk
                ? "OK: sin condiciones de carrera"
                : "ERROR: los numeros no coinciden");
        System.out.println("=================================================");
    }
}
