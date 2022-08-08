package kr.co.makeitall.arduino

class UsbSerialException : RuntimeException {
    private val type: Int

    constructor(type: Int) {
        this.type = type
    }

    constructor(type: Int, throwable: Throwable?) : super(throwable) {
        this.type = type
    }

    companion object {
        const val USB_ERROR_NOT_SUPPORTED = 0
        const val USB_ERROR_NO_USB = 1
        const val USB_ERROR_DRIVER_NOT_WORKING = 2
        const val USB_ERROR_CDC_DRIVER_NOT_WORKING = 3
    }
}
