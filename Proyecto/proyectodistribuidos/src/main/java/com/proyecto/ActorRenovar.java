/**
 * ============================================================
 * Título: ActorRenovar
 * Autores: Ana Sofia Grass, Sergio Ortiz, Isabella Palacio, Sebastián Vargas
 * Fecha: 2025-10-10
 * ============================================================
 * ActorRenovar escucha mensajes en el tópico "RENOVAR" usando ZeroMQ SUB socket.
 * Imprime los mensajes recibidos en consola.
 */
package com.proyecto;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

public class ActorRenovar {
    public static void main(String[] args) {
        // Crear contexto de ZeroMQ para manejar los sockets
        try (ZContext context = new ZContext()) {
            // Crear un socket de tipo SUB (suscriptor)
            Socket socketGC = context.createSocket(SocketType.SUB);
            // Conectar el socket al puerto 5558 en localhost
            socketGC.connect("tcp://localhost:5558");
            // Suscribirse al tópico "RENOVAR"
            socketGC.subscribe("RENOVAR".getBytes(ZMQ.CHARSET));

            // Bucle principal: espera mensajes hasta que el hilo sea interrumpido
            while (!Thread.currentThread().isInterrupted()) {
                // Recibe el tópico del mensaje
                String topico = socketGC.recvStr();
                // Recibe el contenido del mensaje
                String solicitud = socketGC.recvStr();
                // Imprime el mensaje recibido
                System.out.println(topico+": " + solicitud);
            }

        } catch (Exception e) {
            // Imprime cualquier excepción que ocurra
            System.out.println(e.getMessage());
        }
    }
}
