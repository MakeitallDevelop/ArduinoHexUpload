package kr.co.makeitall.arduino

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.annotation.IntDef
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface.*
import java.nio.charset.StandardCharsets

class UsbSerialManager(private val context: Context, lifecycle: Lifecycle) : LifecycleObserver {

    companion object {
        private val TAG = UsbSerialManager::class.java.simpleName

        private const val BAUD_RATE = 115200 // BaudRate. Change this value if you need

        const val ACTION_USB_PERMISSION_REQUEST = "kr.co.makeitall.arduino.action.USB_PERMISSION_REQUEST"

        const val USB_PERMISSION_GRANTED = 0
        const val USB_PERMISSION_DENIED = 1

        const val USB_STATE_READY = 2
        const val USB_STATE_CONNECTED = 3
        const val USB_STATE_DISCONNECTED = 4
    }

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        USB_PERMISSION_GRANTED,
        USB_PERMISSION_DENIED
    )
    annotation class UsbPermission

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        USB_STATE_READY,
        USB_STATE_CONNECTED,
        USB_STATE_DISCONNECTED
    )
    annotation class UsbState

    init {
        lifecycle.addObserver(this@UsbSerialManager)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreated(source: LifecycleOwner) {
        Log.i(TAG, "onCreated")
        registerUsbHardwareReceiver()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        Log.i(TAG, "onResume")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        Log.i(TAG, "onPause")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        Log.i(TAG, "onDestroy")
        removeOnStateListener()
        removeOnPermissionListener()
        removeOnErrorListener()
        removeOnUsbReadListener()
        removeOnUsbCtsListener()
        removeOnUsbDsrListener()
        unregisterUsbHardwareReceiver()
    }

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private var onErrorListener: OnErrorListener? = null
    private var onPermissionListener: OnPermissionListener? = null
    private var onStateListener: OnStateListener? = null

    var usbDevice: UsbDevice? = null
    var usbSerialDevice: UsbSerialDevice? = null

    private var usbReadCallback: UsbReadCallback? = null

    /**
     * State changes in the CTS line will be received here
     */
    private var usbCtsCallback: UsbCTSCallback? = null

    /**
     * State changes in the DSR line will be received here
     */
    private var usbDsrCallback: UsbDSRCallback? = null

    private val usbHardwareReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION_REQUEST -> {
                    val isGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)?.let { usbDevice ->
                        if (isGranted) {
                            this@UsbSerialManager.usbDevice = usbDevice
                            getUsbSerialDevice()
                        }
                        onPermissionListener?.onPermission(
                            usbDevice,
                            if (isGranted) USB_PERMISSION_GRANTED
                            else USB_PERMISSION_DENIED
                        )
                    }
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)?.let { usbDevice ->
                        if (!isUsbSerialDevice(usbDevice)) {
                            onErrorListener?.onError(
                                usbDevice,
                                UsbSerialException(UsbSerialException.USB_ERROR_NOT_SUPPORTED)
                            )
                        } else {
                            onStateListener?.onState(usbDevice, USB_STATE_CONNECTED)
                        }
                    }
                    Log.i(TAG, "usb attached")
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)?.let { usbDevice ->
                        if (this@UsbSerialManager.usbDevice == usbDevice) {
                            usbSerialDevice?.close()
                            this@UsbSerialManager.usbDevice = null
                            usbSerialDevice = null
                        }
                        onStateListener?.onState(usbDevice, USB_STATE_DISCONNECTED)
                        removeOnUsbReadListener()
                        removeOnUsbCtsListener()
                        removeOnUsbDsrListener()
                    }
                    Log.i(TAG, "usb detached")
                }

                UsbManager.ACTION_USB_ACCESSORY_ATTACHED -> {
                    intent.getParcelableExtra<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)?.let { usbAccessory ->

                    }
                    Log.i(TAG, "accessory attached")
                }

                UsbManager.ACTION_USB_ACCESSORY_DETACHED -> {
                    intent.getParcelableExtra<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)?.let { usbAccessory ->

                    }
                    Log.i(TAG, "accessory detached")
                }
            }
        }
    }

    private fun registerUsbHardwareReceiver() {
        Log.i(TAG, "registerUsbHardwareReceiver")
        context.registerReceiver(usbHardwareReceiver,
            IntentFilter().apply {
                addAction(ACTION_USB_PERMISSION_REQUEST)
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
//                addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
//                addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
            }
        )
    }

    private fun unregisterUsbHardwareReceiver() {
        context.unregisterReceiver(usbHardwareReceiver)
    }

    fun addOnErrorListener(listener: (UsbDevice, UsbSerialException) -> Unit): UsbSerialManager {
        onErrorListener = object : OnErrorListener {
            override fun onError(usbDevice: UsbDevice, error: UsbSerialException) {
                listener(usbDevice, error)
            }
        }
        return this@UsbSerialManager
    }

    fun removeOnErrorListener() {
        onErrorListener = null
    }

    fun addOnPermissionListener(listener: (UsbDevice, Int) -> Unit): UsbSerialManager {
        onPermissionListener = object : OnPermissionListener {
            override fun onPermission(usbDevice: UsbDevice, @UsbPermission permission: Int) {
                listener(usbDevice, permission)
            }
        }
        return this@UsbSerialManager
    }

    fun removeOnPermissionListener() {
        onPermissionListener = null
    }

    fun addOnStateListener(listener: (UsbDevice, Int) -> Unit): UsbSerialManager {
        onStateListener = object : OnStateListener {
            override fun onState(usbDevice: UsbDevice, @UsbState state: Int) {
                listener(usbDevice, state)
            }
        }
        return this@UsbSerialManager
    }

    fun removeOnStateListener() {
        onStateListener = null
    }

    fun addOnUsbReadListener(
        baudRate: Int = BAUD_RATE,
        dataBits: Int = DATA_BITS_8,
        stopBits: Int = STOP_BITS_1,
        listener: (String?) -> Unit
    ): UsbSerialManager {
        usbReadCallback = UsbReadCallback { data ->
            listener(data?.let { String(it, StandardCharsets.UTF_8) })
        }
        usbSerialDevice?.apply {
            if (open()) {
                setBaudRate(baudRate)
                setDataBits(dataBits)
                setStopBits(stopBits)
                setParity(PARITY_NONE)
                setFlowControl(FLOW_CONTROL_OFF)
                usbReadCallback?.let { read(it) }
                usbCtsCallback?.let { getCTS(it) }
                usbDsrCallback?.let { getDSR(it) }
                usbDevice?.let { onStateListener?.onState(it, USB_STATE_READY) }
                Log.i(TAG, "tryGetDevice: ACTION_USB_READY ")
            } else {
                usbDevice?.let {
                    onErrorListener?.onError(
                        it,
                        UsbSerialException(UsbSerialException.USB_ERROR_NOT_SUPPORTED)
                    )
                }
            }
        }
        return this@UsbSerialManager
    }

    fun removeOnUsbReadListener() {
        usbSerialDevice?.close()
        usbReadCallback = null
    }

    fun addOnUsbCtsListener(listener: (Boolean) -> Unit): UsbSerialManager {
        usbCtsCallback = UsbCTSCallback {
            listener(it)
        }
        return this@UsbSerialManager
    }

    fun removeOnUsbCtsListener() {
        usbCtsCallback = null
    }

    fun addOnUsbDsrListener(listener: (Boolean) -> Unit): UsbSerialManager {
        usbDsrCallback = UsbDSRCallback {
            listener(it)
        }
        return this@UsbSerialManager
    }

    fun removeOnUsbDsrListener() {
        usbDsrCallback = null
    }

    fun hasPermission(usbDevice: UsbDevice? = this@UsbSerialManager.usbDevice): Boolean {
        return usbDevice?.let { usbManager.hasPermission(it) } ?: false
    }

    fun requestPermission(usbDevice: UsbDevice? = this@UsbSerialManager.usbDevice) {
        if (!usbManager.hasPermission(usbDevice)) {
            usbManager.requestPermission(
                usbDevice,
                PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION_REQUEST), 0)
            )
        }
    }

    fun isUsbSerialDevice(usbDevice: UsbDevice? = this@UsbSerialManager.usbDevice): Boolean =
        usbDevice?.let {
            // no usable interfaces
            if (it.interfaceCount <= 0) {
                return false
            }
            val vendorId = it.vendorId
            val productId = it.productId
            // There is a device connected to our Android device. Try to open it as a Serial Port.
            vendorId != 0x1d6b && productId != 0x0001 && productId != 0x0002 && productId != 0x0003
        } ?: false

    fun getUsbSerialDevice(usbDevice: UsbDevice? = this@UsbSerialManager.usbDevice): UsbSerialDevice? {
        usbDevice?.let { device ->
            usbManager.openDevice(device)?.let { connection ->
                usbSerialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection)
                usbSerialDevice?.apply {
                    if (!isUsbSerialDevice(device)) {
                        onErrorListener?.onError(device, UsbSerialException(UsbSerialException.USB_ERROR_NOT_SUPPORTED))
                        return null
                    }
                    Log.i(TAG, "tryGetDevice: Serial device found")
                    if (!usbManager.hasPermission(device)) {
                        onPermissionListener?.onPermission(device, USB_PERMISSION_DENIED)
                        return null
                    }
                } ?: run {
                    // No driver for given device, even generic CDC driver could not be loaded
                    Log.e(TAG, "tryGetDevice: ACTION_USB_NOT_SUPPORTED")
                    onErrorListener?.onError(device, UsbSerialException(UsbSerialException.USB_ERROR_NOT_SUPPORTED))
                    return null
                }
            } ?: Log.e(TAG, "tryGetDevice: could not open connection")
        }

        return usbSerialDevice
    }

    fun updateDevice() {
        val map = usbManager.deviceList
        if (map.size <= 0) {
            usbDevice = null
            Log.i(TAG, "usbDevice null")
        } else {
            val iterator: Iterator<UsbDevice> = map.values.iterator()
            var device: UsbDevice? = null
            while (iterator.hasNext()) {
                device = iterator.next()
            }
            usbDevice = device
            Log.i(TAG, "usbDevice valid")
        }
        getUsbSerialDevice()
    }
}
