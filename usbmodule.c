//Install libusb:
//sudo apt-get install libusb-dev
//sudo apt-get install libusb-1.0-0-dev
//To check VID and PID of device: 'lsusb' (same command to get the VID and PID of device in accessory mode)
//To check the IN and OUT endpoints for the accessory, run (after device in accessory mode): 'lsusb -v' 
//	(the only endpoints should be bulk due to android accessory protocol)
//To compile: gcc usbmodule.c -I/usr/include/ -o usbmodule -lusb-1.0 -I/usr/include/ -I/usr/include/libusb-1.0

//Thanks go to Manuel di Cerbo, for an example

#include <stdio.h>
#include <usb.h>
#include <libusb.h>
#include <string.h>
#include <unistd.h>


#define IN 0x81	//Accessory
#define OUT 0x02 //Accessory
//#define IN 0x82	//Accessory Samsung
//#define OUT 0x04 //Accessory

#define VID 0x18D1 //Google
#define PID 0x4EE1 //Nexus 5
//#define VID 0x22B8 //Motorola
//#define PID 0x2E82 //Moto G
//#define VID 0x04e8 //Samsung
//#define PID 0x6860 

#define ACCESSORY_PID 0x2D01 // Used when usb debug activated
#define ACCESSORY_PID_ALT 0x2D00 //Used generally
#define ACCESSORY_VID 0x18D1 //Google

#define LEN 2

static int mainPhase();
static int mainPhaseOut();
static int init(void);
static int deInit(void);
static void error(int code);
static void status(int code);
static int setupAccessory(
	const char* manufacturer,
	const char* modelName,
	const char* description,
	const char* version,
	const char* uri,
	const char* serialNumber);

//static
static struct libusb_device_handle* handle;
static char stop;
static char success = 0;

int main (int argc, char *argv[]){
	if(init() < 0)
		return;
	//doTransfer();
	if(setupAccessory(
		    "DebugWithPrintf",//"Manufacturer",
		    "androidusb",//"Model",
		    "Descriptionless description",
		    "vanilla",//"VersionName",
		    "http://nositeavailable.com",
		    "2254711SerialNo.") < 0){
		fprintf(stdout, "Error setting up accessory\n");
		deInit();
		return -1;
	};
    //if(mainPhase() < 0){
	if(mainPhaseOut() < 0){
		fprintf(stdout, "Error during main phase\n");
		deInit();
		return -1;
	}	
	deInit();
	fprintf(stdout, "Done, no errors\n");
	return 0;
}

static int mainPhase(){
	unsigned char buffer[500000];
	int response = 0;
	static int transferred;

	response = libusb_bulk_transfer(handle,IN,buffer,16384, &transferred,0);
	if(response < 0){error(response);return -1;} //Why do this? Because im sending the size first.

	response = libusb_bulk_transfer(handle,IN,buffer,500000, &transferred,0);
	if(response < 0){error(response);return -1;}
	fprintf(stdout,"%s\n", buffer);
	return 0;
}

static int mainPhaseOut(){
	unsigned char buffer[16384];
	int response = 0;
	static int transferred;
	
	//These are for test purposes, the RPi will get the real values by contacting GPS module and OBDII
	char *id = "14";
	int hours = 10;
	int minutes = 11;
	int secs = 12;
	float latitude = 34.123512;
	float longitude = 9.102311;
	unsigned int current_speed = 4;
	unsigned int current_rpm = 2500;
	
    sleep(10);
    printf("Sending data to device\n");
	
	while(1){
        
		sprintf(buffer, "%s %d %d %d %f %f %u %u", id, hours, minutes, secs, latitude, longitude, current_speed, current_rpm);
		current_rpm++; //Teste
        response = LIBUSB_ERROR_TIMEOUT;
        int tries =0;
		response = libusb_bulk_transfer(handle,OUT,buffer, strlen(buffer)+1, &transferred,0); //timeout in secs (0 == no timeout)
        
		if(response < 0){error(response);return -1;}
        //printf("%s", buffer);
        write(1, buffer, strlen(buffer)); //because printf tends to buffer instead of printing right away
        write(1, "\n", 1);
        sleep(1);
	}
    return 0;
}


static int init(){
	libusb_init(NULL);
	if((handle = libusb_open_device_with_vid_pid(NULL, VID, PID)) == NULL){
		fprintf(stdout, "Problem acquiring handle\n");
		return -1;
	}
	libusb_claim_interface(handle, 0);
	return 0;
}

static int deInit(){
	//TODO free all transfers individually...
	//if(ctrlTransfer != NULL)
	//	libusb_free_transfer(ctrlTransfer);
	if(handle != NULL){
		libusb_release_interface (handle, 0);
		libusb_close(handle);
	}
	libusb_exit(NULL);
	return 0;
}

static int setupAccessory(
	const char* manufacturer,
	const char* modelName,
	const char* description,
	const char* version,
	const char* uri,
	const char* serialNumber){

	unsigned char ioBuffer[2];
	int devVersion;
	int response;
	int tries = 10;

	response = libusb_control_transfer(
		handle, //handle
		0xC0, //bmRequestType
		51, //bRequest
		0, //wValue
		0, //wIndex
		ioBuffer, //data
		2, //wLength
        0 //timeout
	);

	if(response < 0){error(response);return-1;}

	devVersion = ioBuffer[1] << 8 | ioBuffer[0];
	fprintf(stdout,"Version Code Device: %d\n", devVersion);
	
	usleep(1000);//sometimes hangs on the next transfer :(

	response = libusb_control_transfer(handle,0x40,52,0,0,(char*)manufacturer,strlen(manufacturer)+1,0);
	if(response < 0){error(response);return -1;}
	response = libusb_control_transfer(handle,0x40,52,0,1,(char*)modelName,strlen(modelName)+1,0);
	if(response < 0){error(response);return -1;}
	response = libusb_control_transfer(handle,0x40,52,0,2,(char*)description,strlen(description)+1,0);
	if(response < 0){error(response);return -1;}
	response = libusb_control_transfer(handle,0x40,52,0,3,(char*)version,strlen(version)+1,0);
	if(response < 0){error(response);return -1;}
	response = libusb_control_transfer(handle,0x40,52,0,4,(char*)uri,strlen(uri)+1,0);
	if(response < 0){error(response);return -1;}
	response = libusb_control_transfer(handle,0x40,52,0,5,(char*)serialNumber,strlen(serialNumber)+1,0);
	if(response < 0){error(response);return -1;}

	fprintf(stdout,"Accessory Identification sent\n", devVersion);

	response = libusb_control_transfer(handle,0x40,53,0,0,NULL,0,0);
	if(response < 0){error(response);return -1;}

	fprintf(stdout,"Attempted to put device into accessory mode\n", devVersion);

	if(handle != NULL){
		libusb_release_interface (handle, 0);
        libusb_close(handle);
    }
    

	for(;;){//attempt to connect to new PID, if that doesn't work try ACCESSORY_PID_ALT
		tries--;
		if((handle = libusb_open_device_with_vid_pid(NULL, ACCESSORY_VID, ACCESSORY_PID_ALT)) == NULL){
			if(tries < 0){
                fprintf(stdout,"Can't open accessory\n");
				return -1;
			}
		}else{
			break;
		}
		sleep(1);
	}
	libusb_claim_interface(handle, 0);
	fprintf(stdout, "Interface claimed, ready to transfer data\n");
	return 0;
}

static void error(int code){
	fprintf(stdout,"\n");
	switch(code){
	case LIBUSB_ERROR_IO:
		fprintf(stdout,"Error: LIBUSB_ERROR_IO\nInput/output error.\n");
		break;
	case LIBUSB_ERROR_INVALID_PARAM:
		fprintf(stdout,"Error: LIBUSB_ERROR_INVALID_PARAM\nInvalid parameter.\n");
		break;
	case LIBUSB_ERROR_ACCESS:
		fprintf(stdout,"Error: LIBUSB_ERROR_ACCESS\nAccess denied (insufficient permissions).\n");
		break;
	case LIBUSB_ERROR_NO_DEVICE:
		fprintf(stdout,"Error: LIBUSB_ERROR_NO_DEVICE\nNo such device (it may have been disconnected).\n");
		break;
	case LIBUSB_ERROR_NOT_FOUND:
		fprintf(stdout,"Error: LIBUSB_ERROR_NOT_FOUND\nEntity not found.\n");
		break;
	case LIBUSB_ERROR_BUSY:
		fprintf(stdout,"Error: LIBUSB_ERROR_BUSY\nResource busy.\n");
		break;
	case LIBUSB_ERROR_TIMEOUT:
		fprintf(stdout,"Error: LIBUSB_ERROR_TIMEOUT\nOperation timed out.\n");
		break;
	case LIBUSB_ERROR_OVERFLOW:
		fprintf(stdout,"Error: LIBUSB_ERROR_OVERFLOW\nOverflow.\n");
		break;
	case LIBUSB_ERROR_PIPE:
		fprintf(stdout,"Error: LIBUSB_ERROR_PIPE\nPipe error.\n");
		break;
	case LIBUSB_ERROR_INTERRUPTED:
		fprintf(stdout,"Error:LIBUSB_ERROR_INTERRUPTED\nSystem call interrupted (perhaps due to signal).\n");
		break;
	case LIBUSB_ERROR_NO_MEM:
		fprintf(stdout,"Error: LIBUSB_ERROR_NO_MEM\nInsufficient memory.\n");
		break;
	case LIBUSB_ERROR_NOT_SUPPORTED:
		fprintf(stdout,"Error: LIBUSB_ERROR_NOT_SUPPORTED\nOperation not supported or unimplemented on this platform.\n");
		break;
	case LIBUSB_ERROR_OTHER:
		fprintf(stdout,"Error: LIBUSB_ERROR_OTHER\nOther error.\n");
		break;
	default:
		fprintf(stdout, "Error: unkown error\n");
	}
}

static void status(int code){
	fprintf(stdout,"\n");
	switch(code){
		case LIBUSB_TRANSFER_COMPLETED:
			fprintf(stdout,"Success: LIBUSB_TRANSFER_COMPLETED\nTransfer completed.\n");
			break;
		case LIBUSB_TRANSFER_ERROR:
			fprintf(stdout,"Error: LIBUSB_TRANSFER_ERROR\nTransfer failed.\n");
			break;
		case LIBUSB_TRANSFER_TIMED_OUT:
			fprintf(stdout,"Error: LIBUSB_TRANSFER_TIMED_OUT\nTransfer timed out.\n");
			break;
		case LIBUSB_TRANSFER_CANCELLED:
			fprintf(stdout,"Error: LIBUSB_TRANSFER_CANCELLED\nTransfer was cancelled.\n");
			break;
		case LIBUSB_TRANSFER_STALL:
			fprintf(stdout,"Error: LIBUSB_TRANSFER_STALL\nFor bulk/interrupt endpoints: halt condition detected (endpoint stalled).\nFor control endpoints: control request not supported.\n");
			break;
		case LIBUSB_TRANSFER_NO_DEVICE:
			fprintf(stdout,"Error: LIBUSB_TRANSFER_NO_DEVICE\nDevice was disconnected.\n");
			break;
		case LIBUSB_TRANSFER_OVERFLOW:
			fprintf(stdout,"Error: LIBUSB_TRANSFER_OVERFLOW\nDevice sent more data than requested.\n");
			break;
		default:
			fprintf(stdout,"Error: unknown error\nTry again(?)\n");
			break;
	}
}


