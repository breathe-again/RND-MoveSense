
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.scan.ScanResult

class MyScanResult2(scanResult: ScanResult) {
    var rssi: Int = 0
    var macAddress: String = ""
    var name: String = ""
    var connectedSerial: String? = null

    init {
        macAddress = scanResult.bleDevice.macAddress
        rssi = scanResult.rssi
        name = scanResult.bleDevice.name!!
    }

    fun isConnected(): Boolean {
        return connectedSerial != null
    }

    fun markConnected(serial: String) {
        connectedSerial = serial
    }

    fun markDisconnected() {
        connectedSerial = null
    }

    override fun equals(other: Any?): Boolean {
        if (other is MyScanResult2 && other.macAddress == this.macAddress) {
            return true
        } else if (other is RxBleDevice && other.macAddress == this.macAddress) {
            return true
        } else {
            return false
        }
    }

    override fun toString(): String {
        return (if (isConnected()) "*** " else "") + macAddress + " - " + name + " [" + rssi + "]" + (if (isConnected()) " ***" else "")
    }
}

