package com.proyecto.Modelos;

public class Libro {
    private String isbn;
    private String titulo;
    private String autor;
    private int ejemplaresDisponibles;
    private int ejemplaresTotales;

    public Libro(String isbn, String titulo, String autor, int ejemplares) {
        this.isbn = isbn;
        this.titulo = titulo;
        this.autor = autor;
        this.ejemplaresTotales = ejemplares;
        this.ejemplaresDisponibles = ejemplares;
    }

    public Libro(String isbn, String titulo, String autor, int ejemplaresTotales, int ejemplaresPrestados) {
        this.isbn = isbn;
        this.titulo = titulo;
        this.autor = autor;
        this.ejemplaresTotales = ejemplaresTotales;
        this.ejemplaresDisponibles = ejemplaresTotales - ejemplaresPrestados;
    }

    public synchronized boolean prestar() {
        if (ejemplaresDisponibles > 0) {
            ejemplaresDisponibles--;
            return true;
        }
        return false;
    }

    public synchronized void devolver() {
        if (ejemplaresDisponibles < ejemplaresTotales) {
            ejemplaresDisponibles++;
        }
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitulo() {
        return titulo;
    }

    public int getEjemplaresDisponibles() {
        return ejemplaresDisponibles;
    }

    public String toCSV() {
        int ejemplaresPrestados = ejemplaresTotales - ejemplaresDisponibles;
        return String.format("%s,%s,%s,%d,%d",
                isbn, titulo, autor, ejemplaresTotales, ejemplaresPrestados);
    }
}
