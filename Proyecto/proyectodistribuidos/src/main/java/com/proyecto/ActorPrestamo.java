package com.proyecto;
import org.zeromq.SocketType;
import org.zeromq.ZMQ.Socket;

import zmq.ZMQ;

import org.zeromq.ZContext;

public class ActorPrestamo {
    public static void main(String[] args) {
        try(ZContext context = new ZContext()){
            Socket socketGC = context.createSocket(SocketType.REP);
            socketGC.bind("tcp://localhost:5556");
            System.out.println("Actor Prestamo iniciado en puerto 5556");

            while(!Thread.currentThread().isInterrupted()){
                byte[] reply = socketGC.recv(0);
                System.out.println("Mensaje: "+ new String(reply, ZMQ.CHARSET));
                String respuesta = "Prestamo realizado exitosamente";
                socketGC.send(respuesta.getBytes());
            }
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        
    }
}
