import java.net.*;
import java.io.*;
// crea la clase socketudpcli y su metodo principal main
public class socketudpcli {
   public static void main(String argv[]) {
      // verifica que el usuario haya pasado un argumento en este caso la IP del servidor 
      if (argv.length == 0) {
         System.err.println("Java socketudpcli servidor");
         System.exit(1); // Termina el programa si no hay argumentos 
      }
      // crea un lector de texto para que el usuario escriba en consola 
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

      System.out.println("Prueba de sockets UDP (cliente)");
      // declaracion de variables 
      DatagramSocket socket; // socket UDP del cliente 
      InetAddress address;  // direccion IP del servidor 
      byte[] mensaje_bytes = new byte[256]; // Buffer de bytes para el mensaje 
      String mensaje=""; // Mensaje de texto 
      DatagramPacket paquete; // paquete UDP que contiene los datos 

      mensaje_bytes=mensaje.getBytes();
      try {
         // Crea el socket UDP 
         System.out.print("Creando socket... ");
         socket = new DatagramSocket();
         System.out.println("ok");
         
         // Obtiene la dirreccion IP del servidor 
         System.out.print("Capturando direccion de host... ");
         address=InetAddress.getByName(argv[0]);
         System.out.println("ok");

         // El usuario ingresa el mensaje a enviar 
         System.out.println("Introduce mensajes a enviar:");

         do {
            mensaje = in.readLine();// lee la linea desde el teclado 
            mensaje_bytes = mensaje.getBytes(); // convierte el mensaje en bytes 
            // crea un datagrama llamado paquete con el mensaje, la dirreccion y el puerto al cual se va a conectar 
            paquete = new DatagramPacket(mensaje_bytes,mensaje.length(),address,6000);
            // ennvia el paquete al servidor 
            socket.send(paquete);
         } while (!mensaje.startsWith("fin"));
      }
      catch (Exception e) {
         // si ocurre un error, se muestra un mensaje y el programa termina 
         System.err.println(e.getMessage());
         System.exit(1);
      }
   }
}

