package kr.co.makeitall.arduino

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.annotation.IntDef
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.felhr.usbserial.*
import kotlinx.coroutines.*

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
        const val USB_STATE_READ_START = 5
        const val USB_STATE_READ_END = 6

        const val USB_ERROR_NOT_SUPPORTED = -1
        const val USB_ERROR_NO_USB = -2
        const val USB_ERROR_USB_DEVICE_NOT_WORKING = -3
        const val USB_ERROR_CDC_DRIVER_NOT_WORKING = -4
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
        USB_STATE_DISCONNECTED,
        USB_STATE_READ_START,
        USB_STATE_READ_END
    )
    annotation class UsbState

    private interface OnErrorListener {
        fun onError(error: UsbSerialException)
    }

    private fun OnErrorListener(block: (UsbSerialException) -> Unit): OnErrorListener =
        object : OnErrorListener {
            override fun onError(error: UsbSerialException) {
                CoroutineScope(Dispatchers.Main).launch { block(error) }
            }
        }

    private interface OnPermissionListener {
        fun onPermission(@UsbPermission permission: Int)
    }

    private fun OnPermissionListener(block: (Int) -> Unit): OnPermissionListener =
        object : OnPermissionListener {
            override fun onPermission(@UsbPermission permission: Int) {
                CoroutineScope(Dispatchers.Main).launch { block(permission) }
            }
        }

    private interface OnStateListener {
        fun onState(@UsbState state: Int)
    }

    private fun OnStateListener(block: (Int) -> Unit): OnStateListener =
        object : OnStateListener {
            override fun onState(@UsbState state: Int) {
                CoroutineScope(Dispatchers.Main).launch { block(state) }
            }
        }

    private interface OnUsbReadListener {
        fun onRead(data: String)
    }

    private fun OnUsbReadListener(block: (String) -> Unit): OnUsbReadListener =
        object : OnUsbReadListener {
            override fun onRead(data: String) {
                CoroutineScope(Dispatchers.Main).launch { block(data) }
            }
        }

    init {
        lifecycle.addObserver(this@UsbSerialManager)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreated(source: LifecycleOwner) {
        Log.i(TAG, "onCreated")
        registerUsbHardwareReceiver()
        updateDevice()
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
    private var connection: UsbDeviceConnection? = null
    private var isReading = false

    private var serialInputStream: SerialInputStream? = null
    private var serialOutputStream: SerialOutputStream? = null

    private var onUsbReadListener: OnUsbReadListener? = null

    /**
     * State changes in the CTS line will be received here
     */
    private var usbCtsCallback: UsbSerialInterface.UsbCTSCallback? = null

    /**
     * State changes in the DSR line will be received here
     */
    private var usbDsrCallback: UsbSerialInterface.UsbDSRCallback? = null

    private val usbHardwareReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION_REQUEST -> {
                    val isGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)?.let { device ->
                        if (isGranted) {
                            connection = usbManager.openDevice(device)
                            Log.i(TAG, "connectDevice() ready")
                            // Everything went as expected. Send an intent to MainActivity
                            onStateListener?.onState(USB_STATE_READY)
//                            startRead()
                        }
                        onPermissionListener?.onPermission(
                            if (isGranted) USB_PERMISSION_GRANTED
                            else USB_PERMISSION_DENIED
                        )
                    }
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)?.let { device ->
                        if (!isReading) {
                            updateDevice()
                        }
                        onStateListener?.onState(USB_STATE_CONNECTED)
                    }
                    Log.i(TAG, "usb attached")
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)?.let { device ->
                        stopRead()

                        onStateListener?.onState(USB_STATE_DISCONNECTED)
                    }
                    Log.i(TAG, "usb detached")
                }

                UsbManager.ACTION_USB_ACCESSORY_ATTACHED -> {
                    intent.getParcelableExtra<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)?.let { accessory ->

                    }
                    Log.i(TAG, "accessory attached")
                }

                UsbManager.ACTION_USB_ACCESSORY_DETACHED -> {
                    intent.getParcelableExtra<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)?.let { accessory ->

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
                addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
                addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
            }
        )
    }

    private fun unregisterUsbHardwareReceiver() {
        context.unregisterReceiver(usbHardwareReceiver)
    }

    fun addOnErrorListener(listener: (UsbSerialException) -> Unit): UsbSerialManager {
        onErrorListener = OnErrorListener { listener(it) }
        return this@UsbSerialManager
    }

    fun removeOnErrorListener() {
        onErrorListener = null
    }

    fun addOnPermissionListener(listener: (Int) -> Unit): UsbSerialManager {
        onPermissionListener = OnPermissionListener { listener(it) }
        return this@UsbSerialManager
    }

    fun removeOnPermissionListener() {
        onPermissionListener = null
    }

    fun addOnStateListener(listener: (Int) -> Unit): UsbSerialManager {
        onStateListener = OnStateListener { listener(it) }
        return this@UsbSerialManager
    }

    fun removeOnStateListener() {
        onStateListener = null
    }

    fun addOnUsbReadListener(
        baudRate: Int = BAUD_RATE,
        dataBits: Int = UsbSerialInterface.DATA_BITS_8,
        stopBits: Int = UsbSerialInterface.STOP_BITS_1,
        listener: (String?) -> Unit
    ): UsbSerialManager {
        onUsbReadListener = OnUsbReadListener { listener(it) }

        usbSerialDevice?.apply {
            setBaudRate(baudRate)
            setDataBits(dataBits)
            setStopBits(stopBits)
        }
        return this@UsbSerialManager
    }

    fun removeOnUsbReadListener() {
        onUsbReadListener = null
    }

    fun addOnUsbCtsListener(listener: (Boolean) -> Unit): UsbSerialManager {
        usbCtsCallback = UsbSerialInterface.UsbCTSCallback { listener(it) }
        return this@UsbSerialManager
    }

    fun removeOnUsbCtsListener() {
        usbCtsCallback = null
    }

    fun addOnUsbDsrListener(listener: (Boolean) -> Unit): UsbSerialManager {
        usbDsrCallback = UsbSerialInterface.UsbDSRCallback { listener(it) }
        return this@UsbSerialManager
    }

    fun removeOnUsbDsrListener() {
        usbDsrCallback = null
    }

    fun hasPermission(): Boolean {
        return usbDevice?.let { usbManager.hasPermission(it) } ?: false
    }

    fun requestPermission() {
        usbManager.requestPermission(
            usbDevice,
            PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION_REQUEST), 0)
        )
    }

    fun writeString(data: String) {
        Log.w(TAG, "${data}, ${data.toByteArray().contentToString()}")
        serialOutputStream?.write(data.toByteArray())
    }

    fun updateBaudRate(baudRate: Int) {
        usbSerialDevice?.setBaudRate(baudRate)
    }

    fun isUsbSerialDevice(): Boolean =
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

    private fun updateDevice() {
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        val usbDevices = usbManager.deviceList
        if (usbDevices.isNotEmpty()) {
            for ((_, device) in usbDevices) {
                Log.d(
                    TAG,
                    String.format(
                        "USBDevice.HashMap (vid:pid) (%X:%X)-%b class:%X:%X name:%s",
                        device.vendorId, device.productId, UsbSerialDevice.isSupported(device),
                        device.deviceClass, device.deviceSubclass, device.deviceName
                    )
                )

                usbDevice = device

                if (isUsbSerialDevice()) {
//                if (UsbSerialDevice.isSupported(device)) {
                    // There is a device connected to our Android device. Try to open it as a Serial Port.
                    requestPermission()
                    break
                } else {
                    connection = null
                    usbDevice = null
                }
            }
            if (usbDevice == null) {
                onErrorListener?.onError(UsbSerialException(USB_ERROR_NO_USB))
            }
        } else {
            Log.e(TAG, "updateDevice() usbManager returned empty device list.")
            // There is no USB devices connected. Send an intent to MainActivity
            onErrorListener?.onError(UsbSerialException(USB_ERROR_NO_USB))
        }
    }

    /**
     * A simple thread to open a serial port.
     * Although it should be a fast operation. moving usb operations away from UI thread is a good thing.
     */
    fun startRead(lateStartTimeMillis: Long = 0) {
        CoroutineScope(Dispatchers.Main).launch {
            usbSerialDevice = UsbSerialDevice.createUsbSerialDevice(usbDevice, connection)
            Log.i(TAG, "connectDevice() usbSerialDevice: $usbSerialDevice")
            usbSerialDevice?.let { serial ->
                if (serial.syncOpen()) {
                    isReading = true
                    serial.apply {
                        setBaudRate(BAUD_RATE)
                        setDataBits(UsbSerialInterface.DATA_BITS_8)
                        setStopBits(UsbSerialInterface.STOP_BITS_1)
                        setParity(UsbSerialInterface.PARITY_NONE)

                        // Current flow control Options:
                        // UsbSerialInterface.FLOW_CONTROL_OFF
                        // UsbSerialInterface.FLOW_CONTROL_RTS_CTS only for CP2102 and FT232
                        // UsbSerialInterface.FLOW_CONTROL_DSR_DTR only for CP2102 and FT232
                        setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)

                        // InputStream and OutputStream will be null if you are using async api.
                        serialInputStream = serial.inputStream
                        serialOutputStream = serial.outputStream
                    }

                    serialInputStream?.readString(lateStartTimeMillis) {
                        onUsbReadListener?.onRead(it)
                    }

                } else {
                    // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
                    // Send an Intent to Main Activity
                    onErrorListener?.onError(
                        UsbSerialException(
                            if (serial is CDCSerialDevice)
                                USB_ERROR_CDC_DRIVER_NOT_WORKING
                            else
                                USB_ERROR_USB_DEVICE_NOT_WORKING
                        )
                    )
                }
            } ?: run {
                // No driver for given device, even generic CDC driver could not be loaded
                onErrorListener?.onError(UsbSerialException(USB_ERROR_NOT_SUPPORTED))
            }
        }
    }

    private suspend fun SerialInputStream.readString(lateStartTimeMillis: Long, callback: (String) -> Unit) {
        val buffer = ByteArray(1024)
        setTimeout(1)
        withContext(Dispatchers.IO) {
            // Some Arduinos would need some sleep because firmware wait some time to know whether a new sketch is going
            // to be uploaded or not
            if (lateStartTimeMillis > 0) {
                delay(lateStartTimeMillis) // sleep some. YMMV with different chips.
            }

            Log.i(TAG, "readThread() start")
            onStateListener?.onState(USB_STATE_READ_START)

            while (isReading) {
                val length = read(buffer)
                if (length > 0) {
                    val data = String(buffer, 0, length)
//                    Log.d(TAG, "length: $length, data: $data")
                    callback(data)
                }
            }

            Log.i(TAG, "readThread() end")
            onStateListener?.onState(USB_STATE_READ_END)
        }
    }

    fun stopRead() {
        if (isReading) {
            usbSerialDevice?.syncClose()
        }
        isReading = false
        serialInputStream?.close()
        serialOutputStream?.close()
        usbSerialDevice = null
        serialInputStream = null
        serialOutputStream = null
    }

    fun isReading(): Boolean = isReading
}
