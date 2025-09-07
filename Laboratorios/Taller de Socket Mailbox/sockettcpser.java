import java.net.*;
import java.io.*;

//Implementación de la clase de un Servidor TCP que permite la conexión de un cliente
//a través de un socket TCP en el puerto 6001. El servidor recibe el mensaje del cliente y
//lo imprime en pantalla.
public class sockettcpser {
   public static void main(String argv[]) {
      System.out.println("Prueba de sockets TCP (servidor)");
      //Inicializa un objeto para manejar la conexión TCP entrante
      ServerSocket socket;

      try {
         //Se crea un socket en el puerto 6001 para escuchar conexiones
         socket = new ServerSocket(6001);
         //Acepta una conexión entrante de un cliente
         Socket socket_cli = socket.accept();
         //Crea un flujo de entrada para recibir mensajes del cliente
         DataInputStream in =
            new DataInputStream(socket_cli.getInputStream());
         do {
            //String para guardar el mensaje del cliente
            String mensaje ="";
            //Se lee el mensaje UTF enviado por el cliente 
            mensaje = in.readUTF();
            //Se imprime el mensaje 
            System.out.println(mensaje);
         } while (1>0);
      }
      catch (Exception e) {
         //Captura e imprime cualquier error
         System.err.println(e.getMessage());
         System.exit(1); //Termina el programa con código de error
      }
   }
}
