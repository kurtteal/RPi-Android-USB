#include "threads.h"

//Receives the address of a variable that stores a pointer to the struct 
//and allocates and populates the struct that will be passed to the thread
//the strut holds the parameters for the thread to chew
void buildThreadData(tData **td, int tid, char *msg){
        (*td) = (tData *)malloc(sizeof(tData));
        (*td)->tid = tid;
        (*td)->message = (char *)malloc((strlen(msg)+1)*sizeof(char));
        strcpy((*td)->message, msg);
}

