/*
--------------------------------------------------------
Universidad: Pontificia Universidad Javeriana
Carrera: Ingeniería de Sistemas
Asignatura: Introducción a los sistemas distribuidos
Laboratorio: Cliente-Servidor con Sockets TCP
Estudiantes: Sergio Ortiz, Isabella Palacio, Ana Sofia Grass y Sebastian Vargas
Profesor: John Corredor
Fecha: 29/08/2025
--------------------------------------------------------
*/
import java.net.*;
import java.io.*;
// Clase principal del cliente TCP
public class TCPClient {
  public static void main(String[] args) throws Exception {
  try{
    // Crea un socket y establece conexión con el servidor y puerto 8888
    Socket socket=new Socket("127.0.0.1",8888);
    // Flujo de entrada: permite recibir mensajes desde el servidor
    DataInputStream inStream=new DataInputStream(socket.getInputStream());
    // Flujo de salida: permite enviar mensajes al servidor
    DataOutputStream outStream=new DataOutputStream(socket.getOutputStream());
    // Lector de consola para que el usuario pueda escribir mensajes
    BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
    // Variables que almacenan el mensaje del cliente y la respuesta del servidor
    String clientMessage="",serverMessage="";
    // Bucle principal: el cliente envía mensajes hasta que escriba "bye"
    while(!clientMessage.equals("bye")){
      System.out.println("Enter number :"); // Pide al usuario que ingrese un número
      clientMessage=br.readLine(); // Lee el mensaje desde la consola
      outStream.writeUTF(clientMessage); // Envía el mensaje al servidor
      outStream.flush(); // Fuerza el envío inmediato del mensaje
      serverMessage=inStream.readUTF(); // Espera y lee la respuesta del servidor
      System.out.println(serverMessage); // Muestra la respuesta en pantalla
    }
    // Cierra los flujos y el socket
    outStream.close();
    inStream.close();
    socket.close();
  }catch (EOFException e) {
    System.out.println("Conexión finalizada por el servidor.");
  }catch(Exception e){
    // Captura cualquier error y lo imprime
    System.out.println(e);
  }
  }
}