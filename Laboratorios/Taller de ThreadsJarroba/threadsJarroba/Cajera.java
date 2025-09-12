package threadsJarroba;  // Paquete donde reside esta clase

/*
Taller 3
Clase: Sistemas distribuidos
Autores: Ana Sofía Grass, Sebastián Vargas, Sergio Ortiz, Isabela Palacio
*/

public class Cajera { // Clase que modela a una cajera que procesa compras

	private String nombre; // Nombre identificador de la cajera

	public Cajera() { // Constructor por defecto
	}

	public Cajera(String nombre) { // Constructor que inicializa el nombre de la cajera
		this.nombre = nombre; // Asigna el nombre recibido al atributo
	}

	public String getNombre() { // Devuelve el nombre de la cajera
		return nombre; // Retorna el valor del atributo nombre
	}

	public void setNombre(String nombre) { // Actualiza el nombre de la cajera
		this.nombre = nombre; // Guarda el nuevo nombre
	}

	public void procesarCompra(Cliente cliente, long timeStamp) { // Procesa la compra del cliente desde un tiempo inicial de referencia

		System.out.println("La cajera " + this.nombre + 
				" COMIENZA A PROCESAR LA COMPRA DEL CLIENTE " + cliente.getNombre() + 
				" EN EL TIEMPO: " + (System.currentTimeMillis() - timeStamp) / 1000	+
				"seg"); // Mensaje inicial con tiempo transcurrido (segundos)

		for (int i = 0; i < cliente.getCarroCompra().length; i++) { // Itera por cada producto del carro del cliente
			this.esperarXsegundos(cliente.getCarroCompra()[i]); // Simula el tiempo de atención del producto i
			System.out.println("Procesado el producto " + (i + 1) + 
					" ->Tiempo: " + (System.currentTimeMillis() - timeStamp) / 1000 + 
					"seg"); // Imprime el tiempo transcurrido tras procesar el producto
		}

		System.out.println("La cajera " + this.nombre + " HA TERMINADO DE PROCESAR " + 
						cliente.getNombre() + " EN EL TIEMPO: " + 
						(System.currentTimeMillis() - timeStamp) / 1000 + "seg"); // Mensaje final con el tiempo total

	}

	private void esperarXsegundos(int segundos) { // Pausa la ejecución del hilo actual
		try { // Puede ser interrumpido mientras duerme
			Thread.sleep(segundos * 1000); // Duerme el hilo la cantidad de milisegundos equivalente a 'segundos'
		} catch (InterruptedException ex) { // Si el hilo fue interrumpido
			Thread.currentThread().interrupt(); // Restablece el estado de interrupción del hilo
		}
	}

}
