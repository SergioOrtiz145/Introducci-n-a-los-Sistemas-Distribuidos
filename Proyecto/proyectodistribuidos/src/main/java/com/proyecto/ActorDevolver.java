/**
 * ============================================================
 * Título: ActorDevolver
 * Autores: Ana Sofia Grass, Sergio Ortiz, Isabella Palacio, Sebastián Vargas
 * Fecha: 2025-10-10
 * ============================================================
 * ActorDevolver escucha mensajes en el tópico "DEVOLVER" usando ZeroMQ SUB socket.
 * Imprime los mensajes recibidos en consola.
 */
package com.proyecto;

import org.zeromq.SocketType;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;


public class ActorDevolver {
    public static void main(String[] args) {
        // Crear contexto de ZeroMQ para manejar los sockets
        try(ZContext context = new ZContext()){
            // Crear un socket de tipo SUB (suscriptor)
            Socket socketGC = context.createSocket(SocketType.SUB);
            // Conectar el socket al puerto 5557 en localhost
            socketGC.connect("tcp://localhost:5557");
            // Suscribirse al tópico "DEVOLVER"
            socketGC.subscribe("DEVOLVER".getBytes(ZMQ.CHARSET));

            // Bucle principal: espera mensajes hasta que el hilo sea interrumpido
            while(!Thread.currentThread().isInterrupted()){
                // Recibe el tópico del mensaje
                String topico = socketGC.recvStr();
                // Recibe el contenido del mensaje
                String solicitud = socketGC.recvStr();
                // Imprime el mensaje recibido
                System.out.println(topico+": "+solicitud); 
            }

        }catch(Exception e){
            // Imprime cualquier excepción que ocurra
            System.out.println(e.getMessage());
        }
    }
}
