#**************************************************************
#         		Pontificia Universidad Javeriana
#     Autor: Sergio Ortiz
#     Fecha: 24 de agosto 2024
#     Materia: Sistemas Operativos
#     Tema: Laboratorio de paralelismo
#     Fichero: Makefile 
#****************************************************************/
#Compilador a usar
GCC = gcc
#Flags de compilación comunes
CFLAGS = -lm			#-lm enlaza la librería matemática (math.h)
FOPENMP = -fopenmp -O3  #-fopenmp habilita OpenMP, -03 activa optimización de alto nivel
POSIX = -lpthread		#-lpthread enlaza la librería POSIX threads (hilos)
#Archivo de módulo auxiliar
modulo = inter.c
#Programa que se va a generar
PROGRAMAS = mmClasicaOpenMP 
#Regla principal: compilar todos los programas
ALL: $(PROGRAMAS)

#Regla para compilar la versión clásica con OpenMP
#Toma el archivo fuente mmClasicaOpenMP.c y lo compila con el compilador gcc y las banderas de OpenMP
mmClasicaOpenMP:
	$(GCC) $(CLFAGS) $@.c -o $@ $(FOPENMP)
# Regla para limpiar: elimina los binarios generados
# con el comando "make clean"
clean:
	$(RM) $(PROGRAMAS) 
