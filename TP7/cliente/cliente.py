# Ignacio Ruiz - DNI 39.040.338

import json
import sys
import urllib.error
import urllib.request

SERVIDOR = sys.argv[1] if len(sys.argv) > 1 else "localhost"
URL = "http://" + SERVIDOR + ":8080/productos"


def pedir(metodo, url, datos=None):
    cuerpo = None
    print()
    print(">>> " + metodo + " " + url)
    if datos is not None:
        cuerpo = json.dumps(datos)
        print(">>> JSON enviado: " + cuerpo)
        cuerpo = cuerpo.encode("utf-8")

    pedido = urllib.request.Request(url, data=cuerpo, method=metodo)
    pedido.add_header("Content-Type", "application/json")

    try:
        with urllib.request.urlopen(pedido, timeout=5) as respuesta:
            texto = respuesta.read().decode("utf-8")
            print("<<< " + str(respuesta.status) + " | JSON recibido: " + texto)
            return json.loads(texto)
    except urllib.error.HTTPError as e:
        texto = e.read().decode("utf-8")
        print("<<< " + str(e.code) + " | JSON recibido: " + texto)
        try:
            print("ERROR: " + json.loads(texto)["error"])
        except (ValueError, KeyError):
            print("ERROR: el servidor respondio " + str(e.code))
    except urllib.error.URLError:
        print("ERROR: no se pudo conectar con el servidor en " + URL)
    return None


def mostrar(p):
    print("  #%d  %-15s $%10.2f  stock: %d" % (p["id"], p["nombre"], p["precio"], p["stock"]))


def listar():
    productos = pedir("GET", URL)
    if productos is not None:
        print("Productos (" + str(len(productos)) + "):")
        for p in productos:
            mostrar(p)


def buscar():
    id = input("Id del producto: ").strip()
    producto = pedir("GET", URL + "/" + id)
    if producto is not None:
        print("Producto encontrado:")
        mostrar(producto)


def agregar():
    nombre = input("Nombre: ").strip()
    try:
        precio = float(input("Precio: "))
        stock = int(input("Stock: "))
    except ValueError:
        print("ERROR: precio y stock tienen que ser numeros")
        return
    producto = pedir("POST", URL, {"nombre": nombre, "precio": precio, "stock": stock})
    if producto is not None:
        print("Producto creado:")
        mostrar(producto)


print("=================================================")
print("  CLIENTE DE LA API DE PRODUCTOS")
print("  Servidor: " + URL)
print("=================================================")

while True:
    print()
    print("1) Listar productos")
    print("2) Buscar producto por id")
    print("3) Agregar producto")
    print("0) Salir")
    opcion = input("Opcion: ").strip()

    if opcion == "1":
        listar()
    elif opcion == "2":
        buscar()
    elif opcion == "3":
        agregar()
    elif opcion == "0":
        print("Chau!")
        break
    else:
        print("Opcion invalida")
