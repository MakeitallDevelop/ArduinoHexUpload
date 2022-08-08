package kr.co.makeitall.arduino

import android.hardware.usb.UsbDevice
import kr.co.makeitall.arduino.UsbSerialManager.UsbPermission

interface OnPermissionListener {
    fun onPermission(usbDevice: UsbDevice, @UsbPermission permission: Int)
}
