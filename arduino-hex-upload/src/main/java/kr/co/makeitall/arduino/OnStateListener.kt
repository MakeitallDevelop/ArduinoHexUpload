package kr.co.makeitall.arduino

import android.hardware.usb.UsbDevice
import kr.co.makeitall.arduino.UsbSerialManager.UsbState

interface OnStateListener {
    fun onState(usbDevice: UsbDevice, @UsbState state: Int)
}
