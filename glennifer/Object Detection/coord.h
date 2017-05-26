#ifndef COORD_H
#define COORD_H
struct coord
{
	int x;
	int y;

	struct coord* next;
	struct coord* prev;
};

struct three_d_coord
{
	int x;
	int y;
	int z;

	struct three_d_coord* next;
	struct three_d_coord* prev;
};
#endif
