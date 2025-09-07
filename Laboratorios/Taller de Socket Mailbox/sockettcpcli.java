import java.net.*;
import java.io.*;


// Implementación de la clase de un cliente TCP básico que se conecta a un servidor
// a través de un socket en el puerto 6001. El cliente puede enviar mensajes al servidor
// hasta que envia un mensaje de fin.
public class sockettcpcli {
   public static void main(String argv[]) {
      //si no se pasa en el argumento la ip del servidor, imprime error y sale
      if (argv.length == 0) {
         System.err.println("java sockettcpcli servidor");
         System.exit(1);
      }
      //Inicialización de un lector de entrada para capturar los mensajes del cliente
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

      System.out.println("Prueba de sockets TCP (cliente)");
      //Objeto para manejar la conexión TCP
      Socket socket;
      //Dirección IP del servidor
      InetAddress address;
      //Array de bytes para el mensaje
      byte[] mensaje_bytes = new byte[256];
      //String para guardar el mensaje del cliente
      String mensaje="";

      try {
         System.out.print("Capturando direccion de host... ");
         //Captura la dirección IP del servidor ingresado como argumento en la linea de comandos
         address=InetAddress.getByName(argv[0]);
         //Imprime un ok cuando lo realice correctamente
         System.out.println("ok");

         System.out.print("Creando socket... ");
         //Se crea un socket para la conexión con el servidor con la dirección IP y el puerto 6001
         socket = new Socket(address,6001);
         //Imprime un ok cuando lo realice correctamente
         System.out.println("ok");

         //Inicializa un flujo de salida para el envio de mensajes al servidor
         DataOutputStream out =
            new DataOutputStream(socket.getOutputStream());

         System.out.println("Introduce mensajes a enviar:");

         do {
            //Lee y guarda el mensaje ingresado por el cliente
            mensaje = in.readLine();
            //Envía el mensaje al servidor
            out.writeUTF(mensaje);
         } while (!mensaje.startsWith("fin")); //Si el mensaje del cliente es "fin" termina la conexión con el servidor
      }
      catch (Exception e) {
         //Captura e imprime cualquier mensaje de error y acaba el programa
         System.err.println(e.getMessage());
         System.exit(1);//Termina el programa con código de error 
      }
   }
}
