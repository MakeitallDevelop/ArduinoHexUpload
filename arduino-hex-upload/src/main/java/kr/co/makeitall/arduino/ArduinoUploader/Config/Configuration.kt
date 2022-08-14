package kr.co.makeitall.arduino.ArduinoUploader.Config;

public class Configuration {
    private Arduino[] Arduinos;

    public final Arduino[] getArduinos() {
        return Arduinos;
    }

    public final void setArduinos(Arduino[] value) {
        Arduinos = value;
    }
}
