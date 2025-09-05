# Taller RMI – Biblioteca 


## 1. Descripción
Este proyecto implementa un servicio de **Biblioteca** usando **Java RMI**.  
El objeto remoto permite realizar operaciones de préstamo, consulta y devolución de libros almacenados en un archivo (`libros.txt`).  

Se cumplen los requisitos del taller:
- Métodos remotos síncronos:
  - **Préstamo por ISBN**
  - **Préstamo por Título**
  - **Consulta por ISBN**
  - **Devolución por ISBN**
- Persistencia en archivo plano (`libros.txt`).
- Cliente/Servidor corriendo en distintas computadoras.
- Soporte para múltiples clientes concurrentes.

---

## 2. Estructura de Archivos

```
lab02_clienteServidor/
│
├── BibliotecaRemote.java        # Interfaz remota con los métodos
├── BibliotecaRemoteImpl.java    # Implementación del servicio remoto
├── Libro.java                   # Clase modelo (Serializable)
├── GestorArchivo.java           # Manejo del archivo libros.txt
├── Servidor.java                # Servidor RMI
├── Cliente.java                 # Cliente de pruebas
├── libros.txt                   # Datos iniciales de la biblioteca
└── README.md                    # Instrucciones de uso
```

---

## 3. Datos de prueba (`libros.txt`)

```txt
978-0134685991,Effective Java,3
978-0596009205,Head First Java,2
978-0321356680,Java Concurrency in Practice,1
978-0132350884,Clean Code,4
978-0201633610,Design Patterns,2
```

Cada línea sigue el formato:  
`isbn,titulo,cantidad`

---

## 4. Compilación

Desde la carpeta del proyecto:

```bash
# Eliminar clases viejas
rm -f *.class

# Compilar todas las clases
javac *.java
```

En Windows (PowerShell):

```powershell
del *.class
javac *.java
```

---

## 5. Ejecución

### 5.1. Levantar el servidor (Mac o Windows)
En la terminal del servidor:

```bash
java -Djava.rmi.server.hostname=IP_DEL_SERVIDOR Servidor
```

Ejemplo (Mac con IP 192.168.5.131):

```bash
java -Djava.rmi.server.hostname=192.168.5.131 Servidor
```

El servidor imprime:
```
Servidor RMI listo en puerto 1099 con nombre: BibliotecaService
```

---

### 5.2. Ejecutar el cliente (otra computadora)
En la terminal del cliente (Windows en este caso):

```powershell
java Cliente 192.168.5.131 1099
```

El cliente imprime las operaciones de prueba: consulta, préstamo, devolución.

---

## 6. Ejemplo de salida

Cliente:
```
===== MENÚ BIBLIOTECA =====
1. Consultar libro por ISBN
2. Prestar libro por ISBN
3. Prestar libro por Título
4. Devolver libro por ISBN
5. Salir
Elija una opción: 1
Ingrese ISBN: 978-0134685991
ISBN 978-0134685991: disponibles=0
```

Servidor:
```
Servidor RMI listo en puerto 1099 con nombre: BibliotecaService
```

Archivo `libros.txt` actualizado en servidor:
```
978-0134685991,Effective Java,2
978-0596009205,Head First Java,2
978-0321356680,Java Concurrency in Practice,1
978-0132350884,Clean Code,4
978-0201633610,Design Patterns,2
```

---

## 7. Video de entrega 

El video muestra:
1. Estado inicial de `libros.txt`.
2. Compilación del proyecto.
3. Ejecución del servidor.
4. Ejecución del cliente desde otra computadora.
5. Resultados de **todos los métodos remotos**.
6. Archivo `libros.txt` actualizado en el servidor.
7. Prueba con **dos clientes simultáneos** conectados al mismo servidor.

---

## LINK YOTUBE VIDEO 

www.youtube.com/watch?v=pKW4iqJt7OY&feature=youtu.be

---

## 8. Conclusiones - Observaciones

1. El taller permitió entender cómo funciona la invocación de métodos remotos en Java, estableciendo la comunicación entre cliente y servidor de manera transparente, como si fueran llamadas locales.
2. Se demostró la importancia de separar responsabilidades: el servidor gestiona la lógica de negocio y la persistencia, mientras que el cliente únicamente consume los servicios publicados.
3. Se validó que varios clientes pueden interactuar al mismo tiempo con el servidor sin pérdida de datos, lo cual es esencial en aplicaciones distribuidas.
4. La estructura del proyecto (interfaz remota, implementación, clases modelo, gestor de archivos) permitió aplicar principios de modularidad, reutilización y claridad en el diseño.


