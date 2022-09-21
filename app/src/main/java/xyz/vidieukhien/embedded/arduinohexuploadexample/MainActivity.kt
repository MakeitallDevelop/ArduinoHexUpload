package xyz.vidieukhien.embedded.arduinohexuploadexample

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.co.makeitall.arduino.ArduinoUploader.ArduinoSketchUploader
import kr.co.makeitall.arduino.ArduinoUploader.Config.Arduino
import kr.co.makeitall.arduino.ArduinoUploader.IArduinoUploaderLogger
import kr.co.makeitall.arduino.Boards
import kr.co.makeitall.arduino.CSharpStyle.IProgress
import kr.co.makeitall.arduino.LineReader
import kr.co.makeitall.arduino.SerialPortStreamImpl
import kr.co.makeitall.arduino.UsbSerialManager
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

    private fun usbPermissionGranted(usbKey: String) {
        binding.portSelect.text = usbKey
        deviceKeyName = usbKey
        binding.fab.show()
        binding.fab2.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this@MainActivity, R.layout.activity_main)

        usbSerialManager = UsbSerialManager(this@MainActivity, lifecycle)
        usbSerialManager
            .addOnUsbReadListener() { data ->
                Log.i(TAG, "data: $data")
            }
            .addOnStateListener { state ->
                when (state) {
                    UsbSerialManager.USB_STATE_READY -> {
                        usbSerialManager.usbDevice?.let { usbPermissionGranted(it.deviceName) }
                        usbSerialManager.startRead()
                        Toast.makeText(this@MainActivity, "USB ready", Toast.LENGTH_SHORT).show()
                    }

                    UsbSerialManager.USB_STATE_CONNECTED -> {
                        binding.progressBar.visibility = View.VISIBLE
                        Toast.makeText(this@MainActivity, "USB connected", Toast.LENGTH_SHORT).show()
                    }

                    UsbSerialManager.USB_STATE_DISCONNECTED -> {
                        binding.progressBar.visibility = View.INVISIBLE
                        binding.fab.hide()
                        binding.fab2.hide()
                        Toast.makeText(this@MainActivity, "USB disconnected", Toast.LENGTH_SHORT).show()
                    }

                    UsbSerialManager.USB_STATE_READ_START -> {
                        Toast.makeText(this@MainActivity, "USB read start", Toast.LENGTH_SHORT).show()
                    }

                    UsbSerialManager.USB_STATE_READ_END -> {
                        Toast.makeText(this@MainActivity, "USB read end", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnErrorListener { e ->
                e.printStackTrace()
            }

        setSupportActionBar(binding.toolbar)

        binding.fab.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                uploadHex("hello.hex")
            }
        }
        binding.fab2.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                uploadHex("echo.hex")
            }
        }

        binding.button.setOnClickListener {
//            usbSerialManager.write("10")
            usbSerialManager.updateBaudRate(9600)
        }
    }

    private suspend fun uploadHex(fileName: String) {
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

        withContext(Dispatchers.Default) {
            usbSerialManager.stopRead()

            try {
                val file = assets.open(fileName)
                val reader: Reader = InputStreamReader(file)
                val hexFileContents = LineReader(reader).readLines()
                val uploader =
                    ArduinoSketchUploader(this@MainActivity, SerialPortStreamImpl::class.java, null, logger, progress)
                deviceKeyName?.let { uploader.uploadSketch(hexFileContents, arduinoBoard, it) }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            binding.progressBar.progress = 100

            usbSerialManager.startRead()
        }
    }

    private fun logUI(text: String) {
        runOnUiThread { binding.display.append("$text\n") }
    }
}
