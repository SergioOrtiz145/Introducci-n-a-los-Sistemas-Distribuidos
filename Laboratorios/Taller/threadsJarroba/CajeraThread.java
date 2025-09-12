package threadsJarroba; // Paquete del ejemplo

/*
Taller 3
Clase: Sistemas distribuidos
Autores: Ana Sofía Grass, Sebastián Vargas, Sergio Ortiz, Isabela Palacio
*/

public class CajeraThread extends Thread { // Hilo que modela a una cajera atendiendo a un cliente

	private String nombre; // Nombre de la cajera

	private Cliente cliente; // Cliente que atiende este hilo

	private long initialTime; // Tiempo inicial de referencia


	public CajeraThread() { // Constructor por defecto
	}

	public CajeraThread(String nombre, Cliente cliente, long initialTime) { // Constructor con parámetros
		this.nombre = nombre; // Asigna nombre
		this.cliente = cliente; // Asigna cliente
		this.initialTime = initialTime; // Asigna tiempo inicial
	}

	public String getNombre() { // Getter del nombre
		return nombre; // Retorna nombre actual
	}

	public void setNombre(String nombre) { // Setter del nombre
		this.nombre = nombre; // Actualiza nombre
	}

	public long getInitialTime() { // Getter del tiempo inicial
		return initialTime; // Retorna tiempo inicial
	}

	public void setInitialTime(long initialTime) { // Setter del tiempo inicial
		this.initialTime = initialTime; // Actualiza tiempo inicial
	}

	public Cliente getCliente() { // Getter del cliente
		return cliente; // Retorna cliente
	}

	public void setCliente(Cliente cliente) { // Setter del cliente
		this.cliente = cliente; // Actualiza cliente
	}

	@Override
	public void run() { // Lógica que ejecuta el hilo

		System.out.println("La cajera " + this.nombre + " COMIENZA A PROCESAR LA COMPRA DEL CLIENTE " 
				+ this.cliente.getNombre() + " EN EL TIEMPO: " 
				+ (System.currentTimeMillis() - this.initialTime) / 1000 
				+ "seg"); // Imprime inicio del procesamiento y tiempo transcurrido (seg)

		for (int i = 0; i < this.cliente.getCarroCompra().length; i++) { // Itera por cada producto en el carro
			// Se procesa el pedido en X segundos
			this.esperarXsegundos(cliente.getCarroCompra()[i]); // Simula el tiempo de proceso del producto i
			System.out.println("Procesado el producto " + (i + 1) 
					+ " del cliente " + this.cliente.getNombre() + "->Tiempo: " 
					+ (System.currentTimeMillis() - this.initialTime) / 1000 
					+ "seg"); // Imprime avance y tiempo transcurrido tras cada producto
		}

		System.out.println("La cajera " + this.nombre + " HA TERMINADO DE PROCESAR " 
					+ this.cliente.getNombre() + " EN EL TIEMPO: " 
					+ (System.currentTimeMillis() - this.initialTime) / 1000 
					+ "seg"); // Imprime el tiempo total al finalizar
	}

	private void esperarXsegundos(int segundos) { // Pausa el hilo actual
		try { // Puede ser interrumpido
			Thread.sleep(segundos * 1000); // Duerme el hilo: segundos -> milisegundos
		} catch (InterruptedException ex) { // Si el hilo es interrumpido
			Thread.currentThread().interrupt(); // Restablece el estado de interrupción
		}
	}

}
