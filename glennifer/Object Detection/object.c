#include "object.h"

#include <stdlib.h>

void gen_coord_list (struct coord** coord_list, char** topo_map, char** mark_map, int x, int y, int width, int height)
{
	struct coord* new_coord = (struct coord*)malloc (sizeof (struct coord));

	//Fill the new point with data
	new_coord->x = x;
	new_coord->y = y;
	new_coord->next = NULL;
	new_coord->prev = NULL;

	//Add the point to the coordinate list
	if (!(*coord_list))
		*coord_list = new_coord;
	else
	{
		new_coord->next = *coord_list;
		(*coord_list)->prev = new_coord;
		*coord_list = new_coord;
	}

	//Mark this point as already used
	mark_map [x][y] = 1;

	//Recurse
	if (x - 1 > 0 && topo_map [x-1][y] == topo_map [x][y] && !mark_map [x-1][y])
		gen_coord_list (coord_list, topo_map, mark_map, x-1, y, width, height);
	if (x + 1 < width && topo_map [x+1][y] == topo_map [x][y] && !mark_map [x+1][y])
		gen_coord_list (coord_list, topo_map, mark_map, x+1, y, width, height);
	if (y - 1 > 0 && topo_map [x][y-1] == topo_map [x][y] && !mark_map [x][y-1])
		gen_coord_list (coord_list, topo_map, mark_map, x, y-1, width, height);
	if (y + 1 < height && topo_map [x][y+1] == topo_map [x][y] && !mark_map [x][y+1])
		gen_coord_list (coord_list, topo_map, mark_map, x, y+1, width, height);
}

void gen_object (struct object** object_list, char** topo_map, char** mark_map, int x, int y, int width, int height)
{
	struct object* new_object = (struct object*)malloc (sizeof (struct object));

	//Initialize new object
	new_object->next = NULL;
	new_object->prev = NULL;
	new_object->coord_list = NULL;

	//Get all the points inside the object
	gen_coord_list (&(new_object->coord_list), topo_map, mark_map, x, y, width, height);

	//Add object to the object list
	if (!(*object_list))
		*object_list = new_object;
	else
	{
		new_object->next = *object_list;
		(*object_list)->prev = new_object;
		*object_list = new_object;
	}
}

struct object* find_objects (char** topo_map, int width, int height)
{
	char** mark_map = (char**)malloc (width * sizeof (char*));
	int i;
	int j;
	struct object* ret;

	//Create and initialize the mark map
	for (i = 0; i < width; i ++)
	{
		mark_map [i] = (char*)malloc (height * sizeof (char));

		for (j = 0; j < height; j ++)
			mark_map [i][j] = 0;
	}

	for (i = 0; i < width; i ++)
	{
		for (j = 0; j < height; j ++)
		{
			//If there's something here and we haven't already added it to the object list...
			if (topo_map [i][j] && !mark_map [i][j])
				gen_object (&ret, topo_map, mark_map, i, j, width, height);				
		}
	}

	return ret;
}
