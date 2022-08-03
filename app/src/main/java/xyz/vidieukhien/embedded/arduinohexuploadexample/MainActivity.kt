package xyz.vidieukhien.embedded.arduinohexuploadexample

import ArduinoUploader.ArduinoSketchUploader
import ArduinoUploader.ArduinoUploaderException
import ArduinoUploader.Config.Arduino
import ArduinoUploader.Config.McuIdentifier
import ArduinoUploader.Config.Protocol
import ArduinoUploader.IArduinoUploaderLogger
import CSharpStyle.IProgress
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.felhr.usbserial.UsbSerialDevice
import kr.co.makeitall.arduino.Boards
import kr.co.makeitall.arduino.LineReader
import kr.co.makeitall.arduino.SerialPortStreamImpl
import kr.co.makeitall.arduino.UsbSerialManager
import xyz.vidieukhien.embedded.arduinohexuploadexample.databinding.ActivityMainBinding
import java.io.InputStreamReader
import java.io.Reader

class MainActivity : AppCompatActivity() {

    enum class UsbConnectState {
        DISCONNECTED, CONNECT
    }

    private val mUsbNotifyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbSerialManager.ACTION_USB_PERMISSION_GRANTED ->
                    Toast.makeText(context, "USB permission granted", Toast.LENGTH_SHORT).show()

                UsbSerialManager.ACTION_USB_PERMISSION_NOT_GRANTED ->
                    Toast.makeText(context, "USB Permission denied", Toast.LENGTH_SHORT).show()

                UsbSerialManager.ACTION_NO_USB ->
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show()

                UsbSerialManager.ACTION_USB_DISCONNECTED -> {
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show()
                    usbConnectChange(UsbConnectState.DISCONNECTED)
                }

                UsbSerialManager.ACTION_USB_CONNECT -> {
                    Toast.makeText(context, "USB connected", Toast.LENGTH_SHORT).show()
                    usbConnectChange(UsbConnectState.CONNECT)
                }

                UsbSerialManager.ACTION_USB_NOT_SUPPORTED ->
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show()

                UsbSerialManager.ACTION_USB_READY ->
                    Toast.makeText(context, "Usb device ready", Toast.LENGTH_SHORT).show()

                UsbSerialManager.ACTION_USB_DEVICE_NOT_WORKING ->
                    Toast.makeText(context, "USB device not working", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private val mUsbHardwareReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == UsbSerialManager.ACTION_USB_PERMISSION_REQUEST) {
                val granted = intent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if (granted) { // User accepted our USB connection. Try to open the device as a serial port
                    val grantedDevice = intent.extras!!.getParcelable<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    usbPermissionGranted(grantedDevice!!.deviceName)
//                    Intent it = new Intent(UsbSerialManager.ACTION_USB_PERMISSION_GRANTED);
//                    context.sendBroadcast(it);
                    Toast.makeText(context, "USB permission granted", Toast.LENGTH_SHORT).show()
                } else { // User not accepted our USB connection. Send an Intent to the Main Activity
//                    Intent it = new Intent(UsbSerialManager.ACTION_USB_PERMISSION_NOT_GRANTED);
//                    context.sendBroadcast(it);
                    Toast.makeText(context, "USB Permission denied", Toast.LENGTH_SHORT).show()
                }
            } else if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
//                Intent it = new Intent(UsbSerialManager.ACTION_USB_CONNECT);
//                context.sendBroadcast(it);
                Toast.makeText(context, "USB connected", Toast.LENGTH_SHORT).show()
                usbConnectChange(UsbConnectState.CONNECT)
            } else if (intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                // Usb device was disconnected. send an intent to the Main Activity
//                Intent it = new Intent(UsbSerialManager.ACTION_USB_DISCONNECTED);
//                context.sendBroadcast(it);
                Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show()
                usbConnectChange(UsbConnectState.DISCONNECTED)
            }
        }
    }

    private fun setUsbFilter() {
        val filter = IntentFilter()
        filter.addAction(UsbSerialManager.ACTION_USB_PERMISSION_REQUEST)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        registerReceiver(mUsbHardwareReceiver, filter)
    }

    private lateinit var binding: ActivityMainBinding

    private lateinit var usbSerialManager: UsbSerialManager

    private var deviceKeyName: String? = null

    fun usbConnectChange(state: UsbConnectState) {
        if (state == UsbConnectState.DISCONNECTED) {
            binding.requestButton.visibility = View.INVISIBLE
            binding.fab.hide()
        } else if (state == UsbConnectState.CONNECT) {
            binding.requestButton.visibility = View.VISIBLE
        }
    }

    fun usbPermissionGranted(usbKey: String) {
        Toast.makeText(this, "UsbPermissionGranted:$usbKey", Toast.LENGTH_SHORT).show()
        binding.portSelect.text = usbKey
        deviceKeyName = usbKey
        binding.fab.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this@MainActivity, R.layout.activity_main)

        usbSerialManager = UsbSerialManager(this@MainActivity)
        setUsbFilter()

        setSupportActionBar(binding.toolbar)

        binding.requestButton.setOnClickListener {
            val (keySelect) = usbSerialManager.usbDeviceList.entries.iterator().next()
            val hasPem = checkDevicePermission(keySelect)
            if (hasPem) {
                binding.portSelect.text = keySelect
                deviceKeyName = keySelect
                binding.fab.show()
            } else {
                requestDevicePermission(keySelect)
            }
        }
        binding.fab.setOnClickListener {
            Thread(UploadRunnable()).start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mUsbHardwareReceiver)
    }

    private fun setFilters() {
        val filter = IntentFilter()
        filter.addAction(UsbSerialManager.ACTION_USB_PERMISSION_GRANTED)
        filter.addAction(UsbSerialManager.ACTION_NO_USB)
        filter.addAction(UsbSerialManager.ACTION_USB_DISCONNECTED)
        filter.addAction(UsbSerialManager.ACTION_USB_CONNECT)
        filter.addAction(UsbSerialManager.ACTION_USB_NOT_SUPPORTED)
        filter.addAction(UsbSerialManager.ACTION_USB_PERMISSION_NOT_GRANTED)
        registerReceiver(mUsbNotifyReceiver, filter)
    }

    private fun requestDevicePermission(key: String?) {
        usbSerialManager.getDevicePermission(key!!)
    }

    private fun checkDevicePermission(key: String?): Boolean {
        return usbSerialManager.checkDevicePermission(key!!)
    }

    private fun getUsbSerialDevice(key: String?): UsbSerialDevice? {
        return usbSerialManager.tryGetDevice(key!!)
    }

    override fun onResume() {
        super.onResume()
        setFilters()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mUsbNotifyReceiver)
    }

    fun uploadHex() {
        val board = Boards.ARDUINO_UNO
        val arduinoBoard = Arduino(board.boardName, board.chipType, board.uploadBaudRate, board.uploadProtocol)
        val protocol = Protocol.valueOf(arduinoBoard.protocol.name)
        val mcu = McuIdentifier.valueOf(arduinoBoard.mcu.name)
        val preOpenRst = arduinoBoard.preOpenResetBehavior
        var preOpenStr = preOpenRst
        if (preOpenRst == null)
            preOpenStr = ""
        else if (preOpenStr.equals("none", ignoreCase = true))
            preOpenStr = ""
        val postOpenRst = arduinoBoard.postOpenResetBehavior
        var postOpenStr = postOpenRst
        if (postOpenRst == null)
            postOpenStr = ""
        else if (postOpenStr.equals("none", ignoreCase = true))
            postOpenStr = ""
        val closeRst = arduinoBoard.closeResetBehavior
        var closeStr = closeRst
        if (closeRst == null)
            closeStr = ""
        else if (closeStr.equals("none", ignoreCase = true))
            closeStr = ""
        val customArduino = Arduino("Custom", mcu, arduinoBoard.baudRate, protocol)
        if (!TextUtils.isEmpty(preOpenStr)) customArduino.preOpenResetBehavior = preOpenStr
        if (!TextUtils.isEmpty(postOpenStr)) customArduino.postOpenResetBehavior = postOpenStr
        if (!TextUtils.isEmpty(closeStr)) customArduino.closeResetBehavior = closeStr
        if (protocol == Protocol.Avr109) customArduino.sleepAfterOpen = 0 else customArduino.sleepAfterOpen = 250
        val logger: IArduinoUploaderLogger = object : IArduinoUploaderLogger {
            override fun Error(message: String, exception: Exception) {
                Log.e(TAG, "Error:$message")
                logUI("Error:$message")
            }

            override fun Warn(message: String) {
                Log.w(TAG, "Warn:$message")
                logUI("Warn:$message")
            }

            override fun Info(message: String) {
                Log.i(TAG, "Info:$message")
                logUI("Info:$message")
            }

            override fun Debug(message: String) {
                Log.d(TAG, "Debug:$message")
                logUI("Debug:$message")
            }

            override fun Trace(message: String) {
                Log.d(TAG, "Trace:$message")
                logUI("Trace:$message")
            }
        }
        val progress = IProgress<Double?> { value ->
            val result = String.format("Upload progress: %1$,3.2f%%", value * 100)
            Log.d(TAG, result)
            logUI("Procees:$result")
        }
        try {
            val file = assets.open("Blink.uno.hex")
            val reader: Reader = InputStreamReader(file)
            val hexFileContents = LineReader(reader).readLines()
            val uploader =
                ArduinoSketchUploader(this@MainActivity, SerialPortStreamImpl::class.java, null, logger, progress)
//            ArduinoSketchUploader<SerialPortStreamImpl> uploader = new ArduinoSketchUploader<SerialPortStreamImpl>(this@MainActivity, null, logger, progress) {
//                //Ananymous
//            };
            uploader.UploadSketch(hexFileContents, customArduino, deviceKeyName)
        } catch (ex: ArduinoUploaderException) {
            ex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun logUI(text: String) {
        runOnUiThread { binding.display.append("$text\n") }
    }

    private inner class UploadRunnable : Runnable {
        override fun run() {
            uploadHex()
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
