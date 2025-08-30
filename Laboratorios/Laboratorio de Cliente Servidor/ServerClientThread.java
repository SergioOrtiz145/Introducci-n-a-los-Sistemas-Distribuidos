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

// Clase que representa un hilo de atención para cada cliente que se conecta al servidor
public class ServerClientThread extends Thread {
  //Socket de conexión con el cliente
  Socket serverClient;
  //Número de identificación del cliente 
  int clientNo;
  //Variable para almacenar el cuadrado del número enviado por el cliente 
  int squre;
  //Constructor de la clase, recibe el socket y número del cliente 
  public ServerClientThread(Socket inSocket,int counter){
    //Inicializa el socket del cliente 
    serverClient = inSocket;
    //Asigna el número del cliente
    clientNo=counter;
  }
  //Método ejecutando cuando se inicializa el hilo
  public void run(){
    try{
      //Crea los flujos de entrada y salida para la comunicación con el cliente
      DataInputStream inStream = new DataInputStream(serverClient.getInputStream());
      DataOutputStream outStream = new DataOutputStream(serverClient.getOutputStream());
      //Variables para almacenar los mensajes tanto del cliente como del servidor
      String clientMessage="", serverMessage="";
      //Bucle que permanece activo para recibir y enviar mensajes, hasta que el cliente envie un mensaje "bye"
      while(!clientMessage.equals("bye")){
        //Lee el mensaje enviado por el cliente
        clientMessage=inStream.readUTF();
        System.out.println("From Client-" +clientNo+ ": Number is :"+clientMessage);
        //Calcula el cuadrado del número recibido
        squre = Integer.parseInt(clientMessage) * Integer.parseInt(clientMessage);
        serverMessage="From Server to Client-" + clientNo + " Square of " + clientMessage + " is " +squre;
        //Envia el mensaje de respuesta para el cliente
        outStream.writeUTF(serverMessage);
        //Asegura que el mensaje sea enviado de inmediato
        outStream.flush();
      }
      //Se cierra los flujos de entrada y salida para la comunicación con el cliente
      inStream.close();
      outStream.close();
      //Cierra la conexión con el cliente
      serverClient.close();
    }catch(Exception ex){
      // Captura cualquier error y lo imprime
      System.out.println(ex);
    }finally{
      System.out.println("Client -" + clientNo + " exit!! ");
    }
  }
}