# TP2 — Resiliencia: Reintentos con Backoff Exponencial y Jitter

Trabajo Práctico N° 2 — Desarrollo de Aplicaciones para Ambientes Distribuidos.

Continuación de la calculadora distribuida del [TP1](../README.md). Acá el foco
ya no es la comunicación en sí, sino **qué hace el cliente cuando el servidor
falla**: cuántas veces reintenta, cuánto espera entre reintentos y cómo mide su
propio comportamiento.

---

## Contenido

```
TP2/
├── src/
│   ├── ClienteResiliente.java   Cliente con backoff exponencial + jitter y métricas
│   └── ServidorInestable.java   Servidor que simula fallos transitorios (503)
├── capturas/                    Salidas reales de las ejecuciones (PNG + TXT)
└── README.md                    Este archivo (incluye el Ejercicio 3)
```

---

## Compilación y ejecución

```bash
# desde la raíz del repositorio
javac -d TP2/bin TP2/src/ServidorInestable.java TP2/src/ClienteResiliente.java
```

Se necesitan **dos terminales**.

### Terminal A — servidor inestable

```bash
java -cp TP2/bin ServidorInestable [cantidadDeFallosASimular]
```

El argumento indica cuántas de las primeras peticiones se rechazan con un fallo
transitorio simulado. Por defecto, `3`.

```bash
java -cp TP2/bin ServidorInestable 3   # rechaza las 3 primeras, después responde bien
java -cp TP2/bin ServidorInestable 0   # servidor sano, sin fallos simulados
```

### Terminal B — cliente resiliente

```bash
java -cp TP2/bin ClienteResiliente [host] [puerto] [peticion]

# ejemplos
java -cp TP2/bin ClienteResiliente
java -cp TP2/bin ClienteResiliente 127.0.0.1 5500 "15;+;30"
java -cp TP2/bin ClienteResiliente 127.0.0.1 5500 "10;/;0"
```

---

## Ejercicio 1 — Incorporación de Jitter

La espera entre reintentos deja de ser un intervalo fijo y pasa a ser:

```
Espera = (Base × 2^(intento-1)) + Random(0, 500) ms
```

con `Base = 1000 ms`. Implementación (`ClienteResiliente.java`):

```java
private static long calcularEspera(int intento) {
    long backoffExponencial = BASE_MS * (long) Math.pow(2, intento - 1);
    int jitter = RANDOM.nextInt(JITTER_MAX_MS + 1);
    return backoffExponencial + jitter;
}
```

Progresión resultante (el componente aleatorio cambia en cada ejecución):

| Intento | Backoff determinista | Jitter | Espera total |
|---|---|---|---|
| 1 → 2 | 1000 ms | 0 – 500 ms | 1000 – 1500 ms |
| 2 → 3 | 2000 ms | 0 – 500 ms | 2000 – 2500 ms |
| 3 → 4 | 4000 ms | 0 – 500 ms | 4000 – 4500 ms |
| 4 → 5 | 8000 ms | 0 – 500 ms | 8000 – 8500 ms |

El cliente imprime en cada paso las dos componentes por separado, para que se vea
que el jitter efectivamente varía:

```
   backoff = 1000 ms  |  jitter = 60 ms
.. Esperando 1060 ms antes de reintentar.
```

### Reintentos solo ante fallos transitorios

El cliente clasifica la respuesta antes de decidir si reintenta:

| Respuesta recibida | Clasificación | ¿Reintenta? |
|---|---|---|
| `IOException` / `ConnectException` / timeout | transitoria | sí |
| conexión cerrada sin respuesta (`null`) | transitoria | sí |
| `ERROR: 503 ...` | transitoria | sí |
| `ERROR: 400 ...` (división por cero, formato, operador) | permanente | no |
| un resultado numérico | éxito | — |

Reintentar un `400` sería inútil: la petición está mal formada y va a fallar
igual las cinco veces. Por eso el cliente corta de inmediato.

---

## Ejercicio 2 — Métricas de resiliencia

Al terminar, el cliente imprime un bloque de métricas:

```
=================================================
  METRICAS DE RESILIENCIA
=================================================
Estado final de la peticion : EXITO
Detalle                     : 45
Cantidad de intentos        : 4
Tiempo total acumulado      : 8180 ms
  - esperando (backoff)     : 7587 ms
  - comunicacion efectiva   : 593 ms
=================================================
```

- **Estado final:** `EXITO` o `FALLO DEFINITIVO`.
- **Cantidad de intentos:** cuántas veces se abrió realmente un socket.
- **Tiempo total acumulado:** medido con `System.currentTimeMillis()` desde
  antes del primer intento hasta después del último.

El desglose entre *espera* y *comunicación efectiva* no lo pedía la consigna,
pero deja a la vista el costo real de la política: de los 8180 ms del ejemplo,
**7587 ms fueron espera deliberada** y solo 593 ms trabajo de red. Ese es el
precio de la resiliencia, y es lo que obliga a fijar un `MAX_INTENTOS`.

---

## Capturas de la ejecución

### 1. Cliente recuperándose de un error simulado

Los tres primeros intentos reciben `ERROR: 503`; el cuarto tiene éxito y devuelve
`45`. Se ve el backoff creciendo (1000 → 2000 → 4000 ms) y el jitter distinto en
cada paso.

![Cliente recuperándose](capturas/01-cliente-recuperacion.png)

### 2. Log del servidor inestable durante esa misma ejecución

Cuatro conexiones, cada una desde un puerto efímero distinto. Las tres primeras
se rechazan por sobrecarga simulada, la cuarta se procesa normalmente.

![Servidor inestable](capturas/02-servidor-inestable.png)

### 3. Fallo definitivo — servidor apagado

Sin servidor escuchando, los cinco intentos terminan en `ConnectException` y el
cliente reporta `FALLO DEFINITIVO` tras 16 329 ms.

![Fallo definitivo](capturas/03-fallo-definitivo.png)

### 4. Fallo permanente — sin reintentos

`10;/;0` devuelve `ERROR: 400 Division por cero`. El cliente lo reconoce como
permanente y corta en el primer intento: 1 intento, 174 ms, cero esperas.

![Fallo permanente](capturas/04-fallo-permanente.png)

> Las salidas de consola completas de las cuatro ejecuciones también están
> guardadas como archivos `.txt` en la carpeta `capturas/`.

---

## Ejercicio 3 — Análisis teórico

### Pregunta 1 — ¿Qué problema genera que todos los clientes reintenten al mismo tiempo y con intervalos fijos?

Se produce el **Thundering Herd Problem** (efecto estampida o "manada
atronadora"): un pico de carga sincronizado que impide que el servidor se
recupere.

**Por qué se sincronizan.** Cuando un servidor se satura o se reinicia, falla
para *todos* los clientes prácticamente en el mismo instante. Si todos usan la
misma política de espera fija —por ejemplo, "reintentar a los 2 segundos"—,
todos van a reintentar exactamente 2 segundos después de ese instante común. El
fallo actúa como una señal de largada que alinea a la flota entera.

**Por qué se agrava con cada ronda.** El servidor recibe una ráfaga de N
peticiones simultáneas, vuelve a saturarse, vuelve a fallar para todos, y todos
vuelven a esperar el mismo intervalo. La estampida se repite en oleadas cada vez
más nutridas, porque a los clientes que reintentan se suman los nuevos. El
sistema queda atrapado en un ciclo de fallo del que **no puede salir solo**,
incluso cuando la causa original del problema ya desapareció: es una *falla
metaestable*, sostenida ya no por la causa inicial sino por el propio tráfico de
reintentos.

**Por qué el backoff exponencial solo no alcanza.** Duplicar la espera reduce la
frecuencia de los reintentos, pero **no rompe la sincronización**: si todos los
clientes fallaron juntos, todos calculan 1000 ms, después 2000 ms, después 4000
ms… y siguen llegando juntos, solo que más espaciados en el tiempo. Las ráfagas
son menos frecuentes, pero igual de altas y puntiagudas.

**Qué aporta el jitter.** Sumar `Random(0, 500)` ms hace que dos clientes que
fallaron en el mismo milisegundo esperen tiempos distintos. Con eso, la ráfaga
se *dispersa*: en lugar de N peticiones concentradas en un instante, llegan N
peticiones repartidas en una ventana de 500 ms. El pico instantáneo baja, el
servidor puede ir drenando la cola de a poco y recuperarse de verdad.

```
Sin jitter (intervalos fijos)          Con jitter
      |                                     |
carga |    #       #       #           carga|  ####   ####   ####
      |    #       #       #                |  ####   ####   ####
      +----+-------+-------+---- t          +--+------+------+---- t
   picos que vuelven a tumbar           carga repartida: el servidor
   al servidor en cada ronda            alcanza a atender y se recupera
```

**Efectos concretos del problema:**

- Se consumen todas las conexiones y los hilos disponibles del servidor.
- La latencia se dispara para los clientes legítimos que recién llegan.
- Se comporta como un **DDoS autoinfligido**: el propio sistema se ataca.
- Si hay autoescalado, las instancias nuevas se saturan apenas arrancan.
- En arquitecturas de microservicios el efecto se propaga aguas arriba y puede
  terminar en una **falla en cascada**.

**Mitigaciones habituales, además del jitter:** límite máximo de reintentos
(`MAX_INTENTOS`, implementado acá), tope superior de espera (*capped backoff*),
*circuit breaker* para dejar de golpear un servicio caído, presupuesto de
reintentos por cliente, y *deadline* global de la operación.

### Pregunta 2 — Diferencia entre fallo transitorio y fallo permanente

| | **Fallo transitorio** | **Fallo permanente** |
|---|---|---|
| Causa | condición temporal del entorno | condición estable del sistema o de la petición |
| Duración | se resuelve solo, en segundos o minutos | persiste hasta que alguien interviene |
| ¿Reintentar sirve? | **sí**, es la estrategia correcta | **no**, solo desperdicia recursos |
| Analogía | la línea está ocupada | el número no existe |

**Fallo transitorio.** Es un fallo que **desaparece por sí mismo** si se espera
lo suficiente: la petición era válida y volvería a serlo. La causa es el estado
momentáneo del entorno, no la petición.

*Ejemplo en una arquitectura distribuida:* un servicio de pagos devuelve
`503 Service Unavailable` porque su pool de conexiones a la base de datos está
agotado por un pico de tráfico. En 2 segundos se liberan conexiones y la misma
petición, sin cambiarle una coma, se procesa correctamente. Otros casos típicos:
timeouts de red, pérdida de paquetes, `429 Too Many Requests` por rate limiting,
la reelección de líder de un cluster, un contenedor reiniciándose durante un
deploy.

*En este TP:* es lo que simula `ServidorInestable` con el `ERROR: 503`, y lo que
el cliente reintenta hasta recuperarse (captura 1).

**Fallo permanente.** El fallo se va a repetir idéntico las veces que se
reintente, porque la causa está en la petición o en una condición estable del
sistema. Reintentar no solo es inútil: **empeora las cosas**, porque suma carga
sin ninguna posibilidad de éxito.

*Ejemplo en una arquitectura distribuida:* el cliente pide
`GET /api/usuarios/9999` y el servicio responde `404 Not Found` porque ese
usuario no existe. Reintentarlo 5 veces con backoff da 5 veces `404`. Otros
casos: `400 Bad Request` por un JSON mal formado, `401`/`403` por credenciales
inválidas, una violación de restricción de integridad en la base, un host que
no resuelve por DNS.

*En este TP:* es el `ERROR: 400 Division por cero`. El cliente lo detecta, corta
en el primer intento y lo reporta como `FALLO DEFINITIVO` (captura 4).

**Por qué importa la distinción.** Es la decisión de diseño central de cualquier
política de reintentos:

- Tratar un **transitorio como permanente** → se pierden operaciones que habrían
  funcionado con solo esperar un segundo. El sistema se vuelve frágil.
- Tratar un **permanente como transitorio** → se malgastan recursos, se retrasa
  el error que el usuario necesita ver, y se contribuye a la estampida de la
  pregunta 1.

De ahí que el protocolo del TP distinga los códigos `503` (reintentable) y `400`
(no reintentable) en la propia respuesta: la clasificación no se adivina desde el
cliente, la comunica el servidor.

---

## Detalles de implementación

- **`Socket.connect(addr, TIMEOUT_MS)` con timeout explícito** en vez del
  constructor `new Socket(host, puerto)`. Sin timeout, un host inalcanzable deja
  al cliente colgado el tiempo que decida el sistema operativo, y la política de
  reintentos pierde todo sentido.
- **`setSoTimeout(5000)`** para que un servidor que acepta la conexión pero nunca
  responde también se trate como fallo transitorio y no como un bloqueo eterno.
- **Clase interna `Respuesta`** con tres estados (éxito / fallo transitorio /
  fallo permanente), para que la decisión de reintentar quede en un solo lugar.
- **El servidor sigue siendo secuencial**, igual que en el TP1: la resiliencia se
  resuelve del lado del cliente.

---

## Autor

Trabajo Práctico N° 2 — Desarrollo de Aplicaciones para Ambientes Distribuidos
