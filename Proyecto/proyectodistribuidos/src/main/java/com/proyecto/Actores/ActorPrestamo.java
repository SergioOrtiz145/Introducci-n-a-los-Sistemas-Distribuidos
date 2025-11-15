/**
 * ============================================================
 * Título: ActorPrestamo
 * Autores: Ana Sofia Grass, Sergio Ortiz, Isabella Palacio, Sebastián Vargas
 * Fecha: 2025-11-15
 * ============================================================
 * ActorPrestamo se comunica directamente con los GA para procesar préstamos.
 * Ahora usa JSON y maneja respuestas del GA.
 */
package com.proyecto.Actores;

import org.zeromq.SocketType;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZContext;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.*;

public class ActorPrestamo {
    private static final Gson gson = new Gson();
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java ActorPrestamo <direccionGA1> <direccionGA2>");
            System.out.println("Ejemplo: java ActorPrestamo tcp://localhost:5555 tcp://localhost:6555");
            return;
        }
        
        String direccionGA1 = args[0];
        String direccionGA2 = args[1];
        
        try (ZContext context = new ZContext()) {
            // Socket para recibir solicitudes del GC
            Socket socketGC = context.createSocket(SocketType.REP);
            socketGC.bind("tcp://*:5556");
            System.out.println("Actor Prestamo iniciado en puerto 5556");
            System.out.println("  -> Conectado a GA1: " + direccionGA1);
            System.out.println("  -> Conectado a GA2: " + direccionGA2);
            
            // Sockets para comunicarse con los GA
            Socket socketGA1 = context.createSocket(SocketType.REQ);
            socketGA1.connect(direccionGA1);
            socketGA1.setReceiveTimeOut(5000); // Timeout de 5 segundos
            
            Socket socketGA2 = context.createSocket(SocketType.REQ);
            socketGA2.connect(direccionGA2);
            socketGA2.setReceiveTimeOut(5000);
            
            while (!Thread.currentThread().isInterrupted()) {
                // Recibir solicitud del GC en formato: "PRESTAR,ISBN,USUARIO"
                String solicitudGC = socketGC.recvStr();
                System.out.println("\n[SOLICITUD] Recibida: " + solicitudGC);
                
                String[] partes = solicitudGC.split(",");
                if (partes.length < 3) {
                    socketGC.send("ERROR: Formato invalido. Use: PRESTAR,ISBN,USUARIO");
                    continue;
                }
                
                String operacion = partes[0];
                String isbn = partes[1];
                String usuario = partes[2];
                
                if (!operacion.equals("PRESTAR")) {
                    socketGC.send("ERROR: Operacion no soportada");
                    continue;
                }
                
                // Construir solicitud JSON para el GA
                Map<String, Object> solicitudGA = new HashMap<>();
                solicitudGA.put("operacion", "PRESTAMO");
                solicitudGA.put("isbn", isbn);
                solicitudGA.put("usuario", usuario);
                
                String solicitudJson = gson.toJson(solicitudGA);
                
                // Intentar con GA1 primero
                System.out.println("[GA1] Enviando solicitud...");
                socketGA1.send(solicitudJson);
                String respuestaGA1 = socketGA1.recvStr();
                
                if (respuestaGA1 != null) {
                    Map<String, Object> respuesta = gson.fromJson(respuestaGA1, 
                        new TypeToken<Map<String, Object>>(){}.getType());
                    
                    boolean exito = (boolean) respuesta.get("exito");
                    String mensaje = (String) respuesta.get("mensaje");
                    
                    if (exito) {
                        System.out.println("[GA1] Exito: " + mensaje);
                        socketGC.send("PRESTAMO EXITOSO: " + mensaje);
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
                            
                            if (exito2) {
                                System.out.println("[GA2] Exito: " + mensaje2);
                                socketGC.send("PRESTAMO EXITOSO (GA2): " + mensaje2);
                            } else {
                                System.out.println("[GA2] Fallo: " + mensaje2);
                                socketGC.send("ERROR: Libro no disponible en ninguna sede");
                            }
                        } else {
                            socketGC.send("ERROR: GA2 no responde");
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
                        
                        if (exito2) {
                            System.out.println("[GA2] Exito: " + mensaje2);
                            socketGC.send("PRESTAMO EXITOSO (GA2): " + mensaje2);
                        } else {
                            System.out.println("[GA2] Fallo: " + mensaje2);
                            socketGC.send("ERROR: " + mensaje2);
                        }
                    } else {
                        socketGC.send("ERROR: Ambos GA no responden");
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}