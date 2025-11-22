/**
 * ============================================================
 * Titulo: ActorDevolverSincrono
 * Autores: Ana Sofia Grass, Sergio Ortiz, Isabella Palacio, Sebastian Vargas
 * Fecha: 2025-11-15
 * ============================================================
 * El ActorDevolverSincrono es un componente especializado del sistema de gestión de
 * bibliotecas distribuido responsable de procesar solicitudes de devolución de libros
 * de forma sincrónica. Se bloquea el GC hasta que se completa la devolución, asegurando que  
 * si se realice o no la actualización de la disponibilidad de libros en el sistema.
 * ============================================================
 */

package com.proyecto.Actores;

import java.util.HashMap;
import java.util.Map;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Socket;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class ActorDevolverSincrono {
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java ActorDevolverSincrono <direccionGA1> <direccionGA2>");
            System.out.println("Ejemplo: java ActorDevolverSincrono tcp://localhost:5555 tcp://localhost:6555");
            return;
        }

        String direccionGA1 = args[0];
        String direccionGA2 = args[1];

        try (ZContext context = new ZContext()) {
            Socket socketGC = context.createSocket(SocketType.REP);
            socketGC.bind("tcp://*:5560"); 
            
            System.out.println("Actor Devolver SÍNCRONO iniciado en puerto 5560");
            System.out.println("  -> Conectado a GA1: " + direccionGA1);
            System.out.println("  -> Conectado a GA2: " + direccionGA2);

            while (!Thread.currentThread().isInterrupted()) {
                // Recibir solicitud del GC
                String solicitudGC = socketGC.recvStr();
                System.out.println("\n[SOLICITUD] Recibida: " + solicitudGC);

                Map<String, Object> solicitudJSON;
                try {
                    solicitudJSON = gson.fromJson(solicitudGC, 
                        new TypeToken<Map<String, Object>>(){}.getType());
                } catch (JsonSyntaxException e) {
                    System.err.println("ERROR: No se pudo parsear JSON: " + e.getMessage());
                    socketGC.send("ERROR: Formato JSON invalido");
                    continue;
                }
                
                String isbn = (String) solicitudJSON.get("isbn");
                String usuario = (String) solicitudJSON.get("usuario");
                String idPrestamo = (String) solicitudJSON.get("idPrestamo");
                
                if ((isbn == null || usuario == null) && idPrestamo == null) {
                    System.err.println("ERROR: Faltan campos obligatorios");
                    socketGC.send("ERROR: Faltan campos obligatorios");
                    continue;
                }

                System.out.println("  ISBN: " + isbn);
                System.out.println("  Usuario: " + usuario);

                // Construir solicitud para GA
                Map<String, Object> solicitudGA = new HashMap<>();
                solicitudGA.put("operacion", "DEVOLUCION");
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
                String respuestaFinal = procesarConGA(context, direccionGA1, 
                                                      direccionGA2, solicitudJson);
                
                // Enviar respuesta al GC
                socketGC.send(respuestaFinal);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
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
            System.out.println("[" + nombreGA + "] Enviando devolución...");

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
                System.out.println("[" + nombreGA + "] Éxito: " + mensaje);
                return "DEVOLUCION EXITOSA (" + nombreGA + "): " + mensaje;
            } else {
                System.out.println("[" + nombreGA + "] Fallo: " + mensaje);
                return "ERROR: " + mensaje;
            }

        } catch (JsonSyntaxException e) {
            System.err.println("[" + nombreGA + "] Error: " + e.getMessage());
            return null;
        } finally {
            socketGA.close();
        }
    }
}