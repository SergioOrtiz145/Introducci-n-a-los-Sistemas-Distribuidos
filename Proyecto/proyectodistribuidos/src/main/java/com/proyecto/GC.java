package com.proyecto;
import org.zeromq.SocketType;
import org.zeromq.ZMQ.Socket;

import zmq.ZMQ;

import org.zeromq.ZContext;

public class GC {
    public static void main(String[] args) {
        try(ZContext context = new ZContext()){
            //Socket con el PS
            Socket socketPS = context.createSocket(SocketType.REP);
            socketPS.bind("tcp://*:5555");
            //Socket con el actor prestamo
            Socket socketPrestamo = context.createSocket(SocketType.REQ);
            socketPrestamo.connect("tcp://localhost:5556");

            while(!Thread.currentThread().isInterrupted()){
                byte[] reply = socketPS.recv();
                String solicitud = new String(reply, ZMQ.CHARSET);
                System.out.println("Mensaje: "+solicitud);
                String[] partes = solicitud.split(",");
                if(partes[0].equals("PRESTAR")){
                    //lógica con el actor que maneje prestar - reply/request
                    System.out.println("El GC fue conectado correctamente al actor Prestamo.");
                    socketPrestamo.send(solicitud.getBytes(), 0);
                    System.out.println("Se envió mensaje al actor Prestamo.");
                    byte[] replyPrestamo = socketPrestamo.recv(0);
                    System.out.println("Mensaje del actor: "+ new String(replyPrestamo, ZMQ.CHARSET));
                    socketPS.send(replyPrestamo);
                }else if(partes[0].equals("DEVOLVER")){
                    //lógica con el actor que maneje devolver - publish/subscribe
                }else if(partes[0].equals("RENOVAR")){
                    //lógica con el actor que maneje renovar - publish/subscribe
                }
            }
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }
    
}
