package com.proyecto.Actores;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.*;

public class ActorRenovar {
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java ActorRenovar <direccionGA1> <direccionGA2>");
            System.out.println("Ejemplo: java ActorRenovar tcp://localhost:5555 tcp://localhost:6555");
            return;
        }

        String direccionGA1 = args[0];
        String direccionGA2 = args[1];

        try (ZContext context = new ZContext()) {
            Socket socketGC = context.createSocket(SocketType.SUB);
            socketGC.connect("tcp://localhost:5558");
            socketGC.subscribe("RENOVAR".getBytes(ZMQ.CHARSET));
            System.out.println("Actor Renovar iniciado, escuchando topico RENOVAR, puerto 5558");
            System.out.println("  -> Conectado a GA1: " + direccionGA1);
            System.out.println("  -> Conectado a GA2: " + direccionGA2);

            while (!Thread.currentThread().isInterrupted()) {
                String topico = socketGC.recvStr();
                String solicitud = socketGC.recvStr();
                System.out.println("\n[" + topico + "] Recibido: " + solicitud);

                // Parsear JSON recibido del GC
                Map<String, Object> solicitudJSON = null;
                try {
                    solicitudJSON = gson.fromJson(solicitud, 
                        new TypeToken<Map<String, Object>>(){}.getType());
                } catch (Exception e) {
                    System.err.println("ERROR: No se pudo parsear JSON: " + e.getMessage());
                    continue;
                }

                // ============================================
                // CORRECCIÓN: Extraer los campos del JSON
                // ============================================
                String isbn = (String) solicitudJSON.get("isbn");
                String usuario = (String) solicitudJSON.get("usuario");
                String idPrestamo = (String) solicitudJSON.get("idPrestamo");
                String fechaActual = (String) solicitudJSON.get("fechaActual");
                String fechaNuevaEntrega = (String) solicitudJSON.get("fechaNuevaEntrega");
                
                // Validar campos obligatorios
                if ((isbn == null || usuario == null) && idPrestamo == null) {
                    System.err.println("ERROR: Faltan campos obligatorios (isbn+usuario) o (idPrestamo)");
                    continue;
                }

                System.out.println("  ISBN: " + isbn);
                System.out.println("  Usuario: " + usuario);
                if (idPrestamo != null) {
                    System.out.println("  ID Prestamo: " + idPrestamo);
                }
                if (fechaNuevaEntrega != null) {
                    System.out.println("  Nueva fecha entrega: " + fechaNuevaEntrega);
                }

                // ============================================
                // Construir solicitud JSON para el GA con los campos extraídos
                // ============================================
                Map<String, Object> solicitudGA = new HashMap<>();
                solicitudGA.put("operacion", "RENOVACION");
                
                // Incluir todos los campos disponibles
                if (idPrestamo != null) {
                    solicitudGA.put("idPrestamo", idPrestamo);
                }
                if (isbn != null) {
                    solicitudGA.put("isbn", isbn);
                }
                if (usuario != null) {
                    solicitudGA.put("usuario", usuario);
                }
                if (fechaActual != null) {
                    solicitudGA.put("fechaActual", fechaActual);
                }
                if (fechaNuevaEntrega != null) {
                    solicitudGA.put("fechaNuevaEntrega", fechaNuevaEntrega);
                }

                String solicitudJson = gson.toJson(solicitudGA);
                System.out.println("  Solicitud al GA: " + solicitudJson);

                // Procesar con los GA
                procesarConGA(context, direccionGA1, direccionGA2, solicitudJson);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void procesarConGA(ZContext context, String direccionGA1, 
                                      String direccionGA2, String solicitudJson) {
        boolean exitoGA1 = enviarAGA(context, direccionGA1, solicitudJson, "GA1");
        if (exitoGA1) return;
        
        enviarAGA(context, direccionGA2, solicitudJson, "GA2");
    }

    private static boolean enviarAGA(ZContext context, String direccionGA, 
                                      String solicitud, String nombreGA) {
        Socket socketGA = context.createSocket(SocketType.REQ);
        socketGA.setReceiveTimeOut(5000);
        socketGA.setSendTimeOut(5000);

        try {
            socketGA.connect(direccionGA);
            System.out.println("[" + nombreGA + "] Enviando renovacion...");

            boolean enviado = socketGA.send(solicitud);
            if (!enviado) {
                System.err.println("[" + nombreGA + "] No se pudo enviar");
                return false;
            }

            String respuestaJson = socketGA.recvStr();
            if (respuestaJson == null) {
                System.err.println("[" + nombreGA + "] Timeout - no responde");
                return false;
            }

            Map<String, Object> respuesta = gson.fromJson(respuestaJson, 
                new TypeToken<Map<String, Object>>(){}.getType());

            boolean exito = (boolean) respuesta.get("exito");
            String mensaje = (String) respuesta.get("mensaje");

            System.out.println("[" + nombreGA + "] " + (exito ? "Exito" : "Fallo") + ": " + mensaje);
            return exito;

        } catch (Exception e) {
            System.err.println("[" + nombreGA + "] Error: " + e.getMessage());
            return false;
        } finally {
            socketGA.close();
        }
    }
}
