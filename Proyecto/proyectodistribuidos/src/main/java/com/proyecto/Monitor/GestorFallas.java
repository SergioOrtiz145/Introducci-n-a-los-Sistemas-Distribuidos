package com.proyecto.Monitor;

import java.util.*;
import java.util.concurrent.*;

public class GestorFallas {
    private final MonitorSalud monitorPrimario;
    private final MonitorSalud monitorSecundario;
    private String gaActivo;
    private final String direccionPrimario;
    private final String direccionSecundario;
    private final List<FailoverListener> listeners;
    
    public interface FailoverListener {
        void onFailover(String nuevoDireccionGA, String razon);
    }
    
    public GestorFallas(String direccionPrimario, String direccionSecundario) {
        this.direccionPrimario = direccionPrimario;
        this.direccionSecundario = direccionSecundario;
        this.gaActivo = direccionPrimario;
        this.listeners = new CopyOnWriteArrayList<>();
        
        this.monitorPrimario = new MonitorSalud(direccionPrimario);
        this.monitorSecundario = new MonitorSalud(direccionSecundario);
        
        iniciarGestorFallas();
    }
    
    private void iniciarGestorFallas() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            evaluarEstadoSistema();
        }, 5, 5, TimeUnit.SECONDS);
    }
    
    private void evaluarEstadoSistema() {
        boolean primarioOK = monitorPrimario.isGADisponible();
        boolean secundarioOK = monitorSecundario.isGADisponible();
        
        if (!primarioOK && gaActivo.equals(direccionPrimario)) {
            if (secundarioOK) {
                System.out.println("\n!!! FAILOVER: Cambiando a GA secundario !!!");
                realizarFailover(direccionSecundario, "GA primario caído");
            } else {
                System.err.println("!!! CRÍTICO: Ambos GA no disponibles !!!");
            }
        } else if (primarioOK && gaActivo.equals(direccionSecundario)) {
            System.out.println("\n*** FAILBACK: GA primario recuperado, regresando ***");
            realizarFailover(direccionPrimario, "GA primario recuperado");
        }
    }
    
    private void realizarFailover(String nuevaDireccion, String razon) {
        gaActivo = nuevaDireccion;
        System.out.println("→ Nuevo GA activo: " + gaActivo);
        System.out.println("→ Razón: " + razon);
        
        // Notificar a todos los listeners
        for (FailoverListener listener : listeners) {
            listener.onFailover(gaActivo, razon);
        }
    }
    
    public void agregarListener(FailoverListener listener) {
        listeners.add(listener);
    }
    
    public String getGAActivo() {
        return gaActivo;
    }
}

