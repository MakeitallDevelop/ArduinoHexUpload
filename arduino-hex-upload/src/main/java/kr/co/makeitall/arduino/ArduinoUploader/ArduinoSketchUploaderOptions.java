package kr.co.makeitall.arduino.ArduinoUploader;

public class ArduinoSketchUploaderOptions {
    private String FileName;


    public final String getFileName() {
        return FileName;
    }


    public final void setFileName(String value) {
        FileName = value;
    }

    private String PortName;

    public final String getPortName() {
        return PortName;
    }

    public final void setPortName(String value) {
        PortName = value;
    }

    private kr.co.makeitall.arduino.ArduinoUploader.Hardware.ArduinoModel ArduinoModel = getArduinoModel().values()[0];

    public final kr.co.makeitall.arduino.ArduinoUploader.Hardware.ArduinoModel getArduinoModel() {
        return ArduinoModel;
    }

    public final void setArduinoModel(kr.co.makeitall.arduino.ArduinoUploader.Hardware.ArduinoModel value) {
        ArduinoModel = value;
    }
}
