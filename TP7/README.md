# TP7 — API REST de productos

Trabajo Práctico N° 7 — Desarrollo de Aplicaciones para Ambientes Distribuidos
(Lic. Gabriel Artaza).

**Alumno:** Ignacio Ruiz — DNI 39.040.338
**Servidor:** Java + Spring Boot · **Cliente:** Python

Un servidor guarda una lista de productos y un cliente se conecta para verlos y
agregar nuevos. Se mandan los datos en **JSON** por **HTTP**.

| Pedido | Qué hace |
|---|---|
| `GET /productos` | Devuelve todos los productos |
| `GET /productos/{id}` | Devuelve un producto (o `404` si no existe) |
| `POST /productos` | Recibe un producto nuevo y lo guarda |

---

## Archivos

```
TP7/
├── servidor/        El servidor (Spring Boot)
│   └── src/main/java/tp7/
│       ├── Servidor.java            Arranca el servidor
│       ├── Producto.java            Los datos de un producto
│       ├── ProductoController.java  Los pedidos GET y POST
│       └── ManejoDeErrores.java     Las respuestas de error
├── cliente/
│   └── cliente.py   El cliente con menú
├── capturas/
└── README.md
```

---

## Cómo se comunican

```mermaid
flowchart LR
    C["Cliente<br/>(Python)"] -- "pedido HTTP + JSON" --> S["Servidor<br/>(Java, puerto 8080)"]
    S -- "respuesta HTTP + JSON" --> C
```

El cliente pasa los datos a JSON y los manda. El servidor los lee, contesta
con otro JSON y el cliente lo muestra en pantalla.

---

## Cómo compilar y ejecutar

**Terminal A — servidor** (en la carpeta `TP7/servidor`):

```bash
mvn package
java -jar target/servidor-1.0.jar
```

**Terminal B — cliente** (en la carpeta `TP7/cliente`):

```bash
python cliente.py                  # servidor en esta misma compu
python cliente.py 192.168.0.142    # servidor en otra compu de la red
```

---

## Errores

| Caso | Respuesta |
|---|---|
| El producto no existe | `404` |
| Id con letras, JSON mal escrito o datos inválidos | `400` |
| Un error inesperado en el servidor | `500` |
| El servidor está apagado | El cliente avisa y sigue andando |

---

## Capturas

### 1. Pedido exitoso

El cliente lista los productos, agrega uno y lo busca. Se ve lo que manda
(`>>>`) y lo que vuelve (`<<<`).

![Pedido exitoso](capturas/01-invocacion-exitosa.png)

### 2. El JSON que llega

En el servidor se ve cada JSON que recibe:

![JSON en el servidor](capturas/02-json-en-el-servidor.png)

Y con `curl -i` se ve la respuesta completa que viaja por la red:

![JSON en la red](capturas/02-json-en-la-red.png)

### 3. Errores

Producto que no existe, id con letras, nombre vacío, precio negativo y stock
con letras:

![Errores](capturas/03-errores-cliente.png)

JSON mal escrito:

![JSON mal formado](capturas/03-json-mal-formado.png)

Servidor apagado:

![Servidor apagado](capturas/03-servidor-apagado.png)

> Las salidas completas también están como `.txt` en `capturas/`.

---

## Autor

Ignacio Ruiz — DNI 39.040.338
Trabajo Práctico N° 7 — Desarrollo de Aplicaciones para Ambientes Distribuidos
