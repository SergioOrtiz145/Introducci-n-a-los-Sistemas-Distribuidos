import java.net.*;
import java.io.*;
// crea la clase socketudpser con su clase principal 
public class socketudpser {
   public static void main(String argv[]) {
      System.out.println("Prueba de sockets UDP (servidor)");
      DatagramSocket socket;
      boolean fin = false; // Bandera para saber cuándo detener el servidor

      try {
         // se crea el socket  en el puerto 6000 
         System.out.print("Creando socket... ");
         socket = new DatagramSocket(6000);
         System.out.println("ok");
         // el servidor queda en modo escucha esperando a recibir el mensaje enviado de socketudpcli
         System.out.println("Recibiendo mensajes... ");
         do {
           byte[] mensaje_bytes = new byte[256];
           DatagramPacket paquete = new DatagramPacket(mensaje_bytes,256); 
           socket.receive(paquete); // espera y recibe un paquete 
           String mensaje ="";  
           mensaje = new String(mensaje_bytes);  // Convierte los bytes recibidos en una cadena de texto
           System.out.println(mensaje); // muestra enl mensaje recibido por consola
           // Si el mensaje comienza con "fin", cambia la bandera para terminar el ciclo 
            if (mensaje.startsWith("fin")) fin=true;
         } while (!fin); // el programa se repite hasta que reciba el mensaje "fin"
      }
      catch (Exception e) {
         // Si ocurre un error, se muestra en consola y el programa termina
         System.err.println(e.getMessage());
         System.exit(1);
      }
   }
}

