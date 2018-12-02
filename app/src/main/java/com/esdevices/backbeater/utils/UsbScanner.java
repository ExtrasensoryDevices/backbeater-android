package com.esdevices.backbeater.utils;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.CountDownTimer;

/**
 *
 */
public final class UsbScanner {

    public interface UsbMicrophoneListener {
        void onUSBScanComplete(boolean microphoneFound);
    }

    private Context context;
    private UsbMicrophoneListener listener;

    public UsbScanner(Context context, UsbMicrophoneListener listener) {
        this.context = context;
        this.listener = listener;
    }


    public boolean isUsbClassAudioDeviceConnected() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            boolean shouldCheck = false;
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            for (UsbDevice device : usbManager.getDeviceList().values()) {
                for (int i = 0; i < device.getInterfaceCount(); i++) {
                    if (device.getInterface(i).getInterfaceClass() == UsbConstants.USB_CLASS_AUDIO) {
                        shouldCheck = true;
                        break;
                    }
                }
            }
            return shouldCheck;
        } else {
            return false;
        }
    }

    public void checkForUSBMic() {
        timer.cancel();
        timer.start();
    }

    public void stopCheckingForUSBMic() {
        timer.cancel();
    }



    private CountDownTimer timer = new CountDownTimer(3000, 500) {
        public void onTick(long millisUntilFinished) {
            checkUSBMicOnTick(false);
        }
        public void onFinish() {
            checkUSBMicOnTick(true);
        }
    };


    private void checkUSBMicOnTick(boolean finished) {
        if (android.os.Build.VERSION.SDK_INT >=  23) {
            boolean found = false;
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            AudioDeviceInfo[] devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_ALL);
            for (AudioDeviceInfo device : devices) {
                int deviceType = device.getType();

                if (deviceType == AudioDeviceInfo.TYPE_USB_DEVICE) {
                    found = true;
                    break;
                }
            }
            if (found) {
                timer.cancel();
                listener.onUSBScanComplete(found);
            }
            if (!found && finished) {
                listener.onUSBScanComplete(false);
            }
        } else {
            listener.onUSBScanComplete(false);
        }
    }
}
