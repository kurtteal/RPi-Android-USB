#include "usbmodule.h"

int main (int argc, char *argv[]){

    int initReturn;
    tData *tdata1 = NULL; //Not needed, just here for awareness
    tData *tdata2;
    int rc = 0;
    char input[50];
    int counter = 0;
    int tid1 = 0, tid2 = 1;

    int *t1_on, *t2_on;

    //signal(SIGINT, sigintHandler);
    initReturn = UsbInit();

    if(initReturn != 4) //non-succesful init
        return 0;

    //Initializing mutexes
    if (pthread_mutex_init(&lock, NULL) != 0){printf("\n Mutex init failed\n");return 1;}
    if (pthread_mutex_init(&lock_t1, NULL) != 0){printf("\n Mutex init failed\n");return 1;}
    if (pthread_mutex_init(&lock_t2, NULL) != 0){printf("\n Mutex init failed\n");return 1;}

    //KeepAliveThread
    rc = pthread_create(&thread1, NULL, KeepAliveThread, (void *)tdata1);
    if (rc){
         printf("ERROR; return code from pthread_create() is %d\n", rc);
         exit(-1);
    }

    //input cycle... will simulate the coordinates from the RPi
    while(1){

        scanf("%s", input);
        if(strcmp(input, "quit") == 0)
            break;

        //A thread is created for each individual transfer
        sprintf(input, "%d %s", counter++, "something something the coordinates of the dark side");
        buildThreadData(&tdata2, tid2, input);
        rc = pthread_create(&thread2, NULL, InfoThread, (void *)tdata2);
        if (rc){
             printf("ERROR; return code from pthread_create() is %d\n", rc);
             exit(-1);
        }
    }
    //Prepare to leave

    printf("Terminating threads...\n");
    //See if the threads are running, and cancel them
    pthread_mutex_lock(&lock_t1);
    *t1_on = t1;
    pthread_mutex_unlock(&lock_t1); 
    if(*t1_on) pthread_cancel(thread1);
        
    pthread_mutex_lock(&lock_t2);
    *t2_on = t2;
    pthread_mutex_unlock(&lock_t2);
    if(*t2_on) pthread_cancel(thread2);
    
    pthread_mutex_destroy(&lock);
    pthread_mutex_destroy(&lock_t1);
    pthread_mutex_destroy(&lock_t2);

	UsbDeInit();
	return 0;
}
