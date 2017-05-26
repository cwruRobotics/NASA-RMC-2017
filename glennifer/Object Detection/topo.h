#include "coord.h"

#ifndef TOPO_H
#define TOPO_H
char** gen_topo_map (struct three_d_coord* point_cloud, int width, int height, int up_cutoff, int down_cutoff);
void interpolate (int** two_d_map, int width, int height);
#endif
