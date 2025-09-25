package com.proyecto;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

public class ActorRenovar {
    public static void main(String[] args) {
        try (ZContext context = new ZContext()) {
            Socket socketGC = context.createSocket(SocketType.SUB);
            socketGC.connect("tcp://localhost:5558");
            socketGC.subscribe("RENOVAR".getBytes(ZMQ.CHARSET));

            while (!Thread.currentThread().isInterrupted()) {
                String topico = socketGC.recvStr();
                String solicitud = socketGC.recvStr();
                System.out.println(topico+": " + solicitud);
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
