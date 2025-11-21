/**
 * ============================================================
 * Titulo: ActorDevolver
 * Autores: Ana Sofia Grass, Sergio Ortiz, Isabella Palacio, Sebastian Vargas
 * Fecha: 2025-11-15
 * ============================================================
 * El ActorDevolver es un componente especializado del sistema de gestión de bibliotecas
 * distribuido responsable de procesar solicitudes de devolución de libros. Se suscribe
 * al tópico de publicación DEVOLVER y coordina la devolución de libros prestados con
 * el gestor de carga (GA) de cada sede.
 * ============================================================
 */

package com.proyecto.Actores;

import org.zeromq.SocketType;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.*;

public class ActorDevolver {
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java ActorDevolver <direccionGA1> <direccionGA2>");
            System.out.println("Ejemplo: java ActorDevolver tcp://localhost:5555 tcp://localhost:6555");
            return;
        }

        String direccionGA1 = args[0];
        String direccionGA2 = args[1];

        try (ZContext context = new ZContext()) {
            Socket socketGC = context.createSocket(SocketType.SUB);
            socketGC.connect("tcp://localhost:5557");
            socketGC.subscribe("DEVOLVER".getBytes(ZMQ.CHARSET));
            System.out.println("Actor Devolver iniciado, escuchando topico DEVOLVER, puerto 5557");
            System.out.println("  -> Conectado a GA1: " + direccionGA1);
            System.out.println("  -> Conectado a GA2: " + direccionGA2);

            while (!Thread.currentThread().isInterrupted()) {
                String topico = socketGC.recvStr();
                String solicitud = socketGC.recvStr();
                System.out.println("\n[" + topico + "] Recibido: " + solicitud);

                Map<String, Object> solicitudJSON = null;
                try {
                    solicitudJSON = gson.fromJson(solicitud, 
                        new TypeToken<Map<String, Object>>(){}.getType());
                } catch (Exception e) {
                    System.err.println("ERROR: No se pudo parsear JSON: " + e.getMessage());
                    continue;
                }
                String isbn = (String) solicitudJSON.get("isbn");
                String usuario = (String) solicitudJSON.get("usuario");
                String idPrestamo = (String) solicitudJSON.get("idPrestamo");
                
                // Validar que tengamos al menos isbn+usuario o idPrestamo
                if ((isbn == null || usuario == null) && idPrestamo == null) {
                    System.err.println("ERROR: Faltan campos obligatorios (isbn+usuario) o (idPrestamo)");
                    continue;
                }

                System.out.println("  ISBN: " + isbn);
                System.out.println("  Usuario: " + usuario);
                if (idPrestamo != null) {
                    System.out.println("  ID Prestamo: " + idPrestamo);
                }

                Map<String, Object> solicitudGA = new HashMap<>();
                solicitudGA.put("operacion", "DEVOLUCION");
                
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
        // Intentar con GA1
        boolean exitoGA1 = enviarAGA(context, direccionGA1, solicitudJson, "GA1");
        if (exitoGA1) return;
        
        // Si falla, intentar con GA2
        enviarAGA(context, direccionGA2, solicitudJson, "GA2");
    }

    private static boolean enviarAGA(ZContext context, String direccionGA, 
                                      String solicitud, String nombreGA) {
        Socket socketGA = context.createSocket(SocketType.REQ);
        socketGA.setReceiveTimeOut(5000);
        socketGA.setSendTimeOut(5000);

        try {
            socketGA.connect(direccionGA);
            System.out.println("[" + nombreGA + "] Enviando devolucion...");

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
