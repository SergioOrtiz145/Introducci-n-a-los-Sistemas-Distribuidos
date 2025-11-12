package com.proyecto.Modelos;

import java.time.LocalDateTime;

public class Prestamo {
    private String idPrestamo;
    private String isbn;
    private String usuario;
    private LocalDateTime fechaPrestamo;
    private LocalDateTime fechaDevolucion;
    private String sede;
    private boolean prestamoActivo = true;
    private int numRenovaciones = 0;

    public Prestamo(String idPrestamo, String isbn, String usuario, String sede) {
        this.idPrestamo = idPrestamo;
        this.isbn = isbn;
        this.usuario = usuario;
        this.fechaPrestamo = LocalDateTime.now();
        this.sede = sede;
    }

    public String toCSV() {
        return String.format("%s,%s,%s,%s,%s,%s",
                idPrestamo, isbn, usuario, fechaPrestamo,
                fechaDevolucion != null ? fechaDevolucion : "", sede);
    }

    public boolean puedeRenovarse() {
        return prestamoActivo && numRenovaciones < 2;
    }

    public String getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(String idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFechaPrestamo() {
        return fechaPrestamo;
    }

    public void setFechaPrestamo(LocalDateTime fechaPrestamo) {
        this.fechaPrestamo = fechaPrestamo;
    }

    public LocalDateTime getFechaDevolucion() {
        return fechaDevolucion;
    }

    public void setFechaDevolucion(LocalDateTime fechaDevolucion) {
        this.fechaDevolucion = fechaDevolucion;
    }

    public String getSede() {
        return sede;
    }

    public void setSede(String sede) {
        this.sede = sede;
    }

    public boolean isPrestamoActivo() {
        return prestamoActivo;
    }

    public void setPrestamoActivo(boolean prestamoActivo) {
        this.prestamoActivo = prestamoActivo;
    }

    public int getNumRenovaciones() {
        return numRenovaciones;
    }

    public void setNumRenovaciones(int numRenovaciones) {
        this.numRenovaciones = numRenovaciones;
    }
    

}
