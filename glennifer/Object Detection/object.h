#include "coord.h"

#ifndef OBJECT_H
#define OBJECT_H
struct object
{
	struct coord* coord_list;

	struct object* next;
	struct object* prev;
};

struct object* find_objects (char** topo_map, int width, int height);
void gen_object (struct object** object_list, char** topo_map, char** mark_map, int x, int y, int width, int height);
void gen_coord_list (struct coord** coord_list, char** topo_map, char** mark_map, int x, int y, int width, int height);
#endif
