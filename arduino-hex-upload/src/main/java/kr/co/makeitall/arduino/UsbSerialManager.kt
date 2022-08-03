package kr.co.makeitall.arduino

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface.*

class UsbSerialManager(private val context: Context) {

    companion object {
        private val TAG = UsbSerialManager::class.java.simpleName
        const val ACTION_USB_NOT_SUPPORTED = "xyz.vidieukhien.embedded.device.USB_NOT_SUPPORTED"
        const val ACTION_NO_USB = "xyz.vidieukhien.embedded.device.NO_USB"
        const val ACTION_USB_PERMISSION_GRANTED = "xyz.vidieukhien.embedded.device.USB_PERMISSION_GRANTED"
        const val ACTION_USB_PERMISSION_NOT_GRANTED = "xyz.vidieukhien.embedded.device.USB_PERMISSION_NOT_GRANTED"
        const val ACTION_USB_PERMISSION_REQUEST = "xyz.vidieukhien.embedded.device.ACTION_USB_PERMISSION_REQUEST"
        const val ACTION_USB_DISCONNECTED = "xyz.vidieukhien.embedded.device.ACTION_USB_DISCONNECTED"
        const val ACTION_USB_CONNECT = "xyz.vidieukhien.embedded.device.ACTION_USB_CONNECT"
        const val ACTION_USB_READY = "xyz.vidieukhien.embedded.device.USB_READY"
        const val ACTION_CDC_DRIVER_NOT_WORKING = "xyz.vidieukhien.embedded.device.ACTION_CDC_DRIVER_NOT_WORKING"
        const val ACTION_USB_DEVICE_NOT_WORKING = "xyz.vidieukhien.embedded.device.ACTION_USB_DEVICE_NOT_WORKING"
        private const val BAUD_RATE = 115200 // BaudRate. Change this value if you need
    }

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val deviceWithoutPermission: UsbDevice? = null

    // lazy loaded
    private var device: UsbSerialDevice? = null
    private val callback: UsbReadCallback? = null

    /**
     * State changes in the CTS line will be received here
     */
    private val ctsCallback: UsbCTSCallback? = null

    /**
     * State changes in the DSR line will be received here
     */
    private val dsrCallback: UsbDSRCallback? = null

    var usbDeviceList: HashMap<String, UsbDevice> = usbManager.deviceList
        @Throws(UsbSerialException::class)
        get() {
            field = usbManager.deviceList
            if (field.size == 0) {
                throw UsbSerialException(UsbSerialException.TYPE_NOT_CONNECTED)
            }
            return field
        }

    fun requestDevicePermissionForUsbDevice(usbKey: String, usbDevice: UsbDevice?) {
        synchronized(UsbSerialManager::class.java) {
            usbManager.requestPermission(
                usbDevice,
                PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION_REQUEST), 0)
            )
        }
    }

    fun deviceIsUsbSerial(usbDevice: UsbDevice?): Boolean {
        synchronized(UsbSerialManager::class.java) {
            usbDevice?.let {
                // no usable interfaces
                if (it.interfaceCount <= 0) {
                    return false
                }
                val deviceVID = it.vendorId
                val devicePID = it.productId

                // Danh sach thiet bi
                // There is a device connected to our Android device. Try to open it as a Serial Port.
                return deviceVID != 0x1d6b
            }
            return false
        }
    }

    fun getDevicePermission(usbKey: String) {
        synchronized(UsbSerialManager::class.java) {
            usbDeviceList = usbManager.deviceList
            val usbDevice = usbDeviceList[usbKey]
            if (!usbManager.hasPermission(usbDevice)) {
                requestDevicePermissionForUsbDevice(usbKey, usbDevice)
            }
        }
    }

    fun checkDevicePermission(usbKey: String): Boolean {
        synchronized(UsbSerialManager::class.java) {
            usbDeviceList = usbManager.deviceList
            val usbDevice = usbDeviceList[usbKey]
            return usbManager.hasPermission(usbDevice)
        }
    }

    fun tryGetDevice(key: String): UsbSerialDevice? {
        synchronized(UsbSerialManager::class.java) {
            if (device == null) {
                Log.i(TAG, "tryGetDevice: trying to find and open connection to usb serial")
                usbDeviceList = usbManager.deviceList
                val usbDevice = usbDeviceList[key]

                if (!deviceIsUsbSerial(usbDevice)) {
                    context.sendBroadcast(Intent(ACTION_USB_NOT_SUPPORTED))
                    return null
                }
                Log.i(TAG, "tryGetDevice: Serial device found")
                if (!usbManager.hasPermission(usbDevice)) {
                    context.sendBroadcast(Intent(ACTION_USB_PERMISSION_NOT_GRANTED))
                    return null
                }
                usbManager.openDevice(usbDevice)?.let { conn ->
                    device = UsbSerialDevice.createUsbSerialDevice(usbDevice, conn)
                    device?.apply {
                        if (open()) {
                            setBaudRate(BAUD_RATE)
                            setDataBits(DATA_BITS_8)
                            setStopBits(STOP_BITS_1)
                            setParity(PARITY_NONE)
                            setFlowControl(FLOW_CONTROL_OFF)
                            callback?.let { read(it) }
                            ctsCallback?.let { getCTS(it) }
                            dsrCallback?.let { getDSR(it) }
                            context.sendBroadcast(Intent(ACTION_USB_READY))
                            Log.i(TAG, "tryGetDevice: ACTION_USB_READY ")
                            close() //close before work
                        }
                    } ?: run {
                        // No driver for given device, even generic CDC driver could not be loaded
                        Log.i(TAG, "tryGetDevice: ACTION_USB_NOT_SUPPORTED ")
                        context.sendBroadcast(Intent(ACTION_USB_NOT_SUPPORTED))
                        return null
                    }
                } ?: Log.e(TAG, "tryGetDevice: could not open connection")

                Log.i(
                    TAG, "tryGetDevice: " +
                            if (device == null) "Usb device not found or failed to connect"
                            else "Usb device successfully connected"
                )
            } else {
                Log.i(TAG, "tryGetDevice: using already connected device")
            }
            return device
        }
    }
}
