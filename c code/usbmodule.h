#include "threads.h"

pthread_mutex_t lock; //access control to the usb sockets by the threads
pthread_mutex_t lock_t1; //locks for the flags
pthread_mutex_t lock_t2;
int t1, t2; //flags that indicate if these threads are running
pthread_t thread1, thread2;

int UsbInit();
void *InfoThread(void *msgStruct);
void *KeepAliveThread(void *not_used);
int UsbDeInit();
