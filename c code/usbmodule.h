#include "threads.h"
#include <signal.h> //for the sig_atomic variables

pthread_mutex_t lock; //access control to the usb sockets by the threads
sig_atomic_t t1, t2; //atomic (no need for mutexes...) flags that indicate if these threads are running (1 == running)
                     //actually we don't need these variables either, its fine to call cancel and join on already terminated threads
pthread_t thread1, thread2;


int UsbInit();
void *InfoThread(void *msgStruct);
void *KeepAliveThread(void *not_used);
int UsbDeInit();
