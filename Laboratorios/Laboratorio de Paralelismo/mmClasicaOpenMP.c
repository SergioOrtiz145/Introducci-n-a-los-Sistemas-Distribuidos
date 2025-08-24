/*#######################################################################################
 #* Fecha: 15 de agosto del 2025
 #* Autor: Sergio Ortiz 
 #* Tema: 
 #* 	- Programa Multiplicación de Matrices algoritmo clásico
 #* 	- Paralelismo con OpenM:
######################################################################################*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <sys/time.h>
#include <omp.h>   // Librería de OpenMP

// Variables globales para medir tiempo
struct timeval inicio, fin; 

// La función tiene como proposito guardar el instante de tiempo inicial.
void InicioMuestra() {
    gettimeofday(&inicio, (void *)0);
}

// La función tiene como proposito guardar el instante de tiempo final, calcular y mostrar el tiempo total de ejecución en microsegundos.
void FinMuestra() {
    gettimeofday(&fin, (void *)0);
    fin.tv_usec -= inicio.tv_usec;
    fin.tv_sec  -= inicio.tv_sec;
    double tiempo = (double) (fin.tv_sec*1000000 + fin.tv_usec); 
    printf("%9.0f \n", tiempo);
}

// La función tiene como proposito Imprimir una matriz en pantalla si su tamaño es relativamente pequeño (D < 9).
void impMatrix(double *matrix, int D) {
    printf("\n");
    if(D < 9) {  // Solo imprime matrices pequeñas
        for(int i=0; i<D*D; i++) {
            if(i % D == 0) printf("\n");
            printf("%f ", matrix[i]);
        }
        printf("\n**-----------------------------**\n");
    }
}

// La función tiene como proposito Inicializar las matrices A y B con valores aleatorios entre 0 y 99.
void iniMatrix(double *m1, double *m2, int D) {
    for(int i=0; i<D*D; i++, m1++, m2++) {
        *m1 = rand() % 100;	
        *m2 = rand() % 100;	
    }
}

// La función tiene como proposito multiplicar dos matrices cuadradas de tamaño D usando el algoritmo clásico. Se paraleliza con OpenMP.
void multiMatrix(double *mA, double *mB, double *mC, int D) {
    double Suma, *pA, *pB;

    #pragma omp parallel   // Región paralela
    {
        #pragma omp for    // Distribuir las iteraciones del for entre hilos
        for(int i=0; i<D; i++) {
            for(int j=0; j<D; j++) {
                pA = mA + i*D;   // Fila i de A
                pB = mB + j;     // Columna j de B 
                Suma = 0.0;
                for(int k=0; k<D; k++, pA++, pB+=D) {
                    Suma += *pA * *pB;
                }
                mC[i*D + j] = Suma;
            }
        }
    }
}

// Función principal
int main(int argc, char *argv[]) {
    // Validar parámetros de entrada
    if(argc < 3) {
        printf("\n Uso: $./clasicaOpenMP SIZE Hilos \n\n");
        exit(0);
    }

    // Lectura de argumentos
    int N = atoi(argv[1]);   // Tamaño de la matriz NxN
    int TH = atoi(argv[2]);  // Número de hilos a usar en OpenMP

    // Reserva de memoria dinámica para las matrices
    double *matrixA  = (double *)calloc(N*N, sizeof(double));
    double *matrixB  = (double *)calloc(N*N, sizeof(double));
    double *matrixC  = (double *)calloc(N*N, sizeof(double));

    // Inicializar generador de números aleatorios
    srand(time(NULL));

    // Configurar número de hilos en OpenMP
    omp_set_num_threads(TH);

    // Inicializar matrices A y B con valores aleatorios
    iniMatrix(matrixA, matrixB, N);

    // Imprimir matrices si son pequeñas
    impMatrix(matrixA, N);
    impMatrix(matrixB, N);

    // Multiplicación y medición de tiempo
    InicioMuestra();
    multiMatrix(matrixA, matrixB, matrixC, N);
    FinMuestra();

    // Imprimir resultado si la matriz es pequeña
    impMatrix(matrixC, N);

    // Liberar memoria reservada
    free(matrixA);
    free(matrixB);
    free(matrixC);
    
    return 0;
}
