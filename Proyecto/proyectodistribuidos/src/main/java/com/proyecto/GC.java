package com.proyecto;

import org.zeromq.SocketType;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import java.time.LocalDate;

public class GC {
    public static void main(String[] args) {
        try (ZContext context = new ZContext()) {
            // Socket con el PS
            Socket socketPS = context.createSocket(SocketType.REP);
            socketPS.bind("tcp://*:5555");

            // Socket con el actor prestamo
            Socket socketPrestamo = context.createSocket(SocketType.REQ);
            socketPrestamo.connect("tcp://*:5556");

            // Socket con el actor devolver
            Socket socketDevolver = context.createSocket(SocketType.PUB);
            socketDevolver.bind("tcp://*:5557");

            // Socket con el actor renovar
            Socket socketRenovar = context.createSocket(SocketType.PUB);
            socketRenovar.bind("tcp://*:5558");

            while (!Thread.currentThread().isInterrupted()) {
                byte[] reply = socketPS.recv();
                String solicitud = new String(reply, ZMQ.CHARSET);
                System.out.println("Mensaje: " + solicitud);
                String[] partes = solicitud.split(",");
                if (partes[0].equals("PRESTAR")) {
                    // lógica con el actor que maneje prestar - reply/request

                    // Enviar solicitud al actor de prestamo
                    socketPrestamo.send(solicitud.getBytes(), 0);
                    System.out.println("Se envió mensaje al actor Prestamo.");

                    // Recibir mensaje del actor
                    byte[] replyPrestamo = socketPrestamo.recv(0);
                    System.out.println("Mensaje del actor: " + new String(replyPrestamo, ZMQ.CHARSET));

                    // Enviar respuesta al PS
                    socketPS.send(replyPrestamo);
                } else if (partes[0].equals("DEVOLVER")) {
                    // lógica con el actor que maneje devolver - publish/subscribe

                    // Respuesta inmediata al PS
                    String respuesta = "Devolucion aceptada";
                    socketPS.send(respuesta.getBytes());

                    // Públicar al tópico DEVOLVER
                    socketDevolver.sendMore("DEVOLVER");
                    socketDevolver.send(solicitud.getBytes());
                    System.out.println("Se envió mensaje al actor Devolver.");

                } else if (partes[0].equals("RENOVAR")) {
                    // lógica con el actor que maneje renovar - publish/subscribe

                    // Obtener la fecha actual y la nueva fecha de entrega
                    LocalDate fechaHoy = LocalDate.now();
                    LocalDate fechaSemanaProxima = fechaHoy.plusWeeks(1);
                    // Respuesta inmediata al PS
                    String respuesta = "Renovacion aceptada, nueva fecha de entrega: "+fechaSemanaProxima;
                    socketPS.send(respuesta.getBytes());

                    // Públicar al tópico RENOVAR la solicitud junto a las fechas
                    solicitud = solicitud.concat(","+fechaHoy+","+fechaSemanaProxima);
                    socketRenovar.sendMore("RENOVAR");
                    socketRenovar.send(solicitud.getBytes());
                    System.out.println("Se envió mensaje al actor Renovar.");
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
