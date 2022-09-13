package xyz.vidieukhien.embedded.arduinohexuploadexample

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import kr.co.makeitall.arduino.ArduinoUploader.ArduinoSketchUploader
import kr.co.makeitall.arduino.ArduinoUploader.ArduinoUploaderException
import kr.co.makeitall.arduino.ArduinoUploader.Config.Arduino
import kr.co.makeitall.arduino.ArduinoUploader.IArduinoUploaderLogger
import kr.co.makeitall.arduino.Boards
import kr.co.makeitall.arduino.CSharpStyle.IProgress
import kr.co.makeitall.arduino.LineReader
import kr.co.makeitall.arduino.SerialPortStreamImpl
import kr.co.makeitall.arduino.UsbSerialManager
import kr.co.makeitall.arduino.UsbSerialManager.UsbState
import xyz.vidieukhien.embedded.arduinohexuploadexample.databinding.ActivityMainBinding
import java.io.InputStreamReader
import java.io.Reader

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    private lateinit var binding: ActivityMainBinding

    private lateinit var usbSerialManager: UsbSerialManager

    private var deviceKeyName: String? = null

    private fun usbConnectChange(@UsbState state: Int) {
        if (state == UsbSerialManager.USB_STATE_DISCONNECTED) {
            binding.progressBar.visibility = View.INVISIBLE
            binding.fab.hide()
        } else if (state == UsbSerialManager.USB_STATE_CONNECTED) {
            binding.progressBar.visibility = View.VISIBLE
        }
    }

    private fun usbPermissionGranted(usbKey: String) {
        Toast.makeText(this@MainActivity, "UsbPermissionGranted:$usbKey", Toast.LENGTH_SHORT).show()
        binding.portSelect.text = usbKey
        deviceKeyName = usbKey
        binding.fab.show()

        usbSerialManager.addOnUsbReadListener(9600) { data ->
            Log.i(TAG, "data: $data")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this@MainActivity, R.layout.activity_main)

        usbSerialManager = UsbSerialManager(this@MainActivity, lifecycle)
        usbSerialManager
            .addOnStateListener { usbDevice, state ->
                when (state) {
                    UsbSerialManager.USB_STATE_READY -> {

                    }

                    UsbSerialManager.USB_STATE_CONNECTED -> {
                        Toast.makeText(this@MainActivity, "USB connected", Toast.LENGTH_SHORT).show()
//                        usbConnectChange(state)
                        usbSerialManager.requestPermission(usbDevice)
                    }

                    UsbSerialManager.USB_STATE_DISCONNECTED -> {
                        Toast.makeText(this@MainActivity, "USB disconnected", Toast.LENGTH_SHORT).show()
                        usbConnectChange(state)
                    }
                }
            }
            .addOnPermissionListener { usbDevice, permission ->
                when (permission) {
                    UsbSerialManager.USB_PERMISSION_GRANTED -> {
                        usbPermissionGranted(usbDevice.deviceName)
                        Toast.makeText(this@MainActivity, "USB permission granted", Toast.LENGTH_SHORT).show()
                    }

                    UsbSerialManager.USB_PERMISSION_DENIED -> {
                        Toast.makeText(this@MainActivity, "USB Permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnErrorListener { usbDevice, e ->
                e.printStackTrace()
            }

        setSupportActionBar(binding.toolbar)

        binding.fab.setOnClickListener {
            Thread { uploadHex() }.start()
        }
    }

    override fun onResume() {
        super.onResume()
        usbSerialManager.updateDevice()
        usbSerialManager.usbDevice?.let { device ->
            if (usbSerialManager.hasPermission(device)) {
                usbPermissionGranted(device.deviceName)
                Log.d(TAG, "permission")
            } else {
                usbSerialManager.requestPermission(device)
                Log.w(TAG, "no permission")
            }
        } ?: run {
            Log.w(TAG, "usbDevice null")
        }
    }

    private fun uploadHex() {
        usbSerialManager.removeOnUsbReadListener()

        val arduinoBoard = Arduino(Boards.ARDUINO_UNO)

        val logger: IArduinoUploaderLogger = object : IArduinoUploaderLogger {
            override fun onError(message: String, exception: Exception) {
                logUI("Error:$message")
            }

            override fun onWarn(message: String) {
                logUI("Warn:$message")
            }

            override fun onInfo(message: String) {
                logUI("Info:$message")
            }

            override fun onDebug(message: String) {
                logUI("Debug:$message")
            }

            override fun onTrace(message: String) {
                logUI("Trace:$message")
            }
        }
        val progress = IProgress<Double> { value ->
            binding.progressBar.progress = (value * 100).toInt()
        }
        try {
            val file = assets.open("Blink.uno.hex")
            val reader: Reader = InputStreamReader(file)
            val hexFileContents = LineReader(reader).readLines()
            val uploader =
                ArduinoSketchUploader(this@MainActivity, SerialPortStreamImpl::class.java, null, logger, progress)
            deviceKeyName?.let { uploader.uploadSketch(hexFileContents, arduinoBoard, it) }
        } catch (ex: ArduinoUploaderException) {
            ex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        binding.progressBar.progress = 100

        usbSerialManager.addOnUsbReadListener(9600) { data ->
            Log.i(TAG, "data: $data")
        }
    }

    private fun logUI(text: String) {
        runOnUiThread { binding.display.append("$text\n") }
    }
}
