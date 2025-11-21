/**
 * ============================================================
 * Título: GCSincrono
 * Autores: Sergio Ortiz, Isabella Palacio, Juan Sebastian Vargas, Ana Sofia Grass
 * Fecha: 2025-11-19
 * ============================================================
 * GCSincrono es un componente que maneja las operaciones de préstamo, 
 * devolución y renovación de libros de manera síncrona, interactuando 
 * con los actores correspondientes para procesar las solicitudes. 
 * Se comunica con los actores de préstamo, devolución y renovación 
 * mediante ZeroMQ y proporciona respuestas basadas en el resultado de 
 * dichas operaciones.
 */
package com.proyecto.Gestores;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import com.google.gson.Gson;

public class GCSincrono {
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        String direccionActorPrestamo = args.length > 0 ? args[0] : "tcp://127.0.0.1:5559";
        String direccionActorDevolver = args.length > 1 ? args[1] : "tcp://127.0.0.1:5560";
        String direccionActorRenovar = args.length > 2 ? args[2] : "tcp://127.0.0.1:5561";


        System.out.println("  GESTOR DE CARGA SÍNCRONO (GC) - Iniciando");
        System.out.println("===============================================");
        System.out.println("Actor Préstamo:   " + direccionActorPrestamo);
        System.out.println("Actor Devolución: " + direccionActorDevolver);
        System.out.println("Actor Renovación: " + direccionActorRenovar);

        try (ZContext context = new ZContext()) {
            Socket socketPS = context.createSocket(SocketType.REP);
            socketPS.bind("tcp://*:5565");
            System.out.println("[OK] Socket PS iniciado en puerto 5565");

            Socket socketPrestamo = context.createSocket(SocketType.REQ);
            socketPrestamo.connect(direccionActorPrestamo);
            socketPrestamo.setReceiveTimeOut(7000);
            System.out.println("[OK] Conectado a ActorPrestamo");

            Socket socketDevolver = context.createSocket(SocketType.REQ);
            socketDevolver.connect(direccionActorDevolver);
            socketDevolver.setReceiveTimeOut(7000);
            System.out.println("[OK] Conectado a ActorDevolver");

            Socket socketRenovar = context.createSocket(SocketType.REQ);
            socketRenovar.connect(direccionActorRenovar);
            socketRenovar.setReceiveTimeOut(7000);
            System.out.println("[OK] Conectado a ActorRenovar");

            System.out.println("\n[LISTO] GC Síncrono esperando solicitudes...\n");

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
                        manejarDevolucionSincrona(socketPS, socketDevolver, partes);
                        break;

                    case "RENOVAR":
                        manejarRenovacionSincrona(socketPS, socketRenovar, partes);
                        break;

                    default:
                        String errorOp = "ERROR: Operacion desconocida: " + operacion;
                        System.err.println("  [ERROR] " + errorOp);
                        socketPS.send(errorOp.getBytes());
                }
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Error en GC: " + e.getMessage());
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

    private static void manejarDevolucionSincrona(Socket socketPS, Socket socketDevolver, String[] partes) {
        try {
            if (partes.length < 3) {
                String error = "ERROR: Use DEVOLVER,ISBN,USUARIO";
                System.err.println("  [ERROR] " + error);
                socketPS.send(error.getBytes());
                return;
            }

            String isbn = partes[1].trim();
            String usuario = partes[2].trim();
            
            System.out.println("  [PROCESANDO] Devolucion SÍNCRONA:");
            System.out.println("    - ISBN: " + isbn);
            System.out.println("    - Usuario: " + usuario);

            Map<String, Object> mensajeActor = new HashMap<>();
            mensajeActor.put("operacion", "DEVOLVER");
            mensajeActor.put("isbn", isbn);
            mensajeActor.put("usuario", usuario);
            
            String mensajeJson = gson.toJson(mensajeActor);
            socketDevolver.send(mensajeJson.getBytes(ZMQ.CHARSET));
            
            byte[] respuestaBytes = socketDevolver.recv();
            if (respuestaBytes == null) {
                String error = "ERROR: ActorDevolver no responde (timeout)";
                System.err.println("  [ERROR] " + error);
                socketPS.send(error.getBytes());
                return;
            }

            String respuestaActor = new String(respuestaBytes, ZMQ.CHARSET);
            System.out.println("  [RESPUESTA] ActorDevolver: " + respuestaActor);
            
            // Responder al PS DESPUÉS de que la BD esté actualizada
            socketPS.send(respuestaBytes);

        } catch (Exception e) {
            String error = "ERROR: Fallo procesando devolucion: " + e.getMessage();
            System.err.println("  [ERROR] " + error);
            socketPS.send(error.getBytes());
        }
    }

    // 🔴 NUEVO MÉTODO SÍNCRONO PARA RENOVACIÓN
    private static void manejarRenovacionSincrona(Socket socketPS, Socket socketRenovar, String[] partes) {
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
            
            System.out.println("  [PROCESANDO] Renovacion SÍNCRONA:");
            System.out.println("    - ISBN: " + isbn);
            System.out.println("    - Usuario: " + usuario);
            
            Map<String, Object> mensajeActor = new HashMap<>();
            mensajeActor.put("operacion", "RENOVAR");
            mensajeActor.put("isbn", isbn);
            mensajeActor.put("usuario", usuario);
            mensajeActor.put("fechaActual", fechaActual.toString());
            mensajeActor.put("fechaNuevaEntrega", fechaNuevaEntrega.toString());
            
            String mensajeJson = gson.toJson(mensajeActor);

            socketRenovar.send(mensajeJson.getBytes(ZMQ.CHARSET));
            
            byte[] respuestaBytes = socketRenovar.recv();
            if (respuestaBytes == null) {
                String error = "ERROR: ActorRenovar no responde (timeout)";
                System.err.println("  [ERROR] " + error);
                socketPS.send(error.getBytes());
                return;
            }

            String respuestaActor = new String(respuestaBytes, ZMQ.CHARSET);
            System.out.println("  [RESPUESTA] ActorRenovar: " + respuestaActor);
            socketPS.send(respuestaBytes);

        } catch (Exception e) {
            String error = "ERROR: Fallo procesando renovacion: " + e.getMessage();
            System.err.println("  [ERROR] " + error);
            socketPS.send(error.getBytes());
        }
    }
}