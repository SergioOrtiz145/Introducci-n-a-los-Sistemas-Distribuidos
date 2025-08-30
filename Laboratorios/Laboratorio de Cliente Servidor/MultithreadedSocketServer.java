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

// Clase principal que implementa un servidor de sockets multihilo
public class MultithreadedSocketServer {
  public static void main(String[] args) throws Exception {
    try{
      //Se crea un servidor de sockets en el puerto 8888
      ServerSocket server=new ServerSocket(8888);
      //Contador para llevar la cuenta de clientes que se conectan
      int counter=0;
      System.out.println("Server Started ....");
      //Bucle infinito para aceptar conexiones del cliente
      while(true){
        //Aumenta en uno cuando entra un cliente 
        counter++;
        //Espera a que un cliente se conecte
        Socket serverClient=server.accept();  //el servidor acepta la solicitud de conexión del cliente
        System.out.println(" >> " + "Client No:" + counter + " started!");
        //Crea un nuevo hilo para manejar la comunicación con el cliente conectado
        ServerClientThread sct = new ServerClientThread(serverClient,counter);
        //inicia el hilo para comunicarse con el cliente
        sct.start();
      }
    }catch(Exception e){
      // Captura cualquier error y lo imprime
      System.out.println(e);
    }
  }
}