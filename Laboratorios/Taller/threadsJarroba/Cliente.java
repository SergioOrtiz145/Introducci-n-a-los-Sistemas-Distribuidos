package threadsJarroba; // Paquete del ejemplo

/*
Taller 3
Clase: Sistemas distribuidos
Autores: Ana Sofía Grass, Sebastián Vargas, Sergio Ortiz, Isabela Palacio
*/



public class Cliente { // Clase que representa a un cliente con su carro de compras

	private String nombre; // Nombre del cliente
	private int[] carroCompra; // Arreglo con los tiempos (segundos) por producto

	public Cliente() { // Constructor por defecto
	}

	public Cliente(String nombre, int[] carroCompra) { // Constructor con parámetros
		this.nombre = nombre; // Asigna el nombre
		this.carroCompra = carroCompra; // Asigna el carro de compras
	}

	public String getNombre() { // Getter del nombre
		return nombre; // Retorna el nombre del cliente
	}

	public void setNombre(String nombre) { // Setter del nombre
		this.nombre = nombre; // Actualiza el nombre del cliente
	}

	public int[] getCarroCompra() { // Getter del carro de compras
		return carroCompra; // Retorna el arreglo de tiempos por producto
	}

	public void setCarroCompra(int[] carroCompra) { // Setter del carro de compras
		this.carroCompra = carroCompra; // Actualiza el arreglo de tiempos
	}

}
