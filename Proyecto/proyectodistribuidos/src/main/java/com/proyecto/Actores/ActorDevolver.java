/**
 * ============================================================
 * Título: ActorDevolver
 * Autores: Ana Sofia Grass, Sergio Ortiz, Isabella Palacio, Sebastián Vargas
 * Fecha: 2025-11-15
 * ============================================================
 * ActorDevolver se comunica con los GA para procesar devoluciones.
 * Usa patrón Pub/Sub para recibir del GC y REQ/REP para hablar con los GA.
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
            // Socket SUB para recibir del GC
            Socket socketGC = context.createSocket(SocketType.SUB);
            socketGC.connect("tcp://localhost:5557");
            socketGC.subscribe("DEVOLVER".getBytes(ZMQ.CHARSET));
            System.out.println("Actor Devolver iniciado, escuchando topico DEVOLVER");
            System.out.println("  -> Conectado a GA1: " + direccionGA1);
            System.out.println("  -> Conectado a GA2: " + direccionGA2);
            
            // Sockets para comunicarse con los GA
            Socket socketGA1 = context.createSocket(SocketType.REQ);
            socketGA1.connect(direccionGA1);
            socketGA1.setReceiveTimeOut(5000);
            
            Socket socketGA2 = context.createSocket(SocketType.REQ);
            socketGA2.connect(direccionGA2);
            socketGA2.setReceiveTimeOut(5000);
            
            while (!Thread.currentThread().isInterrupted()) {
                // Recibir tópico y mensaje
                String topico = socketGC.recvStr();
                String solicitud = socketGC.recvStr();
                System.out.println("\n[" + topico + "] Recibido: " + solicitud);
                
                // Formato: "DEVOLVER,ID_PRESTAMO"
                String[] partes = solicitud.split(",");
                if (partes.length < 2) {
                    System.err.println("ERROR: Formato invalido");
                    continue;
                }
                
                String idPrestamo = partes[1];
                
                // Construir solicitud JSON para el GA
                Map<String, Object> solicitudGA = new HashMap<>();
                solicitudGA.put("operacion", "DEVOLUCION");
                solicitudGA.put("idPrestamo", idPrestamo);
                
                String solicitudJson = gson.toJson(solicitudGA);
                
                // Intentar con GA1 primero
                System.out.println("[GA1] Enviando devolucion...");
                socketGA1.send(solicitudJson);
                String respuestaGA1 = socketGA1.recvStr();
                
                if (respuestaGA1 != null) {
                    Map<String, Object> respuesta = gson.fromJson(respuestaGA1, 
                        new TypeToken<Map<String, Object>>(){}.getType());
                    
                    boolean exito = (boolean) respuesta.get("exito");
                    String mensaje = (String) respuesta.get("mensaje");
                    
                    if (exito) {
                        System.out.println("[GA1] Exito: " + mensaje);
                    } else {
                        // Si GA1 falla, intentar con GA2
                        System.out.println("[GA1] Fallo: " + mensaje);
                        System.out.println("[GA2] Intentando con GA secundario...");
                        
                        socketGA2.send(solicitudJson);
                        String respuestaGA2 = socketGA2.recvStr();
                        
                        if (respuestaGA2 != null) {
                            Map<String, Object> respuesta2 = gson.fromJson(respuestaGA2, 
                                new TypeToken<Map<String, Object>>(){}.getType());
                            
                            boolean exito2 = (boolean) respuesta2.get("exito");
                            String mensaje2 = (String) respuesta2.get("mensaje");
                            
                            System.out.println("[GA2] " + (exito2 ? "Exito" : "Fallo") + ": " + mensaje2);
                        } else {
                            System.err.println("[GA2] No responde");
                        }
                    }
                } else {
                    System.out.println("[GA1] No responde, intentando GA2...");
                    
                    socketGA2.send(solicitudJson);
                    String respuestaGA2 = socketGA2.recvStr();
                    
                    if (respuestaGA2 != null) {
                        Map<String, Object> respuesta2 = gson.fromJson(respuestaGA2, 
                            new TypeToken<Map<String, Object>>(){}.getType());
                        
                        boolean exito2 = (boolean) respuesta2.get("exito");
                        String mensaje2 = (String) respuesta2.get("mensaje");
                        
                        System.out.println("[GA2] " + (exito2 ? "Exito" : "Fallo") + ": " + mensaje2);
                    } else {
                        System.err.println("[ERROR] Ambos GA no responden");
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}