## ThreadsJarroba

### Descripción
Ejemplos de concurrencia en Java que simulan la atención de clientes por parte de cajeras, tanto de forma secuencial como usando hilos (`Thread`) y la interfaz `Runnable`.

### Estructura
- `threadsJarroba/Cliente.java`: Modelo de cliente con su nombre y tiempos por producto.
- `threadsJarroba/Cajera.java`: Lógica de atención secuencial de una cajera a un cliente.
- `threadsJarroba/CajeraThread.java`: Variante de cajera que extiende `Thread` y atiende a un cliente en un hilo propio.
- `threadsJarroba/Main.java`: Ejecución secuencial (sin hilos).
- `threadsJarroba/MainThread.java`: Ejecución concurrente creando hilos que extienden `Thread`.
- `threadsJarroba/MainRunnable.java`: Ejecución concurrente creando hilos con tareas `Runnable`.

### Requisitos
- Java 8 o superior (JDK instalado y en el PATH).

### Compilación y ejecución (Windows PowerShell)
1) Posiciónate en la carpeta fuente (source root):
```powershell
cd "C:\Users\anaso\OneDrive\Escritorio\sistemas-distribuidos-\Primer Corte\ThreadsJarroba"
```
2) Compila todas las clases del paquete:
```powershell
javac .\threadsJarroba\*.java
```
3) Ejecuta el modo deseado:
```powershell
# Secuencial
java -cp . threadsJarroba.Main

# Concurrente con Thread
java -cp . threadsJarroba.MainThread

# Concurrente con Runnable
java -cp . threadsJarroba.MainRunnable
```

### Notas sobre paquetes (package) y Source Root
- La primera línea de cada archivo debe ser: `package threadsJarroba;`
- La compilación/ejecución debe realizarse desde esta carpeta: `.../Primer Corte/ThreadsJarroba` para que la ruta física coincida con el paquete.
- Si tu IDE muestra errores de paquete no coincidente, marca esta carpeta como "Sources Root" (IntelliJ: clic derecho → Mark Directory as → Sources Root; VS Code Java: define `"java.project.sourcePaths": ["Primer Corte/ThreadsJarroba"]`).

### Quitar BOM UTF-8 (si ves `illegal character: '\ufeff'`)
Reescribe los archivos `.java` sin BOM:
```powershell
Get-ChildItem .\threadsJarroba\*.java | % {
  $t = Get-Content $_.FullName -Raw
  [IO.File]::WriteAllText($_.FullName, $t, (New-Object Text.UTF8Encoding $false))
}
```

### Clases principales (resumen)
- `Cliente`: contiene `nombre` y `carroCompra` (arreglo de segundos por producto).
- `Cajera`: procesa secuencialmente los productos del `Cliente` e imprime tiempos.
- `CajeraThread`: hace lo mismo que `Cajera`, pero en un hilo independiente.
- `Main`: ejemplo secuencial.
- `MainThread`: lanza dos `CajeraThread` para procesar en paralelo.
- `MainRunnable`: lanza dos hilos con instancias `Runnable` que delegan en `Cajera`.

### Problemas comunes y soluciones
- "declared package ... does not match expected": compila/ejecuta desde `Primer Corte/ThreadsJarroba` o ajusta el Source Root.
- `ClassNotFoundException`: asegúrate de usar `-cp .` y el nombre calificado `threadsJarroba.Algo`.
- `illegal character: '\ufeff'`: elimina BOM con el script anterior.

### Créditos
- Taller 3 · Sistemas distribuidos.
- Autores: Ana Sofía Grass, Sebastián Vargas, Sergio Ortiz, Isabela Palacio.
