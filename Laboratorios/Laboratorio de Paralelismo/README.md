# Laboratorio de Paralelismo
---
## Algoritmo mmClasicaOpenMP
**El programa mmClasicaOpenMP implementa la multiplicación clásica de matrices en el lenguaje C, utilizando la librería OpenMP
para aprovechar el paralelismo en arquitecturas con múltiples núcleos de CPU.**

### ¿Qué hace?
- Multiplica dos matrices cuadradas A y B para obtener una matriz resultante C
- Usa el algoritmo clásico de complejidad O(n³), basado en tres bucles anidados.
- Integra OpenMP para dividir el trabajo entre varios hilos de ejecución, acelerando el cálculo en equipos multicore.
- Si el tamaño de la matriz es menor que 9, imprimira en pantalla los valores de las matrices A y B, junto a la matriz
C y su tiempo de total de ejecución en microsegundos.
- Si el tamaño de la matriz es mayor o igual que 9, solo imprimira el tiempo total de ejecución.

### Beneficios
- Aprovecha mejor los procesadores modernos.
- Reduce el tiempo de cómputo frente a la versión secuencial.

### Usos
- Comparar rendimiento entre la versión secuencial y la versión paralela.
- Analizar el impacto del paralelismo en operaciones matemáticas intensivas.
- Aplicable en áreas como álgebra lineal, simulaciones científicas, gráficos y machine learning.

---

## Script lanzador.pl
**El script se usa para automatizar la ejecución por lotes del programa mmClasicaOpenMP. Tiene como fin facilitar las pruebas
experimentales de rendimiento con diferentes configuraciones de tamaño de matriz y número de hilos.**

### ¿Qué hace?
- Ejecuta el programa mmClasicaOpenMP con distintos tamaños de matrices y diferentes cantidades de hilos de ejecución.
- Cada configuración se repite un número definido de veces (30 veces idealmente) para obtener datos más consistentes.
- Almacena los resultados en archivos .dat nombrados automáticamente según el tamaño de matriz y el número de hilos.

### Proposito
- Automatizar pruebas sin necesidad de ejecutar manualmente cada configuración.
- Recolectar datos de rendimiento de una forma estructurada para posteriormente realizar un análisis.

---

## Makefile
**Esta diseñado para automatizar la compilación del programa mmClasicaOpenMP.**

## Plan de pruebas 
**Para la correcta realización del pruebas se eligieron diferentes tamaños de matrices: 480, 880, 1760, 2880, 3360, 5040 y 6320, todos divisibles en 80, ya que es mínimo común múltiplo de los hilos 1, 2, 4, 8, 16, 20.
Brindando beneficios como comparaciones más limpias, balance de carga perfecto y resultados más consistentes entre ejecuciones.
Se eligió 30 ejecuciones de cada matriz D con cada hilo porque se busca aplicar el Teorema Central del Límite, la media de los tiempos se aproxima a una distribución normal.
Esto nos ayuda a la normalización de datos, la seguridad de comparar promedios entre hilos y reduce el sesgo por ruidos y cargas alternas de SO, brindando resultados más estables.**

**Luego de realizar las pruebas con los anteriores parámetros, cada uno de los resultados de cada matriz D por hilo fue guardado dentro de un archivo .dat, que se encontrarán dentro de la carpeta [Datos](https://github.com/SergioOrtiz145/Introducci-n-a-los-Sistemas-Distribuidos/tree/main/Laboratorios/Laboratorio%20de%20Paralelismo/Datos).**

---

## Ejecución del laboratorio
### 1. Compilar el programa
Desde la terminal, estando en la carpeta del proyecto:
```bash
make
```
Generará el ejecutable:
```bash
mmClasicaOpenMP
```
### 2. Ejecutar el programa manualmente
El programa recibe dos parámetros:
- Tamaño de la matriz (N)
- Número de hilos (TH)
Ejemplo:
```bash
./mmClasicaOpenMP 880 4
```
### 3. Automatizar pruebas por lotes
Para no ejecutar manualmente cada configuración, se puede usar el script en Pearl:
```bash
perl lanzador.pearl
```
- Corre el programa con varios tamaños de matrices
- Usa diferentes números de hilos
- Guarda los resultados en archivos .dat
Ejemplo:
```bash
mmClasicaOpenMP-<Tamaño>-Hilos-<NumHilos>.dat
```
### 4. Limpiar compilaciones
Para borrar el ejecutable generado:
```bash
make Clean
```

