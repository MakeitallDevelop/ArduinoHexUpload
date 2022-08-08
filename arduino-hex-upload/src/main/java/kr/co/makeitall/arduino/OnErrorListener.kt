package kr.co.makeitall.arduino

import android.hardware.usb.UsbDevice

interface OnErrorListener {
    fun onError(usbDevice: UsbDevice, error: UsbSerialException)
}
