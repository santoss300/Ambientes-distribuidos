# Ignacio Ruiz - DNI 39.040.338

import base64
import http.client
import json
import os
import ssl
import sys
import time
from datetime import datetime
from http import HTTPStatus

HOST = "localhost"
PUERTO = 8443
CERTIFICADO = os.path.join(os.path.dirname(__file__), "certificado.pem")
contexto = ssl.create_default_context(cafile=CERTIFICADO)


def pedir(metodo, ruta, token=None, datos=None):
    encabezados = {"Content-Type": "application/json"}
    if token:
        encabezados["Authorization"] = "Bearer " + token
    cuerpo = json.dumps(datos) if datos else None

    print()
    print(">>> " + metodo + " https://" + HOST + ":" + str(PUERTO) + ruta)
    if cuerpo:
        print(">>> JSON enviado: " + cuerpo)
    if token:
        print(">>> Authorization: Bearer " + token[:30] + "...")

    conexion = http.client.HTTPSConnection(HOST, PUERTO, context=contexto, timeout=5)
    conexion.request(metodo, ruta, cuerpo, encabezados)
    print("    (canal cifrado con " + conexion.sock.version() + ")")
    respuesta = conexion.getresponse()
    texto = respuesta.read().decode("utf-8")
    conexion.close()

    print("<<< " + str(respuesta.status) + " " + HTTPStatus(respuesta.status).phrase)
    print("<<< " + texto)
    return json.loads(texto) if texto else {}


def leer_payload(token):
    parte = token.split(".")[1]
    parte += "=" * (-len(parte) % 4)
    return json.loads(base64.urlsafe_b64decode(parte))


def alterar_payload(token, campo, valor):
    cabecera, payload, firma = token.split(".")
    datos = leer_payload(token)
    datos[campo] = valor
    nuevo = json.dumps(datos, separators=(",", ":")).encode("utf-8")
    nuevo = base64.urlsafe_b64encode(nuevo).decode("utf-8").rstrip("=")
    return cabecera + "." + nuevo + "." + firma


def mostrar_payload(token):
    datos = leer_payload(token)
    print("    Payload del token: " + json.dumps(datos))
    print("    Vence: " + datetime.fromtimestamp(datos["exp"]).strftime("%d/%m/%Y %H:%M:%S"))


def prueba_login():
    print("=== 1. Login y emision del token ===")
    token = pedir("POST", "/login", datos={"usuario": "ana", "clave": "1234"})["token"]
    mostrar_payload(token)
    return token


def prueba_acceso(token):
    print()
    print("=== 2. Acceso con token por HTTPS ===")
    pedir("GET", "/datos", token)
    pedir("GET", "/admin", token)


def prueba_rechazos():
    print()
    print("=== 3. Rechazos ===")
    pedir("GET", "/datos")
    pedir("GET", "/datos", "esto.no.es-un-token")
    pedir("POST", "/login", datos={"usuario": "ana", "clave": "mal"})
    token_beto = pedir("POST", "/login", datos={"usuario": "beto", "clave": "abcd"})["token"]
    pedir("GET", "/admin", token_beto)


def prueba_tampering():
    print()
    print("=== 4. Tampering: se cambia el rol sin volver a firmar ===")
    token = pedir("POST", "/login", datos={"usuario": "beto", "clave": "abcd"})["token"]
    alterado = alterar_payload(token, "rol", "admin")
    print()
    print("    Original: " + json.dumps(leer_payload(token)))
    print("    Alterado: " + json.dumps(leer_payload(alterado)))
    print("    La firma se deja igual. Token alterado:")
    print("    " + alterado)
    pedir("GET", "/admin", alterado)


def prueba_vencido():
    print("=== Token vencido ===")
    token = pedir("POST", "/login", datos={"usuario": "ana", "clave": "1234"})["token"]
    mostrar_payload(token)
    pedir("GET", "/datos", token)
    print()
    print("    Esperando 6 segundos a que venza...")
    time.sleep(6)
    pedir("GET", "/datos", token)


def main():
    modo = sys.argv[1] if len(sys.argv) > 1 else "todo"
    try:
        if modo == "vencido":
            prueba_vencido()
        else:
            token = prueba_login()
            prueba_acceso(token)
            prueba_rechazos()
            prueba_tampering()
    except ssl.SSLCertVerificationError as e:
        print("ERROR: el certificado del servidor no es confiable (" + e.verify_message + ")")
    except ConnectionRefusedError:
        print("ERROR: no se pudo conectar con el servidor en " + HOST + ":" + str(PUERTO))
    except TimeoutError:
        print("ERROR: el servidor no respondio a tiempo")
    except OSError as e:
        print("ERROR de red: " + str(e))


if __name__ == "__main__":
    main()
