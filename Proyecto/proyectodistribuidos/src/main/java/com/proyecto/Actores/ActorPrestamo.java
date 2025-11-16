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
            Socket socketGC = context.createSocket(SocketType.REP);
            socketGC.bind("tcp://*:5559");
            System.out.println("Actor Prestamo iniciado en puerto 5559");
            System.out.println("  -> Conectado a GA1: " + direccionGA1);
            System.out.println("  -> Conectado a GA2: " + direccionGA2);
            
            while (!Thread.currentThread().isInterrupted()) {
                String solicitudGC = socketGC.recvStr();
                System.out.println("\n[SOLICITUD] Recibida: " + solicitudGC);
                
                // ============================================
                // FIX: Parsear JSON en lugar de split(",")
                // ============================================
                Map<String, Object> solicitudJSON = null;
                try {
                    solicitudJSON = gson.fromJson(solicitudGC, 
                        new TypeToken<Map<String, Object>>(){}.getType());
                } catch (Exception e) {
                    System.err.println("ERROR: No se pudo parsear JSON: " + e.getMessage());
                    socketGC.send("ERROR: Formato JSON invalido");
                    continue;
                }
                
                // Extraer valores del JSON parseado
                String operacion = (String) solicitudJSON.get("operacion");
                String isbn = (String) solicitudJSON.get("isbn");
                String usuario = (String) solicitudJSON.get("usuario");
                
                // Validar campos
                if (isbn == null || usuario == null) {
                    System.err.println("ERROR: Faltan campos obligatorios (isbn, usuario)");
                    socketGC.send("ERROR: Faltan campos obligatorios");
                    continue;
                }
                
                if (operacion == null || !operacion.equals("PRESTAR")) {
                    System.err.println("ERROR: Operacion no soportada: " + operacion);
                    socketGC.send("ERROR: Operacion no soportada");
                    continue;
                }
                
                System.out.println("  ISBN: " + isbn);
                System.out.println("  Usuario: " + usuario);
                System.out.println("  Operacion: " + operacion);
                
                // Construir solicitud JSON para el GA (ahora con valores correctos)
                Map<String, Object> solicitudGA = new HashMap<>();
                solicitudGA.put("operacion", "PRESTAMO");
                solicitudGA.put("isbn", isbn);
                solicitudGA.put("usuario", usuario);
                
                String solicitudJson = gson.toJson(solicitudGA);
                System.out.println("  Solicitud al GA: " + solicitudJson);
                
                // Procesar con los GA
                String respuestaFinal = procesarConGA(context, direccionGA1, direccionGA2, solicitudJson);
                socketGC.send(respuestaFinal);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String procesarConGA(ZContext context, String direccionGA1, 
                                        String direccionGA2, String solicitudJson) {
        // Intentar con GA1
        String respuesta = enviarAGA(context, direccionGA1, solicitudJson, "GA1");
        if (respuesta != null) {
            return respuesta;
        }
        
        // Si falla, intentar con GA2
        respuesta = enviarAGA(context, direccionGA2, solicitudJson, "GA2");
        if (respuesta != null) {
            return respuesta;
        }
        
        return "ERROR: Ambos GA no responden";
    }
    
    private static String enviarAGA(ZContext context, String direccionGA, 
                                     String solicitud, String nombreGA) {
        Socket socketGA = context.createSocket(SocketType.REQ);
        socketGA.setReceiveTimeOut(5000);
        socketGA.setSendTimeOut(5000);
        
        try {
            socketGA.connect(direccionGA);
            System.out.println("[" + nombreGA + "] Enviando solicitud...");
            
            boolean enviado = socketGA.send(solicitud);
            if (!enviado) {
                System.err.println("[" + nombreGA + "] No se pudo enviar");
                return null;
            }
            
            String respuestaJson = socketGA.recvStr();
            if (respuestaJson == null) {
                System.err.println("[" + nombreGA + "] Timeout - no responde");
                return null;
            }
            
            Map<String, Object> respuesta = gson.fromJson(respuestaJson, 
                new TypeToken<Map<String, Object>>(){}.getType());
            
            boolean exito = (boolean) respuesta.get("exito");
            String mensaje = (String) respuesta.get("mensaje");
            
            if (exito) {
                System.out.println("[" + nombreGA + "] Exito: " + mensaje);
                return "PRESTAMO EXITOSO (" + nombreGA + "): " + mensaje;
            } else {
                System.out.println("[" + nombreGA + "] Fallo: " + mensaje);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("[" + nombreGA + "] Error: " + e.getMessage());
            return null;
        } finally {
            socketGA.close();
        }
    }
}
