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
        const val TYPE_NOT_CONNECTED = 0
        const val TYPE_NEEDS_PERMISSION = 1
        const val TYPE_COMMUNICATION_ERROR = 2
        const val TYPE_READ_TIMEOUT_ERROR = 3
        const val TYPE_WRITE_TIMEOUT_ERROR = 4
        const val TYPE_UNEXPECTED_RESPONSE = 5
    }
}
