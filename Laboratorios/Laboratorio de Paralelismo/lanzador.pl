#!/usr/bin/perl
#**************************************************************
#         		Pontificia Universidad Javeriana
#     Autor: Sergio Ortiz
#     Fecha: 24 de agosto 2024
#     Materia: Sistemas Operativos
#     Tema: Laboratorio de paralelismo
#     Fichero: script automatización ejecución por lotes 
#****************************************************************/
# Obtiene la ruta absoluta del directorio actual y elimina el salto de línea
$Path = `pwd`;
chomp($Path);

# Nombre del ejecutable que se va a correr sin extensiones ni parametros
$Nombre_Ejecutable = "mmClasicaOpenMP";

# Lista de tamaños de matrices a probar
@Size_Matriz = ("480", "880", "1760", "2880", "3360", "5040", "6320", "8800", "10320", "11200", "12400", "13200");

# Lista con el número de hilos a usar en la ejecución
@Num_Hilos = (1, 2, 4, 8, 16, 20);

# Número de repeticiones por cada configuración
$Repeticiones = 30;

# Bucle principal: recorre todos los tamaños de matriz definidos
foreach $size (@Size_Matriz) {
	
	# Para cada tamaño, prueba con diferentes números de hilos
	foreach $hilo (@Num_Hilos) {
		
		# Define el nombre del archivo de salida para esta configuración
		$file = "$Path/$Nombre_Ejecutable-" . $size . "-Hilos-" . $hilo . ".dat";
		
		# Ejecuta el programa repetidamente (30 veces)
		for ($i=0; $i<$Repeticiones; $i++) {
			
			# Llama al ejecutable con parámetros (tamaño, hilos)
			# y redirige la salida estándar al archivo correspondiente
			system("$Path/$Nombre_Ejecutable $size $hilo  >> $file");
			
			# Imprime en pantalla el comando que se está ejecutando (para monitoreo)
			printf("$Path/$Nombre_Ejecutable $size $hilo \n");
		}
		
		# Cierra el archivo .dat de la configuración
		close($file);
		
		# Contador de ejecuciones totales
		$p = $p + 1;
	}
}
