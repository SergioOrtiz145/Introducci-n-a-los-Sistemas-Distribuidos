package threadsJarroba; // Paquete del ejemplo

/*
Taller 3
Clase: Sistemas distribuidos
Autores: Ana Sofía Grass, Sebastián Vargas, Sergio Ortiz, Isabela Palacio
*/

public class MainRunnable implements Runnable{ // Uso de hilos implementando Runnable
	
	private Cliente cliente; // Cliente a ser atendido
	private Cajera cajera; // Cajera que atiende al cliente
	private long initialTime; // Tiempo inicial de referencia
	
	public MainRunnable (Cliente cliente, Cajera cajera, long initialTime){ // Constructor con dependencias
		this.cajera = cajera; // Asigna la cajera
		this.cliente = cliente; // Asigna el cliente
		this.initialTime = initialTime; // Asigna el tiempo inicial
	}

	public static void main(String[] args) { // Punto de entrada: crea y lanza hilos
		
		Cliente cliente1 = new Cliente("Cliente 1", new int[] { 2, 2, 1, 5, 2, 3 }); // Cliente 1
		Cliente cliente2 = new Cliente("Cliente 2", new int[] { 1, 3, 5, 1, 1 }); // Cliente 2
		
		Cajera cajera1 = new Cajera("Cajera 1"); // Cajera 1
		Cajera cajera2 = new Cajera("Cajera 2"); // Cajera 2
		
		// Tiempo inicial de referencia
		long initialTime = System.currentTimeMillis(); // Tiempo base (ms)
		
		Runnable proceso1 = new MainRunnable(cliente1, cajera1, initialTime); // Tarea para cliente1/cajera1
		Runnable proceso2 = new MainRunnable(cliente2, cajera2, initialTime); // Tarea para cliente2/cajera2
		
		new Thread(proceso1).start(); // Inicia hilo 1
		new Thread(proceso2).start(); // Inicia hilo 2

	}

	@Override
	public void run() { // Lógica que ejecuta cada hilo
		this.cajera.procesarCompra(this.cliente, this.initialTime); // Delegación del procesamiento a Cajera
	}

}
