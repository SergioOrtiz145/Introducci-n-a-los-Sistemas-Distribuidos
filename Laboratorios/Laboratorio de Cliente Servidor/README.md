# Laboratorio de Cliente Servidor
En este laboratorio con ayuda del lenguaje Java se implemento una aplicación de red utilizando sockets TCP. Permitiendo
que múltiples clientes se conecten a un servidor de manera simultánea.
Donde cada cliente puede enviar un número al servidor, y este devuelve como respuesta el cuadrado del número recibido.
El sistema funciona en modo multihilo, quiere decir que, el servidor crea un hilo independiente para atender a cada cliente
que se conecta a el.

## Ejemplo de implementación

Para iniciar con la prueba de implementación, se ejecuta el servidor (MultithreadedSocketServer) para que se "aliste" para
empezar a recibir conexiones de clientes tanto locales como remotos.
<img width="945" height="80" alt="image" src="https://github.com/user-attachments/assets/424c2abf-dd91-46ef-8db6-c298707a6451" />

El servidor ya activo para recibir a los clientes, se ejecutan los clientes (TCPClient), locales como remotos.
<img width="809" height="60" alt="image" src="https://github.com/user-attachments/assets/36b5b392-def8-4a5a-8a93-dfe9a0cdb388" />

Aquí el servidor imprime cada que un cliente se conecte a el e imprime el número ingresado de cada cliente.
<img width="1086" height="143" alt="image" src="https://github.com/user-attachments/assets/6751362b-7d06-4570-b13b-e9201daeb14e" />

Por otro lado, en la consola del cliente imprimira el número ingresado y el resultado de elevar ese número al cuadrado.
<img width="1077" height="101" alt="image" src="https://github.com/user-attachments/assets/8e0617df-08ee-4399-b081-90c9e3d7bd56" />
<img width="392" height="61" alt="image" src="https://github.com/user-attachments/assets/e9cc4b0d-8e69-4e79-81b0-c213ece95538" />

Para cerrar la conexión con el servidor desde un cliente, solo es necesario escribir "bye".
<img width="1081" height="168" alt="image" src="https://github.com/user-attachments/assets/d18fc88b-04e8-4993-9e3e-7015899c4ca2" />
En la consola del servidor imprimira que cliente salio de la conexión.
<img width="1079" height="170" alt="image" src="https://github.com/user-attachments/assets/fa69f75a-a79a-406d-a1e2-407cd82f859e" />

---

## Ejecución del Cliente-Servidor
1. Compilar los archivos
   
Primero se deben compilar todos los ficheros .java (servidor, cliente e hilos).
En la consola, se ubica en la carpeta del laboratorio y ejecuta:

```bash
javac MultithreadedSocketServer.java ServerClientThread.java TCPClient.java
```
Esto generará los archivos .class necesarios para la ejecución.

2. Iniciar el servidor
   
En una consola, se debe correr primero el servidor
```bash
java MultithreadedSocketServer
```

3. Conectar clientes locales
   
En otra consola, en la misma máquina, se puede iniciar un cliente con:
```bash
java TCPClient
```

4. Conectar clientes remotos
   
Para ejecutar otro cliente pero desde otro PC de la red, se debe modificar el archivo TCPClient.java para cambiar
la IP default por la IP del servidor.
```java
Socket socket = new Socket("192.321.4.55", 8888);
```

5. Finalizar la conexión
   
Para cerrar un cliente, solo se necesita escribir:
```bash
bye
```
