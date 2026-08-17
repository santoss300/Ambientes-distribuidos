# Informe — Trabajo Práctico N° 1

**Materia:** Desarrollo de Aplicaciones para Ambientes Distribuidos
**Tema:** Arquitectura Cliente-Servidor y Comunicación mediante Sockets TCP

---

## Ejercicio 1 — Calculadora Distribuida Simple

### Arquitectura implementada

```
   ┌──────────────────┐                              ┌──────────────────┐
   │     CLIENTE      │                              │     SERVIDOR     │
   │  (Cliente.java)  │                              │ (Servidor.java)  │
   ├──────────────────┤                              ├──────────────────┤
   │ 1. pide datos    │                              │ ServerSocket(5500)│
   │    por consola   │                              │        │          │
   │ 2. arma la       │                              │   accept()  ◄── BLOQUEA
   │    cadena        │      "15;+;30"               │        │          │
   │ 3. new Socket()  │ ───────────────────────────► │   readLine()◄── BLOQUEA
   │ 4. println()     │                              │        │          │
   │ 5. readLine() ◄── BLOQUEA                       │   procesar()      │
   │                  │           "45"               │        │          │
   │ 6. imprime       │ ◄─────────────────────────── │   println()       │
   │ 7. close()       │                              │   close()         │
   └──────────────────┘                              └──────────────────┘
                              red TCP/IP
```

### Protocolo de aplicación

Se definió un protocolo de texto plano, de una línea, terminada en salto de línea:

| Sentido | Formato | Ejemplo |
|---|---|---|
| Petición (cliente → servidor) | `numero1;operador;numero2` | `15;+;30` |
| Respuesta OK (servidor → cliente) | el resultado | `45` |
| Respuesta de error | `ERROR: <descripción>` | `ERROR: Division por cero` |

El salto de línea que agrega `println()` es lo que le permite al `readLine()` del
otro extremo saber dónde termina el mensaje. Sin ese delimitador, TCP —que es un
flujo de bytes sin fronteras de mensaje— dejaría al receptor bloqueado
indefinidamente esperando más datos.

### Decisiones de diseño

- **Servidor secuencial en bucle:** atiende un cliente completo, cierra la
  conexión y recién entonces vuelve a `accept()`. No usa hilos, tal como permite
  la consigna.
- **Una conexión por operación:** cada cálculo abre y cierra su propio socket.
  Esto deja visible el ciclo de vida completo del socket en cada iteración.
- **Manejo de errores centralizado en el servidor:** el método `procesar()` nunca
  propaga excepciones; todo problema se traduce a un mensaje `ERROR: ...`. Así el
  cliente **siempre** recibe una respuesta y nunca queda colgado en `readLine()`.

### División por cero

La consigna pide gestionarla de forma controlada. En `Servidor.java`:

```java
case "/":
    if (b == 0) {
        return "ERROR: Division por cero";
    }
    return formatear((double) a / b);
```

La validación se hace **antes** de dividir. Si se dejara que Java ejecutara `15/0`
con enteros, se lanzaría una `ArithmeticException` que abortaría el bloque de
atención, el socket se cerraría sin escribir nada, y el cliente recibiría `null`
de su `readLine()` en vez de un mensaje útil.

Evidencia de funcionamiento (captura `03-division-por-cero.png`):

```
  -> Enviando al servidor: "10;/;0"
  <- El servidor respondio: ERROR: Division por cero
```

### Casos probados

| Petición enviada | Respuesta del servidor | Resultado |
|---|---|---|
| `15;+;30` | `45` | OK |
| `100;-;42` | `58` | OK |
| `7;*;8` | `56` | OK |
| `45;/;6` | `7.5` | OK |
| `10;/;0` | `ERROR: Division por cero` | Error controlado |

---

## Ejercicio 2 — Análisis Teórico-Práctico

### Pregunta 1 — ¿Qué sucede con el cliente si el servidor no está ejecutándose al momento de intentar conectar? Muestre la excepción que lanza Java.

El cliente **no se queda esperando**: falla de inmediato, en el momento mismo de
construir el socket. La línea responsable es:

```java
Socket socket = new Socket(host, puerto);   // Cliente.java, línea 90
```

El constructor de `Socket` intenta el *handshake* TCP de tres vías (envía un
paquete `SYN` al puerto 5500). Como no hay ningún proceso escuchando en ese
puerto, el sistema operativo de la máquina destino responde con un paquete
`RST` (reset), rechazando activamente la conexión. Java traduce ese rechazo a
la excepción **`java.net.ConnectException`**, con el mensaje
`Connection refused: connect`.

Jerarquía de la excepción:

```
java.lang.Exception
  └─ java.io.IOException
       └─ java.net.SocketException
            └─ java.net.ConnectException      ← es una excepción verificada (checked)
```

Al ser una excepción verificada, el compilador **obliga** a manejarla. Salida
real capturada (ver `capturas/05-servidor-no-disponible.png`):

```
  !! NO SE PUDO CONECTAR CON EL SERVIDOR.
     Excepcion: java.net.ConnectException
     Mensaje  : Connection refused: connect
     Verifique que el servidor este ejecutandose en 127.0.0.1:5500

     --- Stack trace completo ---
java.net.ConnectException: Connection refused: connect
	at java.base/sun.nio.ch.Net.connect0(Native Method)
	at java.base/sun.nio.ch.Net.connect(Net.java:589)
	at java.base/sun.nio.ch.Net.connect(Net.java:578)
	at java.base/sun.nio.ch.NioSocketImpl.connect(NioSocketImpl.java:583)
	at java.base/java.net.SocksSocketImpl.connect(SocksSocketImpl.java:327)
	at java.base/java.net.Socket.connect(Socket.java:751)
	at java.base/java.net.Socket.connect(Socket.java:686)
	at java.base/java.net.Socket.<init>(Socket.java:555)
	at java.base/java.net.Socket.<init>(Socket.java:324)
	at Cliente.enviarPeticion(Cliente.java:90)
	at Cliente.main(Cliente.java:69)
```

En el código, el caso está capturado explícitamente para no mostrarle al usuario
un volcado crudo:

```java
} catch (ConnectException e) {
    System.out.println("  !! NO SE PUDO CONECTAR CON EL SERVIDOR.");
    ...
}
```

**Observación importante:** hay que distinguir dos escenarios distintos.

- *Servidor apagado, host alcanzable* → `ConnectException` inmediata
  (el host contesta con `RST`). Es el caso probado.
- *Host inexistente o inalcanzable* (por ejemplo, una IP apagada de la red) →
  no llega ninguna respuesta y el intento queda reintentando hasta agotar el
  temporizador del sistema operativo, terminando en
  `java.net.SocketTimeoutException` o `java.net.NoRouteToHostException` tras
  varios segundos.

### Pregunta 2 — Identifique en su código qué línea bloquea la ejecución del programa hasta que ocurre un evento de red.

Hay **tres** llamadas bloqueantes, marcadas con comentarios en el código fuente:

| # | Archivo | Línea | Instrucción | Se desbloquea cuando... |
|---|---|---|---|---|
| 1 | `Servidor.java` | 58 | `Socket conexion = servidor.accept();` | un cliente completa el handshake TCP contra el puerto 5500 |
| 2 | `Servidor.java` | 79 | `String peticion = entrada.readLine();` | llega una línea completa desde el cliente (o este cierra la conexión) |
| 3 | `Cliente.java` | 105 | `String resultado = entrada.readLine();` | llega la línea de respuesta desde el servidor |

**La más representativa es `accept()`**, porque es la que define el rol de
servidor: el proceso queda detenido indefinidamente, sin fecha de vencimiento,
esperando que aparezca alguien del otro lado.

Un punto conceptual que conviene remarcar: durante el bloqueo **el programa no
consume CPU**. No es una espera activa (un `while` girando en vacío); el sistema
operativo saca al hilo de la cola de ejecución y lo deja dormido hasta que la
tarjeta de red genera el evento correspondiente. Recién ahí el planificador lo
vuelve a poner en ejecución.

Esta es justamente la diferencia con una llamada a función local: una función
local retorna cuando termina de calcular, y ese momento depende únicamente de
este proceso. Estas tres líneas retornan cuando ocurre algo **fuera** del
proceso, sobre lo que el programa no tiene ningún control.

Consecuencia directa del diseño secuencial: mientras el servidor está bloqueado
en el `readLine()` de la línea 79 atendiendo al cliente A, un cliente B que
intente conectarse queda encolado en el *backlog* de TCP y no será atendido
hasta que A termine. Ese es exactamente el problema que resolverían los hilos.

### Pregunta 3 — Proponga qué cambios serían necesarios si dos compañeros quisieran ejecutar el Cliente en una notebook y el Servidor en otra, conectadas al Wi-Fi del aula.

**El código ya está preparado para esto**: el host es un parámetro de línea de
comandos, no una constante incrustada.

```java
String host = args.length > 0 ? args[0] : HOST_POR_DEFECTO;  // Cliente.java
```

Los cambios necesarios son los siguientes.

**1. Averiguar la IP de la notebook servidora en la red del aula.**

```bash
ipconfig          # Windows  -> buscar "Dirección IPv4" del adaptador Wi-Fi
ip addr           # Linux
ifconfig          # macOS
```

En la máquina donde se desarrolló este TP, por ejemplo, el adaptador Wi-Fi
tiene la dirección `192.168.0.142`.

**2. Cambiar el destino del cliente: dejar de usar `localhost`.**

Esto es lo esencial. `127.0.0.1` (o `localhost`) es la interfaz de *loopback*:
el tráfico nunca sale de la máquina. Hay que apuntar a la IP real del servidor
en la LAN:

```bash
# En la notebook cliente:
java -cp bin Cliente 192.168.0.142 5500
```

Sin recompilar nada, gracias a que el host es parametrizable.

**3. Verificar que el servidor escuche en todas las interfaces.**

`new ServerSocket(5500)` ya hace *bind* a `0.0.0.0`, es decir, a **todas** las
interfaces de red de la máquina, incluida la Wi-Fi. Por lo tanto no hace falta
cambiar nada. El error a evitar sería haber escrito:

```java
// INCORRECTO para uso en red: solo aceptaría conexiones de la propia máquina
new ServerSocket(5500, 50, InetAddress.getByName("127.0.0.1"));
```

**4. Abrir el puerto 5500 en el firewall de la notebook servidora.**

Este es, en la práctica, el motivo número uno por el que el TP "no funciona" en
el aula. Windows Defender bloquea por defecto las conexiones entrantes hacia la
JVM. Desde una consola con privilegios de administrador:

```powershell
New-NetFirewallRule -DisplayName "TP1 Calculadora Distribuida" `
                    -Direction Inbound -Protocol TCP -LocalPort 5500 -Action Allow
```

Alternativamente, aceptar el cuadro de diálogo "Permitir el acceso" que Windows
muestra la primera vez que se ejecuta el servidor, **marcando la casilla de
redes privadas**.

**5. Comprobar que ambas notebooks estén en la misma subred y que la red lo permita.**

- Las dos IPs deben pertenecer al mismo rango (por ejemplo, ambas `192.168.0.x`).
- Muchas redes Wi-Fi institucionales tienen activado el *aislamiento de clientes*
  (**AP isolation**), que impide el tráfico directo entre dispositivos conectados
  al mismo punto de acceso. Si ese es el caso, ninguna configuración del lado de
  Java lo resuelve: hay que pedir que se desactive, o bien recurrir a una red
  alternativa (un teléfono compartiendo datos, o un cable de red entre ambas
  máquinas).
- Prueba rápida de conectividad antes de culpar al código:

  ```bash
  ping 192.168.0.142                       # ¿se alcanza el host?
  Test-NetConnection 192.168.0.142 -Port 5500   # ¿está abierto el puerto? (PowerShell)
  ```

**6. Consideraciones adicionales que aparecen al salir de `localhost`.**

- **La latencia deja de ser despreciable.** En loopback la respuesta es
  prácticamente instantánea; sobre Wi-Fi hay milisegundos de por medio y, lo más
  importante, **la conexión ahora puede cortarse en el medio de una operación**.
  Convendría fijar un tiempo máximo de espera para no bloquear el cliente para
  siempre:

  ```java
  socket.setSoTimeout(5000);   // lanza SocketTimeoutException a los 5 segundos
  ```

- **La codificación de caracteres deja de estar garantizada.** Si las dos
  máquinas usan configuraciones regionales distintas, el `InputStreamReader`
  interpretaría los bytes con juegos de caracteres diferentes. Lo correcto es
  fijarla explícitamente en ambos extremos:

  ```java
  new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
  ```

- **Un solo cliente por vez.** Con el servidor secuencial, si varios compañeros
  se conectan simultáneamente quedan encolados. Para un aula entera, el paso
  siguiente sería atender cada conexión en su propio hilo.

**Resumen: qué cambia y qué no.**

| Componente | ¿Requiere cambios? |
|---|---|
| Código del servidor | No — ya hace *bind* a todas las interfaces |
| Código del cliente | No — el host ya es parametrizable |
| Invocación del cliente | **Sí** — pasar la IP del servidor como argumento |
| Firewall del servidor | **Sí** — habilitar el puerto 5500 entrante |
| Red del aula | Verificar subred común y ausencia de *AP isolation* |

---

## Conclusiones

El ejercicio deja en evidencia las tres diferencias de fondo entre invocar una
función local e invocar un servicio remoto:

1. **Hay que serializar.** No se pueden pasar variables; hay que traducirlas a un
   formato transmisible y acordar de antemano cómo interpretarlo. Ese acuerdo es
   el protocolo, y en este TP es la cadena `numero1;operador;numero2`.
2. **Hay que esperar.** Aparecen puntos de bloqueo (`accept()`, `readLine()`) en
   los que el programa cede el control y queda a merced de un evento externo.
3. **Puede fallar de maneras nuevas.** Una llamada local no puede "no encontrar
   al servidor" ni "perder la conexión". Por eso el manejo de errores deja de ser
   un detalle y pasa a ser parte del diseño: tanto la `ConnectException` del
   cliente como el mensaje `ERROR: Division por cero` del servidor son
   respuestas previstas, no accidentes.
