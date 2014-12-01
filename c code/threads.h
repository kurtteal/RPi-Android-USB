#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#define NUM_THREADS 1

typedef struct thread_data{
   int  tid;
   char *message;
} tData;


//void *WorkerThreadFunction(void *msgStruct); //defining this is the responsibility of who calls the library
//int startThread(pthread_t *thread, tData **td, int tid, char *msg); //this thread will use the previously defined function

/* ================================= */
void buildThreadData(tData **td, int tid, char *msg); //creates the struct that holds the params for the thread
/* Example of usage:
/*
    tData *tdata;

    buildThreadData(&tdata, tid, message);
    rc = pthread_create(&thread, NULL, KeepAliveThread, (void *)tdata);
    if (rc){
         printf("ERROR; return code from pthread_create() is %d\n", rc);
         exit(-1);
    }

    ... then inside the function executed by the thread:

    void *CoordinatesThread(void *msgStruct){

        tData *data = (tData*)msgStruct;
        sprintf(buffer, "%s", data->message);
*/
