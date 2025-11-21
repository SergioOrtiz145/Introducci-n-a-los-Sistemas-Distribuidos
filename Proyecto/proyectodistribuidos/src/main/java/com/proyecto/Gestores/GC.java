/**
 * ============================================================
 * Titulo: GC (Gestor de Carga) 
 * Autores: Ana Sofia Grass, Sergio Ortiz, Isabella Palacio, Sebastian Vargas
 * Fecha: 2025-11-15
 * ============================================================
 * El Gestor de Carga (GC) es un componente central del sistema de gestión de bibliotecas
 * distribuido que actúa como intermediario entre el Cliente PS y los actores
 * especializados (ActorPrestamo, ActorDevolver, ActorRenovar).
 * ============================================================
 */
package com.proyecto.Gestores;

import org.zeromq.SocketType;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import com.google.gson.Gson;
import java.time.LocalDateTime;
import java.util.*;

public class GC {
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        String direccionActorPrestamo = args.length > 0 ? args[0] : "tcp://127.0.0.1:5559";

        System.out.println("===============================================");
        System.out.println("  GESTOR DE CARGA (GC) - Iniciando");
        System.out.println("===============================================");
        System.out.println("Actor Prestamo: " + direccionActorPrestamo);
        System.out.println("===============================================\n");

        try (ZContext context = new ZContext()) {
            Socket socketPS = context.createSocket(SocketType.REP);
            socketPS.bind("tcp://*:5565");
            System.out.println("[OK] Socket PS iniciado en puerto 5565");

            Socket socketPrestamo = context.createSocket(SocketType.REQ);
            socketPrestamo.connect(direccionActorPrestamo);
            socketPrestamo.setReceiveTimeOut(7000);
            System.out.println("[OK] Conectado a ActorPrestamo");

            Socket socketDevolver = context.createSocket(SocketType.PUB);
            socketDevolver.bind("tcp://*:5557");
            System.out.println("[OK] Socket Devoluciones (PUB) en puerto 5557");

            Socket socketRenovar = context.createSocket(SocketType.PUB);
            socketRenovar.bind("tcp://*:5558");
            System.out.println("[OK] Socket Renovaciones (PUB) en puerto 5558");

            System.out.println("\n[LISTO] GC esperando solicitudes...\n");

            while (!Thread.currentThread().isInterrupted()) {
                byte[] mensajeBytes = socketPS.recv();
                String solicitudTexto = new String(mensajeBytes, ZMQ.CHARSET);

                System.out.println("\n[SOLICITUD] Recibida: " + solicitudTexto);

                String[] partes = solicitudTexto.split(",");

                if (partes.length < 2) {
                    String error = "ERROR: Formato invalido. Use: OPERACION,PARAMETROS";
                    System.err.println("  [ERROR] " + error);
                    socketPS.send(error.getBytes());
                    continue;
                }

                String operacion = partes[0].trim();

                switch (operacion) {
                    case "PRESTAR":
                        manejarPrestamo(socketPS, socketPrestamo, partes);
                        break;

                    case "DEVOLVER":
                        manejarDevolucion(socketPS, socketDevolver, partes);
                        break;

                    case "RENOVAR":
                        manejarRenovacion(socketPS, socketRenovar, partes);
                        break;

                    default:
                        String errorOp = "ERROR: Operacion desconocida: " + operacion;
                        System.err.println("  [ERROR] " + errorOp);
                        socketPS.send(errorOp.getBytes());
                }
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Error en GC: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void manejarPrestamo(Socket socketPS, Socket socketPrestamo, String[] partes) {
        try {
            if (partes.length < 3) {
                String error = "ERROR: Use formato PRESTAR,ISBN,USUARIO";
                System.err.println("  [ERROR] " + error);
                socketPS.send(error.getBytes());
                return;
            }

            String isbn = partes[1].trim();
            String usuario = partes[2].trim();

            System.out.println("  [PROCESANDO] Prestamo:");
            System.out.println("    - ISBN: " + isbn);
            System.out.println("    - Usuario: " + usuario);

            Map<String, Object> solicitudActor = new HashMap<>();
            solicitudActor.put("operacion", "PRESTAR");
            solicitudActor.put("isbn", isbn);
            solicitudActor.put("usuario", usuario);

            String solicitudJson = gson.toJson(solicitudActor);

            System.out.println("  [ENVIANDO] A ActorPrestamo: " + solicitudJson);
            socketPrestamo.send(solicitudJson.getBytes(ZMQ.CHARSET));

            byte[] respuestaBytes = socketPrestamo.recv();

            if (respuestaBytes == null) {
                String error = "ERROR: ActorPrestamo no responde (timeout)";
                System.err.println("  [ERROR] " + error);
                socketPS.send(error.getBytes());
                return;
            }

            String respuestaActor = new String(respuestaBytes, ZMQ.CHARSET);
            System.out.println("  [RESPUESTA] ActorPrestamo: " + respuestaActor);

            socketPS.send(respuestaBytes);

        } catch (Exception e) {
            String error = "ERROR: Fallo procesando prestamo: " + e.getMessage();
            System.err.println("  [ERROR] " + error);
            socketPS.send(error.getBytes());
        }
    }

    private static void manejarDevolucion(Socket socketPS, Socket socketDevolver, String[] partes) {
        try {
            if (partes.length < 3) {
                String error = "ERROR: Use DEVOLVER,ISBN,USUARIO";
                System.err.println("  [ERROR] " + error);
                socketPS.send(error.getBytes());
                return;
            }

            String isbn = partes[1].trim();
            String usuario = partes[2].trim();
            
            System.out.println("  [PROCESANDO] Devolucion:");
            System.out.println("    - ISBN: " + isbn);
            System.out.println("    - Usuario: " + usuario);

            Map<String, Object> mensajeActor = new HashMap<>();
            mensajeActor.put("operacion", "DEVOLVER");
            mensajeActor.put("isbn", isbn);
            mensajeActor.put("usuario", usuario);
            
            String mensajeJson = gson.toJson(mensajeActor);

            // Respuesta INMEDIATA al PS
            String respuesta = "DEVOLUCION ACEPTADA: Se esta procesando la devolucion";
            socketPS.send(respuesta.getBytes(ZMQ.CHARSET));
            System.out.println("  [OK] Respuesta inmediata enviada al PS");

            // Publicar JSON al topico DEVOLVER
            socketDevolver.sendMore("DEVOLVER");
            socketDevolver.send(mensajeJson.getBytes(ZMQ.CHARSET));

            System.out.println("  [PUBLICADO] Topico DEVOLVER: " + mensajeJson);

        } catch (Exception e) {
            String error = "ERROR: Fallo procesando devolucion: " + e.getMessage();
            System.err.println("  [ERROR] " + error);
            socketPS.send(error.getBytes());
        }
    }

    private static void manejarRenovacion(Socket socketPS, Socket socketRenovar, String[] partes) {
        try {
            if (partes.length < 3) {
                String error = "ERROR: Use RENOVAR,ISBN,USUARIO";
                System.err.println("  [ERROR] " + error);
                socketPS.send(error.getBytes());
                return;
            }

            String isbn = partes[1].trim();
            String usuario = partes[2].trim();
            
            LocalDateTime fechaActual = LocalDateTime.now();
            LocalDateTime fechaNuevaEntrega = fechaActual.plusWeeks(1);
            
            System.out.println("  [PROCESANDO] Renovacion:");
            System.out.println("    - ISBN: " + isbn);
            System.out.println("    - Usuario: " + usuario);
            
            Map<String, Object> mensajeActor = new HashMap<>();
            mensajeActor.put("operacion", "RENOVAR");
            mensajeActor.put("isbn", isbn);
            mensajeActor.put("usuario", usuario);
            mensajeActor.put("fechaActual", fechaActual.toString());
            mensajeActor.put("fechaNuevaEntrega", fechaNuevaEntrega.toString());
            
            String mensajeJson = gson.toJson(mensajeActor);

            // Respuesta INMEDIATA al PS
            String respuesta = String.format(
                    "RENOVACION ACEPTADA: Nueva fecha de entrega: %s",
                    fechaNuevaEntrega.toLocalDate());
            socketPS.send(respuesta.getBytes(ZMQ.CHARSET));
            System.out.println("  [OK] Respuesta inmediata enviada al PS");

            // Publicar JSON al topico RENOVAR
            socketRenovar.sendMore("RENOVAR");
            socketRenovar.send(mensajeJson.getBytes(ZMQ.CHARSET));

            System.out.println("  [PUBLICADO] Topico RENOVAR: " + mensajeJson);

        } catch (Exception e) {
            String error = "ERROR: Fallo procesando renovacion: " + e.getMessage();
            System.err.println("  [ERROR] " + error);
            socketPS.send(error.getBytes());
        }
    }
}
