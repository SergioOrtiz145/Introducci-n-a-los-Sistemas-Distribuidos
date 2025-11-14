package com.proyecto.Monitor;

import org.zeromq.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.*;
import java.util.concurrent.*;

public class MonitorSalud {
    private final String direccionGA;
    private final Gson gson;
    private ZContext context;
    private volatile boolean gaDisponible = false;
    private final ScheduledExecutorService scheduler;
    
    public MonitorSalud(String direccionGA) {
        this.direccionGA = direccionGA;
        this.gson = new Gson();
        this.context = new ZContext();
        this.scheduler = Executors.newScheduledThreadPool(1);
        iniciarMonitoreo();
    }
    
    private void iniciarMonitoreo() {
        // Verificar cada 3 segundos
        scheduler.scheduleAtFixedRate(() -> {
            verificarSalud();
        }, 0, 3, TimeUnit.SECONDS);
    }
    
    private void verificarSalud() {
        ZMQ.Socket tempSocket = null;
        try {
            tempSocket = context.createSocket(SocketType.REQ);
            tempSocket.setReceiveTimeOut(2000); // Timeout de 2 segundos
            tempSocket.connect(direccionGA);
            
            // Enviar health check
            Map<String, String> request = new HashMap<>();
            request.put("tipo", "HEALTH_CHECK");
            tempSocket.send(gson.toJson(request));
            
            // Esperar respuesta
            String respuestaJson = tempSocket.recvStr();
            
            if (respuestaJson != null) {
                Map<String, Object> respuesta = gson.fromJson(respuestaJson, new TypeToken<Map<String, Object>>(){}.getType());
                String estado = (String) respuesta.get("estado");
                boolean bdDisponible = (boolean) respuesta.get("bdDisponible");
                
                if ("OK".equals(estado) && bdDisponible) {
                    if (!gaDisponible) {
                        System.out.println("✓ GA disponible en " + direccionGA);
                    }
                    gaDisponible = true;
                } else {
                    if (gaDisponible) {
                        System.err.println("✗ GA con problemas en " + direccionGA);
                    }
                    gaDisponible = false;
                }
            } else {
                if (gaDisponible) {
                    System.err.println("✗ GA no responde en " + direccionGA);
                }
                gaDisponible = false;
            }
            
        } catch (Exception e) {
            if (gaDisponible) {
                System.err.println("✗ Error contactando GA: " + e.getMessage());
            }
            gaDisponible = false;
        } finally {
            if (tempSocket != null) {
                tempSocket.close();
            }
        }
    }
    
    public boolean isGADisponible() {
        return gaDisponible;
    }
    
    public void cerrar() {
        scheduler.shutdown();
        context.close();
    }
}

