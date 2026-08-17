# Calculadora Distribuida — Trabajo Práctico N° 1

Aplicación Cliente-Servidor en Java que resuelve operaciones matemáticas
remotas comunicándose mediante **sockets TCP** (`java.net`).

**Materia:** Desarrollo de Aplicaciones para Ambientes Distribuidos
**Tema:** Arquitectura Cliente-Servidor y Comunicación mediante Sockets TCP

> El análisis teórico completo (Ejercicio 2) está en **[INFORME.md](INFORME.md)**.

---

## Contenido del repositorio

```
.
├── src/
│   ├── Servidor.java     Servidor TCP secuencial, escucha en el puerto 5500
│   └── Cliente.java      Cliente de consola, host y puerto parametrizables
├── capturas/             Capturas de pantalla de la ejecución
├── INFORME.md            Ejercicio 2 — análisis teórico-práctico
└── README.md             Este archivo
```

---

## Requisitos

- **JDK 8 o superior** (desarrollado y probado con **JDK 21.0.8 LTS**).

Verificación:

```bash
java -version
javac -version
```

---

## Compilación

Desde la raíz del proyecto:

```bash
javac -d bin src/Servidor.java src/Cliente.java
```

Esto genera `bin/Servidor.class` y `bin/Cliente.class`.

---

## Ejecución

Se necesitan **dos terminales**. El servidor tiene que estar levantado
**antes** que el cliente.

### 1. Terminal A — Servidor

```bash
java -cp bin Servidor
```

Salida esperada:

```
=========================================
  SERVIDOR DE CALCULADORA DISTRIBUIDA
=========================================
Escuchando en el puerto 5500...
(Ctrl+C para detener)
```

El servidor queda bloqueado en `accept()` esperando conexiones. Se detiene con
`Ctrl+C`.

### 2. Terminal B — Cliente

```bash
java -cp bin Cliente
```

El cliente pide por consola los dos números y la operación, y muestra el
resultado devuelto por el servidor.

#### Conectarse a un servidor en otra máquina

El host y el puerto se pasan como argumentos:

```bash
java -cp bin Cliente <host> [puerto]

# Ejemplos
java -cp bin Cliente                        # 127.0.0.1:5500 (por defecto)
java -cp bin Cliente 192.168.0.142          # otra notebook, puerto 5500
java -cp bin Cliente 192.168.0.142 5500     # host y puerto explícitos
```

Para ejecutarlo entre dos máquinas de la misma red Wi-Fi hay que habilitar
además el puerto 5500 en el firewall del servidor — el procedimiento completo
está en la [pregunta 3 del informe](INFORME.md#pregunta-3--proponga-qué-cambios-serían-necesarios-si-dos-compañeros-quisieran-ejecutar-el-cliente-en-una-notebook-y-el-servidor-en-otra-conectadas-al-wi-fi-del-aula).

---

## Protocolo de comunicación

Texto plano, un mensaje por línea, terminado en salto de línea.

| Sentido | Formato | Ejemplo |
|---|---|---|
| Petición (cliente → servidor) | `numero1;operador;numero2` | `15;+;30` |
| Respuesta correcta | el resultado | `45` |
| Respuesta de error | `ERROR: <descripción>` | `ERROR: Division por cero` |

Operadores admitidos: `+`, `-`, `*`, `/`

Errores controlados que devuelve el servidor:

| Situación | Respuesta |
|---|---|
| División por cero | `ERROR: Division por cero` |
| Cantidad de campos incorrecta | `ERROR: Formato invalido. Se esperaba numero1;operador;numero2` |
| Operandos no numéricos | `ERROR: Los operandos deben ser numeros enteros` |
| Operador desconocido | `ERROR: Operador no soportado. Use + - * /` |

---

## Capturas de pantalla

### 1. Servidor a la escucha en el puerto 5500

El servidor se bloquea en `accept()` esperando a que un cliente se conecte.

![Servidor escuchando](capturas/01-servidor-escuchando.png)

### 2. Cliente ejecutando una operación (15 + 30)

Se ve el ciclo completo: los datos pedidos por consola, la cadena `"15;+;30"`
empaquetada según el protocolo, y el resultado `45` devuelto por el servidor.

![Operación de suma](capturas/02-operacion-suma.png)

### 3. Varias operaciones, incluida la división por cero

Divisiones con resultado decimal (`45 / 6 = 7.5`) y el error controlado ante
`10 / 0`. El servidor responde con un mensaje, no con una excepción.

![División por cero](capturas/03-division-por-cero.png)

### 4. Log del servidor

El servidor registra cada conexión con la IP y el puerto efímero del cliente.
Los puertos de origen distintos (`56549`, `56553`, `56555`) confirman que cada
operación abre una conexión nueva.

![Log del servidor](capturas/04-log-del-servidor.png)

### 5. Cliente con el servidor apagado — `ConnectException`

Evidencia para la pregunta 1 del informe: `java.net.ConnectException:
Connection refused: connect`, lanzada por el constructor de `Socket`.

![Servidor no disponible](capturas/05-servidor-no-disponible.png)

---

## Detalles de la implementación

- **Servidor secuencial en bucle.** Atiende un cliente completo, cierra la
  conexión y vuelve a `accept()`. Sin hilos.
- **Una conexión por operación.** Cada cálculo abre y cierra su propio socket.
- **`try-with-resources`** en ambos extremos, para garantizar el cierre de
  sockets y streams incluso ante una excepción.
- **El servidor nunca cae por culpa de un cliente.** Los errores de E/S de una
  conexión se informan y el bucle continúa atendiendo a los demás.
- **Los errores de aplicación viajan como datos**, no como excepciones: el
  cliente siempre recibe una respuesta y nunca queda bloqueado en `readLine()`.

---

## Autor

Trabajo Práctico N° 1 — Desarrollo de Aplicaciones para Ambientes Distribuidos
