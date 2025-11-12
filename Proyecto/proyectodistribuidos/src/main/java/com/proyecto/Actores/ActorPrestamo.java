/**
 * ============================================================
 * Título: ActorPrestamo
 * Autores: Ana Sofia Grass, Sergio Ortiz, Isabella Palacio, Sebastián Vargas
 * Fecha: 2025-10-10
 * ============================================================
 * ActorPrestamo recibe solicitudes de préstamo usando un socket ZeroMQ REP.
 * Responde a cada solicitud con un mensaje de confirmación.
 */
package com.proyecto.Actores;
import org.zeromq.SocketType;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

public class ActorPrestamo {
    public static void main(String[] args) {
        // Crear contexto de ZeroMQ para manejar los sockets
        try(ZContext context = new ZContext()){
            // Crear un socket de tipo REP (receptor de solicitudes)
            Socket socketGC = context.createSocket(SocketType.REP);
            // Asociar el socket al puerto 5556 en localhost
            socketGC.bind("tcp://localhost:5556");
            System.out.println("Actor Prestamo iniciado en puerto 5556");

            // Bucle principal: espera mensajes hasta que el hilo sea interrumpido
            while(!Thread.currentThread().isInterrupted()){
                // Recibe la solicitud como arreglo de bytes
                byte[] reply = socketGC.recv(0);
                // Imprime el mensaje recibido en consola
                System.out.println("Mensaje: "+ new String(reply, ZMQ.CHARSET));
                // Prepara la respuesta
                String respuesta = "Prestamo realizado exitosamente";
                // Envía la respuesta al solicitante
                socketGC.send(respuesta.getBytes());
            }
        }catch(Exception e){
            // Imprime cualquier excepción que ocurra
            System.out.println(e.getMessage());
        }
        
    }
}
