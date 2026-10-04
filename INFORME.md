# Informe — Trabajo Práctico N° 1

**Materia:** Desarrollo de Aplicaciones para Ambientes Distribuidos
**Tema:** Cliente-Servidor con Sockets TCP

---

## Ejercicio 1 — Calculadora distribuida

### Cómo funciona

```
   CLIENTE                                   SERVIDOR (puerto 5500)
   1. pide los números por consola
   2. arma el texto "15;+;30"
   3. se conecta          ───────────────►   accept()   (estaba esperando)
   4. manda "15;+;30"     ───────────────►   readLine() (lee el pedido)
                                             hace la cuenta
   5. recibe "45"         ◄───────────────   manda "45"
   6. lo muestra y cierra                    cierra y vuelve a esperar
```

### El "idioma" que usan (protocolo)

Se mandan una línea de texto cada uno:

| Quién | Formato | Ejemplo |
|---|---|---|
| Cliente → Servidor | `numero1;operador;numero2` | `15;+;30` |
| Servidor → Cliente (bien) | el resultado | `45` |
| Servidor → Cliente (error) | `ERROR: <qué pasó>` | `ERROR: Division por cero` |

El salto de línea al final es importante: así el que lee sabe dónde termina el
mensaje.

### Decisiones

- **El servidor atiende de a un cliente por vez**, sin hilos (lo permite la
  consigna).
- **Una conexión por cuenta**: cada operación abre y cierra su propia conexión.
- **Los errores se mandan como texto**: el servidor nunca "explota", siempre
  contesta algo. Así el cliente nunca se queda esperando para siempre.

### División por cero

El servidor revisa **antes** de dividir:

```java
case "/":
    if (b == 0) {
        return "ERROR: Division por cero";
    }
    return formatear((double) a / b);
```

Si no se revisara, Java tiraría un error, el servidor cortaría la conexión sin
contestar y el cliente no sabría qué pasó.

### Casos probados

| Pedido | Respuesta | Resultado |
|---|---|---|
| `15;+;30` | `45` | OK |
| `100;-;42` | `58` | OK |
| `7;*;8` | `56` | OK |
| `45;/;6` | `7.5` | OK |
| `10;/;0` | `ERROR: Division por cero` | Error controlado |

---

## Ejercicio 2 — Preguntas

### Pregunta 1 — ¿Qué le pasa al cliente si el servidor no está prendido?

Falla **al instante**, en el momento de conectarse:

```java
Socket socket = new Socket(host, puerto);   // Cliente.java, línea 61
```

Como nadie está escuchando en el puerto 5500, la otra máquina rechaza la
conexión y Java lanza **`java.net.ConnectException: Connection refused`**.

El cliente atrapa ese error y muestra un mensaje claro en vez de un error feo
(ver `capturas/05-servidor-no-disponible.png`):

```
  !! NO SE PUDO CONECTAR CON EL SERVIDOR.
     Excepcion: java.net.ConnectException
     Mensaje  : Connection refused: connect
     Verifique que el servidor este ejecutandose en 127.0.0.1:5500
```

Un detalle: si la **máquina** del servidor directamente no existe o está
apagada, no hay rechazo inmediato. El cliente se queda esperando unos segundos
y después falla por tiempo (timeout).

### Pregunta 2 — ¿Qué línea frena el programa hasta que pasa algo en la red?

Hay tres líneas que se quedan esperando:

| Archivo | Línea | Instrucción | Sigue cuando... |
|---|---|---|---|
| `Servidor.java` | 33 | `servidor.accept()` | se conecta un cliente |
| `Servidor.java` | 45 | `entrada.readLine()` | el cliente manda su pedido |
| `Cliente.java` | 68 | `entrada.readLine()` | llega la respuesta del servidor |

La más importante es **`accept()`**: el servidor se queda ahí parado, sin
límite de tiempo, hasta que alguien se conecta.

Mientras espera, el programa **no gasta procesador**: queda "dormido" y el
sistema operativo lo despierta cuando llega algo por la red.

Como el servidor atiende de a uno, si está esperando al cliente A, el cliente B
tiene que hacer cola. Eso se resuelve con hilos (se hace en el TP3).

### Pregunta 3 — ¿Qué hay que cambiar para usarlo entre dos notebooks en el Wi-Fi del aula?

**El código no hace falta cambiarlo**, porque al cliente ya se le puede pasar
la IP del servidor. Los pasos son:

1. **Ver la IP de la notebook servidor** con `ipconfig` (Windows). Ejemplo:
   `192.168.0.142`.
2. **Correr el cliente apuntando a esa IP** (no a `127.0.0.1`, que es "esta
   misma compu"):
   ```bash
   java -cp bin Cliente 192.168.0.142 5500
   ```
3. **Abrir el puerto 5500 en el firewall** de la notebook servidor. Es el
   problema más común. En PowerShell como administrador:
   ```powershell
   New-NetFirewallRule -DisplayName "TP1 Calculadora" -Direction Inbound -Protocol TCP -LocalPort 5500 -Action Allow
   ```
   O aceptar el cartel de Windows "Permitir acceso" la primera vez que se
   corre el servidor.
4. **Estar en la misma red.** Algunas redes Wi-Fi (como las de facultades)
   bloquean que los dispositivos se hablen entre sí. Si pasa eso, se puede usar
   el hotspot de un celular.
5. **Probar la conexión** antes de culpar al código:
   ```powershell
   ping 192.168.0.142
   Test-NetConnection 192.168.0.142 -Port 5500
   ```

El servidor no hay que tocarlo: `new ServerSocket(5500)` ya acepta conexiones
desde cualquier red de la máquina, incluida la Wi-Fi.

| ¿Qué cambia? | |
|---|---|
| Código del servidor | Nada |
| Código del cliente | Nada |
| Cómo se ejecuta el cliente | Pasarle la IP del servidor |
| Firewall del servidor | Abrir el puerto 5500 |

---

## Conclusión

Llamar a algo por la red no es como llamar a una función común:

1. **Hay que acordar un formato** para mandar los datos (`numero1;operador;numero2`).
2. **Hay que esperar** a que el otro conteste (`accept()`, `readLine()`).
3. **Pueden pasar errores nuevos**: que el servidor no esté o que se corte la
   conexión. Por eso hay que prever esos casos desde el principio.
