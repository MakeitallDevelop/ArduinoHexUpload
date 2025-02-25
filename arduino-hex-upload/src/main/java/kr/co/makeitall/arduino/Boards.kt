package kr.co.makeitall.arduino

import kr.co.makeitall.arduino.ArduinoUploader.Config.McuIdentifier
import kr.co.makeitall.arduino.ArduinoUploader.Config.Protocol

enum class Boards(
    val idText: String,
    val chipType: McuIdentifier,
    val uploadProtocol: Protocol,
    val uploadBaudRate: Int,
    val comProtocol: Int,
    val boardName: String,
    val openReset: String,
    val closeReset: String,
    val postReset: String
) {
    // Arduino Series
    ARDUINO_UNO("auno", McuIdentifier.AtMega328P, Protocol.Stk500v1, 115200, ComProtocols.UART, "Arduino Uno", "DTR;true", "DTR-RTS;50;250;false", ""),
    ARDUINO_DUEMILANOVE_328("duem", McuIdentifier.AtMega328P, Protocol.Stk500v1, 57600, ComProtocols.UART, "Arduino Duemilanove ATmega328", "DTR;true", "DTR-RTS;250;50", ""),
    ARDUINO_DUEMILANOVE_168("diec", McuIdentifier.AtMega168, Protocol.Stk500v1, 19200, ComProtocols.UART, "Arduino Diecimila or Duemilanove ATmega168", "DTR;true", "DTR-RTS;250;50", ""),
    ARDUINO_NANO_328("na32", McuIdentifier.AtMega328P, Protocol.Stk500v1, 57600, ComProtocols.UART, "Arduino Nano ATmega328", "DTR;true", "DTR-RTS;250;50", ""),
    ARDUINO_NANO_168("na16", McuIdentifier.AtMega168, Protocol.Stk500v1, 57600, ComProtocols.UART, "Arduino Nano ATmega168", "DTR;true", "DTR-RTS;250;50", ""),
    ARDUINO_MEGA_2560_ADK("mg25", McuIdentifier.AtMega2560, Protocol.Stk500v2, 115200, ComProtocols.UART, "Arduino Mega 2560 or ADK", "DTR-RTS;50;250;true", "", "DTR-RTS;250;50;true"),
//    ARDUINO_MEGA_1280( "mg16", ChipTypes.M1280, Protocol.Stk500v1, 57600, ComProtocols.UART, "Arduino Mega (ATmega1280)"),
    ARDUINO_LEONARD("leon", McuIdentifier.AtMega32U4, Protocol.Avr109, 57600, ComProtocols.UART, "Arduino Leonardo", "1200bps", "", ""),
    ARDUINO_ESPLORA("espl", McuIdentifier.AtMega32U4, Protocol.Avr109, 57600, ComProtocols.UART, "Arduino Esplora", "1200bps", "", ""),
    ARDUINO_MICRO("micr", McuIdentifier.AtMega32U4, Protocol.Avr109, 57600, ComProtocols.UART, "Arduino Micro", "1200bps", "", ""),
    ARDUINO_MINI_328("mn32", McuIdentifier.AtMega328P, Protocol.Stk500v1, 57600, ComProtocols.UART, "Arduino Mini ATmega328", "DTR;true", "DTR-RTS;250;50", ""),
    ARDUINO_MINI_168("mn16", McuIdentifier.AtMega168, Protocol.Stk500v1, 57600, ComProtocols.UART, "Arduino Mini ATmega168", "DTR;true", "DTR-RTS;250;50", ""),
    ARDUINO_ETHERNET("ethe", McuIdentifier.AtMega328P, Protocol.Stk500v1, 115200, ComProtocols.UART, "Arduino Ethernet", "DTR;true", "DTR-RTS;50;250;false", ""),
    ARDUINO_FIO("afio", McuIdentifier.AtMega328P, Protocol.Stk500v1, 57600, ComProtocols.UART, "Arduino Fio", "DTR;true", "DTR-RTS;250;50", ""),
    ARDUINO_BT_328("bt32", McuIdentifier.AtMega328P, Protocol.Stk500v1, 19200, ComProtocols.UART, "Arduino BT ATmega328", "DTR;true", "DTR-RTS;250;50", ""),
    ARDUINO_BT_168("bt16", McuIdentifier.AtMega168, Protocol.Stk500v1, 19200, ComProtocols.UART, "Arduino BT ATmega168", "DTR;true", "DTR-RTS;250;50", ""),
    ARDUINO_LILYPAD_USB("lpus", McuIdentifier.AtMega32U4, Protocol.Avr109, 57600, ComProtocols.UART, "LilyPad Arduino USB", "1200bps", "", ""),
    ARDUINO_LILYPAD_328("lp32", McuIdentifier.AtMega328P, Protocol.Stk500v1, 57600, ComProtocols.UART, "LilyPad Arduino ATmega328", "DTR;true", "DTR-RTS;250;50", ""),
    ARDUINO_LILYPAD_168("lp16", McuIdentifier.AtMega168, Protocol.Stk500v1, 19200, ComProtocols.UART, "LilyPad Arduino ATmega168", "DTR;true", "DTR-RTS;250;50", ""),
    ARDUINO_PRO_5V_328("pm53", McuIdentifier.AtMega328P, Protocol.Stk500v1, 57600, ComProtocols.UART, "Arduino Pro or Pro Mini (5V, 16MHz) ATmega328", "DTR;true", "DTR-RTS;250;50", ""),
    ARDUINO_PRO_5V_168("pm51", McuIdentifier.AtMega168, Protocol.Stk500v1, 19200, ComProtocols.UART, "Arduino Pro or Pro Mini (5V, 16MHz) ATmega168", "DTR;true", "DTR-RTS;250;50", ""),
    ARDUINO_PRO_33V_328("pm33", McuIdentifier.AtMega328P, Protocol.Stk500v1, 57600, ComProtocols.UART, "Arduino Pro or Pro Mini (3.3V, 8MHz) ATmega328", "DTR;true", "DTR-RTS;250;50", ""),
    ARDUINO_PRO_33V_168("pm31", McuIdentifier.AtMega168, Protocol.Stk500v1, 19200, ComProtocols.UART, "Arduino Pro or Pro Mini (3.3V, 8MHz) ATmega168", "DTR;true", "DTR-RTS;250;50", ""),
    ARDUINO_NG_168("ng16", McuIdentifier.AtMega168, Protocol.Stk500v1, 19200, ComProtocols.UART, "Arduino NG or older ATmega168", "DTR;true", "DTR-RTS;250;50", ""),
//    ARDUINO_NG_8("ng08", ChipTypes.M8, Protocol.Stk500v1, 19200, ComProtocols.UART, "Arduino NG or older ATmega8"),
    BALANDUINO("bala", McuIdentifier.AtMega1284, Protocol.Stk500v1, 115200, ComProtocols.UART, "Balanduino", "DTR;true", "DTR-RTS;250;50", ""),
    POCKETDUINO("podu", McuIdentifier.AtMega328P, Protocol.Stk500v1, 57600, ComProtocols.UART, "PocketDuino", "DTR;true", "DTR-RTS;250;50", "");

    companion object {
        @JvmStatic
        fun fromName(boardName: String): Boards =
            when (boardName) {
                ARDUINO_UNO.boardName -> ARDUINO_UNO
                ARDUINO_DUEMILANOVE_328.boardName -> ARDUINO_DUEMILANOVE_328
                ARDUINO_DUEMILANOVE_168.boardName -> ARDUINO_DUEMILANOVE_168
                ARDUINO_NANO_328.boardName -> ARDUINO_NANO_328
                ARDUINO_NANO_168.boardName -> ARDUINO_NANO_168
                ARDUINO_MEGA_2560_ADK.boardName -> ARDUINO_MEGA_2560_ADK
                ARDUINO_LEONARD.boardName -> ARDUINO_LEONARD
                ARDUINO_ESPLORA.boardName -> ARDUINO_ESPLORA
                ARDUINO_MICRO.boardName -> ARDUINO_MICRO
                ARDUINO_MINI_328.boardName -> ARDUINO_MINI_328
                ARDUINO_MINI_168.boardName -> ARDUINO_MINI_168
                ARDUINO_ETHERNET.boardName -> ARDUINO_ETHERNET
                ARDUINO_FIO.boardName -> ARDUINO_FIO
                ARDUINO_BT_328.boardName -> ARDUINO_BT_328
                ARDUINO_BT_168.boardName -> ARDUINO_BT_168
                ARDUINO_LILYPAD_USB.boardName -> ARDUINO_LILYPAD_USB
                ARDUINO_LILYPAD_328.boardName -> ARDUINO_LILYPAD_328
                ARDUINO_LILYPAD_168.boardName -> ARDUINO_LILYPAD_168
                ARDUINO_PRO_5V_328.boardName -> ARDUINO_PRO_5V_328
                ARDUINO_PRO_5V_168.boardName -> ARDUINO_PRO_5V_168
                ARDUINO_PRO_33V_328.boardName -> ARDUINO_PRO_33V_328
                ARDUINO_PRO_33V_168.boardName -> ARDUINO_PRO_33V_168
                ARDUINO_NG_168.boardName -> ARDUINO_NG_168
                BALANDUINO.boardName -> BALANDUINO
                POCKETDUINO.boardName -> POCKETDUINO
                else -> throw IllegalStateException("Unknown boardName")
            }
    }

    object ComProtocols {
        const val UART = 1
        const val I2C = 2
        const val SPI = 3
        const val USYNC_FIFO = 4
        const val SYNC_FIFO = 5
    }
}
