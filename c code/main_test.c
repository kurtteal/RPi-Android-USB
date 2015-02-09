#include "usbmodule.h"


int main (int argc, char *argv[]){

    int initReturn;
    tData *tdata1 = NULL; //Not needed, just here for awareness
    tData *tdata2;
    int rc = 0;
    char input[50];
    int counter = 0;
    int tid1 = 0, tid2 = 1;

    //signal(SIGINT, sigintHandler);
    initReturn = UsbInit();

    if(initReturn != 4) //non-succesful init
        return 0;

    //Initializing mutex
    if (pthread_mutex_init(&lock, NULL) != 0){printf("\n Mutex init failed\n");return 1;}

    //KeepAliveThread
    t1 = 0;
    rc = pthread_create(&thread1, NULL, KeepAliveThread, (void *)tdata1);
    if (rc){
         printf("ERROR; return code from pthread_create() is %d\n", rc);
         exit(-1);
    }

    //input cycle... will simulate the coordinates from the RPi
    while(1){

    	//Just type in anything to send the string...
        scanf("%s", input);
        if(strcmp(input, "quit") == 0)
            break;

        //A thread is created for each individual transfer
        sprintf(input, "%d %s", counter++, "something something the coordinates of the dark side");
        buildThreadData(&tdata2, tid2, input);
        t2 = 0;
        rc = pthread_create(&thread2, NULL, InfoThread, (void *)tdata2);
        if (rc){
             printf("ERROR; return code from pthread_create() is %d\n", rc);
             exit(-1);
        }
    }
    //Prepare to leave

    printf("Terminating threads...\n");
    //See if the threads are running, and cancel them 

    pthread_cancel(thread1);
    pthread_join(thread1, NULL);

    pthread_cancel(thread2);
    pthread_join(thread2, NULL);


    pthread_mutex_destroy(&lock);

	UsbDeInit();
    printf("Exiting the program\n");
	return 0;
}
