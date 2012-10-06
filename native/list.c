#import <CoreFoundation/CoreFoundation.h>
#include <stdio.h>
#include "MobileDevice.h"

// perform error check
void check(const char* msg, mach_error_t code) {
    if (code!=0) {
        fprintf(stderr,"Error(%d):%s\n",code,msg);
        exit(-1);
    }
}


void onDevice(am_device_notification_callback_info* info, void* _) {
    if (info->msg!=ADNCI_MSG_CONNECTED) return;

    am_device* device = info->dev;

    CFStringRef id = AMDeviceCopyDeviceIdentifier(device);
    // CFShow(id);
    CFRelease(id);

    check("Connecting to device",
            AMDeviceConnect(device));
    check("Validate pairing",
            AMDeviceValidatePairing(device));
    check("Starting a session",
            AMDeviceStartSession(device));

    char* properties[] = {
        // "ActivationPublicKey",
        "ActivationState",
        "BluetoothAddress",
        "BuildVersion",
        "CPUArchitecture",
        // "DeviceCertificate",
        "DeviceClass",  // iPhone, iPad, iPod, etc.
        "DeviceColor",
        "DeviceName",
        //"DieID",
        "FirmwareVersion",
        "HardwareModel",
        "HardwarePlatform",
        "InternationalMobileEquipmentIdentity",
        "MLBSerialNumber",
        "ModelNumber",
        "PhoneNumber",
        "ProductType",
        "ProductVersion", // iOS version
        "SerialNumber",
        "SIMStatus",
        // "SupportedDeviceFamilies",
        // "UniqueChipID",
        "UniqueDeviceID",
        "WiFiAddress",
        NULL}; 

    int i;
    for (i=0; properties[i]!=NULL; i++) {
        char* prop = properties[i];
        CFStringRef cprop = CFStringCreateWithCString(NULL,prop,kCFStringEncodingUTF8);
        CFStringRef s = AMDeviceCopyValue(device,0,cprop);

        if (s==NULL) {
            // no property value
        } else {
            //CFShow(s);
            int cflen = CFStringGetMaximumSizeForEncoding(CFStringGetLength(s),kCFStringEncodingUTF8);
            char* p = (char*)malloc(cflen+1);

            CFStringGetCString(s, p, cflen, kCFStringEncodingUTF8);

            printf("%s=%s\n", prop,p,cflen);
            CFRelease(s);
        }
        CFRelease(cprop);
    }
    puts(""); // separator
    check("Disconnecting",AMDeviceStopSession(device));
}

void timeout(CFRunLoopTimerRef timer, void* cookie) {
    exit(0);
}

int main(int argc, char** argv) {

    CFRunLoopTimerRef timer = CFRunLoopTimerCreate(NULL, CFAbsoluteTimeGetCurrent()+1, 0,0,0, timeout, NULL);
    CFRunLoopAddTimer(CFRunLoopGetCurrent(), timer, kCFRunLoopCommonModes);

    am_device_notification* notification;
    check("Subscribing to device notification",
            AMDeviceNotificationSubscribe(&onDevice, 0,0,NULL, &notification));
    CFRunLoopRun();
    return 1;
}
