all: object.o topo.o
object.o: object.c object.h coord.h
	gcc -c object.c
topo.o: topo.c topo.h coord.h
	gcc -c topo.c
clean:
	rm object.o topo.o
