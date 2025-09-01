import java.io.Serializable;

//Clase que representa un libro dentro la biblioteca
//Se usa como modelo de datos 
public class Libro implements Serializable {
  //identificador para serialización
  private static final long serialVersionUID = 1L;

  private String isbn;
  private String titulo;
  private int cantidad;

  public Libro() {}

  public Libro(String isbn, String titulo, int cantidad) {
    this.isbn = isbn;
    this.titulo = titulo;
    this.cantidad = cantidad;
  }

  public String getIsbn() { return isbn; }
  public void setIsbn(String isbn) { this.isbn = isbn; }
  public String getTitulo() { return titulo; }
  public void setTitulo(String titulo) { this.titulo = titulo; }
  public int getCantidad() { return cantidad; }
  public void setCantidad(int cantidad) { this.cantidad = cantidad; }

  //Rpresentación en texto del libro.
  @Override public String toString() {
    return "Libro{isbn='" + isbn + "', titulo='" + titulo + "', cantidad=" + cantidad + "}";
  }
}
