# Taller Socket Mailbox
En este laboratorio con ayuda del lenguaje de java se implemento un sistema de comunicación cliente-servidor usando el protocolo TCP y UDP 

### Funcionamiento TCP
1. Servidor (sockettcpser)
- Declara un ServerSocket para escuchar la conexión.
- Crea un socket TCP de servidor en el puerto 6001.
- Espera y acepta la conexión de un cliente.
- Inicializa un flujo de entrada para recibir los mensajes.
- Imprime cada mensaje recibido en pantalla
- Si ocurre alguna excepción, imprime y finaliza el programa.
2. Cliente (sockettcpcli)
- Revisa si se pasa la dirección IP del servidor, si no, muestra error y termina.
- Prepara un Buffer para leer los mensajes desde el teclado.
- Guarda la dirección IP del servidor enviada como argumento.
- Crea un socket TCP con la dirección IP y el puerto 6001.
- Cuando el cliente envia un mensaje, inmediatamente este es enviado al servidor.
- Si el cliente envia un mensaje de "fin", termina la conexión con el servidor.

### Ejemplo de implementación TCP
Para iniciar el envio y recepción de mensajes es necesario ejecutar el servidor TCP para que este a la espera de mensajes del cliente y así imprimirlos por pantalla:

<img width="1021" height="62" alt="image" src="https://github.com/user-attachments/assets/981c6986-a99e-41fe-ad8c-43bae9bbf571" />

Continuamos ejecutando al cliente, ingresando como argumento la dirección IP del servidor al cual hará la conexión.

<img width="1114" height="111" alt="image" src="https://github.com/user-attachments/assets/b3580082-0ae2-436d-aeb0-9a4806bee94a" />

Ya realizada la conexión exitosamente, el cliente puede enviar mensajes al servidor para que estos sean impresos en la consola del servidor.

<img width="1125" height="125" alt="image" src="https://github.com/user-attachments/assets/2ab42ed4-c543-455e-bb43-1dc8a041aecc" />

<img width="1038" height="82" alt="image" src="https://github.com/user-attachments/assets/3a52221c-af49-448d-8a5b-33bc36815226" />

Cuando el cliente no quiera enviar más mensajes solo es necesario que envie un mensaje de "fin" para acabar la conexión con el servidor.

<img width="1116" height="140" alt="image" src="https://github.com/user-attachments/assets/184a545e-ee29-4ddc-9012-b82cc2fc74de" />

<img width="1025" height="106" alt="image" src="https://github.com/user-attachments/assets/fc3f6db0-fe8f-4644-844f-eea44f5e457b" />

---

## Funcionamiento UDP 
1. Servidor (socketudpser)
-  un socket UDP en el puerto 6000.
- Se queda a la espera de mensajes desde cualquier cliente.
- Cada mensaje recibido se convierte en texto y se muestra en consola.
- El servidor se detiene cuando recibe un mensaje que comienza con "fin"
2. Cliente (socketudpcli)
- Crea un socket UDP para enviar mensajes.
- Toma como argumento la dirección IP del servidor (ejemplo: localhost o 192.168.x.x).
- Permite al usuario escribir mensajes desde la terminal.
- Cada mensaje se envía como un datagrama UDP al servidor en el puerto 6000.
- El cliente finaliza cuando el usuario escribe un mensaje que empieza con "fin".
## Ejemplo de implementacion UDP
Para comenzar con las pruebas de implementacion,  se ejecuta el servidor UDP (socketudpser) para que se quede "a la escucha" y pueda recibir mensajes desde los clientes.

<img width="663" height="92" alt="image" src="https://github.com/user-attachments/assets/aab27929-571b-45f9-9cac-b78805870a02" />

Con el servidor activo, se ejecuta el cliente UDP (socketudpcli) en otra terminal, indicando como argumento la dirección IP del servidor (en este caso, localhost o la IP de la máquina).

<img width="651" height="105" alt="image" src="https://github.com/user-attachments/assets/01a197f7-34fd-48a1-834d-def32c1e9054" />

Se introduce el mensaje que se quiere enviar 

<img width="639" height="65" alt="image" src="https://github.com/user-attachments/assets/59202498-7545-49dd-a290-384091623f43" />

por otro lado, en la consola del servidor imprime el mensaje enviado por el cliente UDP( socketudpcli)

<img width="585" height="127" alt="image" src="https://github.com/user-attachments/assets/ea93999a-0b00-426a-8901-dbd38029c1f2" />

para cerrar la conexión del servidor, desde cliente se debe escribir la palabra "fin".

<img width="657" height="176" alt="image" src="https://github.com/user-attachments/assets/0e33317c-61e7-4e80-b0a9-d73143bb6cfe" />

## Comparación de protocolos
1. Orientación en la conexión
- TCP: necesita que el cliente se conecte al servidor con Socket.
- UDP: no establece conexión, solo envía y recibe paquetes usando DatagramSocket.
2. Fiabilidad
- TCP: garantiza que los mensajes lleguen en orden y sin pérdidas.
- UDP: no garantiza la entrega, orden ni evita duplicados de mensajes.
3. Transmisión de datos
- TCP: trabaja con flujos de bytes continuos, por eso el servidor se usan flujos como DataInputStream.
- UDP: trabaja con mensajes individuales, usando DatagramPacket para enviar y recibir datos.
4. Velocidad y eficiencia
- TCP: tiende a ser más lento, porque tiene sobrecarga por control de errores, confirmaciones y gestión de conexión.
- UDP: es más rápido y ligero, porque no hay tanta sobrecarga.

### Conclusión
Que uno sea mejor que otro dependerá de las necesidades de la aplicación, en donde importa la fiabilidad y una conexión segura se usará TCP, ejemplos: correo electrónico, páginas web, transferencia de archivos, etc.
Por otro lado, si importa más la rapidez y una tolerancia a pérdida de datos es mejor usar UDP, ejemplos: servicios de streaming, videojuegos en línea, VoIP (llamadas de voz a través de internet), etc. 

## Ejecución 
1. Compilar los archivos
   
Primero se deben compilar todos los ficheros .java (servidor y cliente).
En la consola, se ubica en la carpeta de talleres y ejecuta
```bash
javac *.java
```
2. Iniciar el servidor
Dependiendo del protocolo a usar se ejecuta el servidor TCP o UDP
En una terminal , se debe correr primero el servidor
```bash
 java socketudpser
```
```bash
 java sockettcpser
```
3. Conectar clientes locales
Dependiendo del protocolo a usar se ejecuta el cliente TCP o UDP 
En otra terminal , en la misma máquina, se puede iniciar un cliente con:
```bash
java  socketudpcli 172.19.100.27
```
```bash
java  sockettcpcli 172.19.100.27
```

4. Finalizar la conexión
   
Para cerrar  cliente, solo se necesita escribir:
```bash
fin
```
