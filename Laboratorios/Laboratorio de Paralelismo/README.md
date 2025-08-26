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

## Análisis de resultados
### Matriz 480x480
La matriz de 480 refleja en la grafica de promedio de tiempo por hilo que el tiempo de ejecución disminuye consistentemente conforme aumentan los hilos. Notando que con 1 hilo este tarda 67.000 ms, y con 20 hilos baja hasta 10.000 ms, significando una reducción de casi 6 veces entre el peor y el mejor caso. El speedup (médir cuánto más rápido ejecuta un algoritmo) empieza a disminuir desde los 16 hilos.
El speedup es notable hata los 8 hilos, pero después de 16 hilos estos beneficios se ven reducidos.
Para verificar que los datos son constantes y confiables, se puede ver que las desviaciones estándar son muy bajas en todos los casos, con un máximo de 3139.
Podemos concluir que el mejor hilo para realizar la múltiplicación es con 20 hilos por sus cortos tiempos de ejecución.

<img width="550" height="326" alt="image" src="https://github.com/user-attachments/assets/6f6bd297-38ff-44fd-b72f-7178b647d4fd" />

### Matriz de 880
La tendencia de la matriz es que a medida que se aumentan los hilos, el tiempo baja progresivamente hasta 74.000 ms con 20 hilos, teniendo en cuenta que con 1 hilo, el cálculo toma 418.000 ms, una reducción de 5.6 veces entre el hilo 1 y 20 hilos. El speedup en esta matriz es parecida a matriz de 480, es notable el crecimiento hasta 8, luego en 16 en adelante disminuye. el speedup muestra tres fases distintas: la primera fase muestra alta eficiencia entre 1 a 4 hilos donde habria un 92% de eficiencia, la segunda fase entre 4 a 8 hilos con una eficiencia moderada del 59% y por ultimo, una fase de saturación entre 8 a 20 hilos donde la eficiencia cae hasta el 28%.
Los datos de desviación estándar son de igual forma muy bajos entre 1840 y 5801, resultando en datos estables y repetibles.
Como conclusión de la matriz, la configuración de 8 hilos seria la mejor opción con este algoritmo, ya que ofrece el mejor balance costo-beneficio porque tiene un buen rendimiento, un speedup de 4.73x y mejor estabilidad con la menor desviación estándar entre hilos.

<img width="544" height="325" alt="image" src="https://github.com/user-attachments/assets/142c3cc8-db44-4137-b60a-864b59014451" />

### Matriz de 1760




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

