# TP4 — Mandar datos por la red: JSON vs Binario

Trabajo Práctico N° 4 — Desarrollo de Aplicaciones para Ambientes Distribuidos.

Para mandar un objeto por la red hay que **convertirlo en bytes** (serializar o
*marshalling*) y, del otro lado, **volver a armarlo** (deserializar o
*unmarshalling*). En este TP se manda una `Transaccion` de dos formas y se
compara **cuánto ocupa** y **cuánto tarda** cada una:

- **JSON:** texto que se puede leer, como `{"id":101,"origen":"NodoA",...}`
- **Binario:** los datos "crudos", byte por byte, con `DataOutputStream`

---

## Archivos

```
TP4/
├── src/
│   ├── Transaccion.java             La clase con los 4 campos
│   ├── ParserMensajes.java          Convierte a JSON y a binario (y al revés)
│   ├── ServidorTransacciones.java   Recibe y vuelve a armar las transacciones (puerto 7000)
│   └── ClienteTransacciones.java    Manda 1000 transacciones en JSON y después en binario
├── capturas/                        Capturas y salidas reales de las pruebas
└── README.md                        Este archivo
```

---

## Cómo compilar y ejecutar

Desde la carpeta raíz del repositorio:

```bash
javac -d TP4/bin TP4/src/*.java
```

**Terminal A — servidor:**

```bash
java -cp TP4/bin ServidorTransacciones
```

**Terminal B — cliente:**

```bash
java -cp TP4/bin ClienteTransacciones [host] [puerto] [cantidad]

java -cp TP4/bin ClienteTransacciones                     # 1000 transacciones a 127.0.0.1:7000
java -cp TP4/bin ClienteTransacciones 192.168.0.142 7000  # a otra compu de la red
```

El cliente manda las 1000 en JSON, después **las mismas 1000** en binario, y
al final muestra la comparación. El servidor muestra lo que recibió de cada una.

---

## Cómo funciona

### 1. La clase `Transaccion`

| Campo | Tipo en Java | Tamaño en binario |
|---|---|---|
| `idTransaccion` | `int` (32 bits) | 4 bytes |
| `origen` | `String` | 2 bytes (largo) + 1 byte por letra |
| `monto` | `double` | 8 bytes |
| `timestamp` | `long` (64 bits) | 8 bytes |

### 2. `ParserMensajes`: los dos formatos

**JSON** — se arma el texto a mano, sin librerías externas:

```java
return "{\"id\":" + t.getIdTransaccion()
        + ",\"origen\":\"" + escapar(t.getOrigen()) + "\""
        + ",\"monto\":" + t.getMonto()
        + ",\"timestamp\":" + t.getTimestamp()
        + "}";
```

Para leerlo, `desdeJson()` recorre el texto y saca cada campo. Acepta los
campos en cualquier orden y con espacios, y también funciona si el `origen`
tiene comillas o acentos.

**Binario** — se escribe cada dato con su tipo:

```java
salida.writeInt(t.getIdTransaccion());
salida.writeUTF(t.getOrigen());
salida.writeDouble(t.getMonto());
salida.writeLong(t.getTimestamp());
```

Para leerlo, `desdeBinario()` usa `DataInputStream` y lee **en el mismo orden**
(`readInt`, `readUTF`, `readDouble`, `readLong`). En binario no hay nombres de
campos: los dos lados tienen que saber el orden de antemano.

### Ejemplo con la misma transacción

```
JSON    (69 bytes): {"id":101,"origen":"NodoA","monto":5466.53,"timestamp":1700000000084}
Binario (27 bytes): 00 00 00 65 00 05 4E 6F 64 6F 41 40 B5 5A 87 AE 14 7A E1 00 00 01 8B CF E5 68 54
```

Los 27 bytes del binario se leen así:

| Bytes | Qué es |
|---|---|
| `00 00 00 65` | id = 101 |
| `00 05` | el texto que sigue mide 5 letras |
| `4E 6F 64 6F 41` | "NodoA" |
| `40 B5 5A 87 AE 14 7A E1` | monto = 5466.53 |
| `00 00 01 8B CF E5 68 54` | timestamp = 1700000000084 |

En JSON, el número `1700000000084` ocupa 13 bytes (uno por cada dígito) más el
nombre `"timestamp":`. En binario, cualquier `long` ocupa siempre 8 bytes.

### 3. Cómo viajan por la red

Por cada conexión, el cliente manda:

```
[1 byte: formato 'J' o 'B'] [4 bytes: cantidad]
[4 bytes: largo] [mensaje 1]
[4 bytes: largo] [mensaje 2]
...
```

El "largo" adelante de cada mensaje le dice al servidor cuántos bytes leer, así
sabe dónde termina uno y empieza el otro. Se usa igual en los dos formatos, así
la comparación es justa. Al terminar, el servidor contesta cuántas recibió y
cuántos bytes le llegaron, para confirmar que no se perdió nada.

---

## Resultados

### Tamaño (siempre da lo mismo)

| | JSON | Binario |
|---|---|---|
| Carga útil (1000 transacciones) | **72 927 bytes** | **30 021 bytes** |
| Promedio por transacción | 72 bytes | 30 bytes |
| Con los 4 bytes de largo de cada mensaje | 76 932 bytes | 34 026 bytes |

**El binario ocupa un 58,8 % menos.** JSON ocupa 2,43 veces más.

### Tiempo (cambia un poco en cada corrida)

Tres corridas seguidas, 1000 transacciones cada una, en la misma compu
(`127.0.0.1`):

| Corrida | Serialización JSON | Serialización Binario | Serialización + envío JSON | Serialización + envío Binario | Unmarshalling JSON (servidor) | Unmarshalling Binario (servidor) |
|---|---|---|---|---|---|---|
| 1 | 3,35 ms | 0,53 ms | 35,61 ms | 2,20 ms | 11,97 ms | 0,54 ms |
| 2 | 3,22 ms | 1,04 ms | 41,41 ms | 6,88 ms | 20,94 ms | 0,60 ms |
| 3 | 2,03 ms | 0,46 ms | 24,58 ms | 3,65 ms | 3,85 ms | 0,55 ms |

**Serializar en JSON tardó entre 3 y 6 veces más**, y **leerlo en el servidor
entre 7 y 35 veces más**.

### Cómo se midió

- **Tamaño:** se suma el largo de cada mensaje ya convertido a bytes.
- **Tiempos:** con `System.nanoTime()`, que es más preciso que `currentTimeMillis()`
  para cosas que duran milisegundos.
- **Serialización:** solo el tiempo de convertir el objeto a bytes.
- **Serialización + envío:** desde que empieza hasta que se terminó de mandar
  el último byte.
- **Calentamiento:** antes de medir, cliente y servidor convierten miles de
  transacciones sin mandarlas. Java se va acelerando a medida que el código se
  repite, y sin esto el primer formato medido saldría perjudicado.

### ¿Por qué el envío de JSON tarda tanto más?

No es solo que pese 2,4 veces más. El servidor tarda más en leer JSON (tiene
que ir letra por letra buscando comillas, comas y dos puntos), entonces la
"bandeja de entrada" de la conexión se llena y **el cliente tiene que esperar**
a que el servidor vaya liberando lugar. En binario el servidor lee directo
4 + 8 + 8 bytes y listo, así que nunca hace esperar al cliente.

Además JSON va primero, y aunque hay calentamiento, la primera conexión
siempre paga un poco más. Por eso los tiempos varían entre corridas. **El
tamaño, en cambio, es exacto y siempre da igual.**

---

## Capturas

### 1. Cliente

Arriba, la misma transacción en los dos formatos (69 bytes vs 27 bytes).
Después las dos ráfagas de 1000, con el `[OK]` del servidor confirmando que
llegó todo, y al final la comparación.

![Cliente](capturas/01-cliente.png)

### 2. Servidor

Recibe las dos ráfagas, vuelve a armar las 1000 transacciones de cada una y
compara. La "suma de montos" da igual en los dos formatos y la primera
transacción es idéntica: **los datos llegaron iguales**.

![Servidor](capturas/02-servidor.png)

> Las salidas completas de las 3 corridas están como `.txt` en `capturas/`.

---

## Conclusión: ¿cuál conviene?

| | JSON | Binario |
|---|---|---|
| Tamaño | Más grande | **58 % más chico** |
| Velocidad | Más lento | **Mucho más rápido** |
| ¿Se puede leer a simple vista? | **Sí** | No, son bytes |
| ¿Hay que saber el orden de los campos? | No, cada campo tiene nombre | **Sí**, si cambia el orden se rompe |
| ¿Lo entiende cualquier lenguaje? | **Sí**, es un estándar | Solo si el otro lado sabe el formato exacto |
| Se usa en | APIs web, archivos de configuración | Juegos, bases de datos, sistemas con muchísimos mensajes |

- **JSON** conviene cuando importa que sea fácil de leer, depurar y conectar con
  otros sistemas. Por eso lo usan casi todas las APIs web.
- **Binario** conviene cuando hay muchísimos mensajes y cada byte o
  milisegundo cuenta. Por eso existen formatos como Protocol Buffers (Google),
  que son binarios pero con un "contrato" que describe los campos.

---

## Autor

Ignacio Ruiz — DNI 39.040.338
Trabajo Práctico N° 4 — Desarrollo de Aplicaciones para Ambientes Distribuidos
