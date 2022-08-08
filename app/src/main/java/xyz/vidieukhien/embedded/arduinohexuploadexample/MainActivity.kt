package xyz.vidieukhien.embedded.arduinohexuploadexample

import ArduinoUploader.ArduinoSketchUploader
import ArduinoUploader.ArduinoUploaderException
import ArduinoUploader.Config.Arduino
import ArduinoUploader.Config.McuIdentifier
import ArduinoUploader.Config.Protocol
import ArduinoUploader.IArduinoUploaderLogger
import CSharpStyle.IProgress
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import kr.co.makeitall.arduino.Boards
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
        usbSerialManager.usbDevice?.let { device ->
            if (usbSerialManager.hasPermission(device)) {
                usbPermissionGranted(device.deviceName)
            } else {
                usbSerialManager.requestPermission(device)
            }
        }
    }

    fun uploadHex() {
        val board = Boards.ARDUINO_UNO
        val arduinoBoard = Arduino(board.boardName, board.chipType, board.uploadBaudRate, board.uploadProtocol)
        val protocol = Protocol.valueOf(arduinoBoard.protocol.name)
        val mcu = McuIdentifier.valueOf(arduinoBoard.mcu.name)

        val preOpenRst = arduinoBoard.preOpenResetBehavior?.let {
            if (it.equals("none", ignoreCase = true)) "" else it
        } ?: ""

        val postOpenRst = arduinoBoard.postOpenResetBehavior?.let {
            if (it.equals("none", ignoreCase = true)) "" else it
        } ?: ""

        val closeRst = arduinoBoard.closeResetBehavior?.let {
            if (it.equals("none", ignoreCase = true)) "" else it
        } ?: ""

        val customArduino = Arduino("Custom", mcu, arduinoBoard.baudRate, protocol)
        if (preOpenRst.isNotEmpty()) customArduino.preOpenResetBehavior = preOpenRst
        if (postOpenRst.isNotEmpty()) customArduino.postOpenResetBehavior = postOpenRst
        if (closeRst.isNotEmpty()) customArduino.closeResetBehavior = closeRst
        customArduino.sleepAfterOpen = if (protocol == Protocol.Avr109) 0 else 250
        val logger: IArduinoUploaderLogger = object : IArduinoUploaderLogger {
            override fun Error(message: String, exception: Exception) {
                logUI("Error:$message")
            }

            override fun Warn(message: String) {
                logUI("Warn:$message")
            }

            override fun Info(message: String) {
                logUI("Info:$message")
            }

            override fun Debug(message: String) {
                logUI("Debug:$message")
            }

            override fun Trace(message: String) {
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
//            ArduinoSketchUploader<SerialPortStreamImpl> uploader = new ArduinoSketchUploader<SerialPortStreamImpl>(this@MainActivity, null, logger, progress) {
//                //Ananymous
//            };
            uploader.UploadSketch(hexFileContents, customArduino, deviceKeyName)
        } catch (ex: ArduinoUploaderException) {
            ex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        binding.progressBar.progress = 100
    }

    private fun logUI(text: String) {
        runOnUiThread { binding.display.append("$text\n") }
    }
}
