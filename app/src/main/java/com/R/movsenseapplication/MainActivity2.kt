package com.R.movsenseapplication

import MyScanResult2
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.movesense.mds.Mds
import com.movesense.mds.MdsConnectionListener
import com.movesense.mds.MdsException
import com.movesense.mds.MdsNotificationListener
import com.movesense.mds.MdsSubscription
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.disposables.Disposable

class MainActivity2 : AppCompatActivity(), AdapterView.OnItemLongClickListener,
    AdapterView.OnItemClickListener {
    private var mMds: Mds? = null

    // UI
    private var mScanResultListView: ListView? = null
    private val mScanResArrayList = ArrayList<MyScanResult2>()
    private var mScanResArrayAdapter: ArrayAdapter<MyScanResult2>? = null
    private var mdsSubscription: MdsSubscription? = null
    private var subscribedDeviceSerial: String? = null
       override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        // Init Scan UI
        mScanResultListView = findViewById<View>(R.id.listScanResult) as ListView?
        mScanResArrayAdapter = ArrayAdapter<MyScanResult2>(
            this,
            android.R.layout.simple_list_item_1, mScanResArrayList
        )
        mScanResultListView!!.adapter = mScanResArrayAdapter
        mScanResultListView!!.setOnItemLongClickListener(this)
        mScanResultListView!!.setOnItemClickListener(this)

        // Make sure we have all the permissions this app needs
        requestNeededPermissions()

        // Initialize Movesense MDS library
        initMds()
    }

    private val bleClient: RxBleClient?
        private get() {
            // Init RxAndroidBle (Ble helper library) if not yet initialized
            if (mBleClient == null) {
                mBleClient = RxBleClient.create(this)
            }
            return mBleClient
        }

    private fun initMds() {
        mMds = Mds.builder().build(this)
    }

    fun requestNeededPermissions() {
        var requiredPermissions: Array<String?>

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(
                this, arrayOf<String>(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                ),
                MY_PERMISSIONS_REQUEST_LOCATION
            )
        }
    }

    var mScanSubscription: Disposable? = null
    fun onScanClicked(view: View?) {
        findViewById<View>(R.id.buttonScan).setVisibility(View.GONE)
        findViewById<View>(R.id.buttonScanStop).setVisibility(View.VISIBLE)

        // Start with empty list
        mScanResArrayList.clear()
        mScanResArrayAdapter!!.notifyDataSetChanged()
        mScanSubscription = bleClient!!.scanBleDevices(
            ScanSettings.Builder()// .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // change if needed
                // .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // change if needed
                .build() // add filters if needed
        )
            .subscribe(
                { scanResult ->
                    Log.d(LOG_TAG, "scanResult: $scanResult")
                    // Process scan result here. filter movesense devices.
                    if (scanResult.getBleDevice() != null && scanResult.getBleDevice()
                            .getName() != null &&
                        scanResult.getBleDevice().getName()!!.startsWith("Movesense")
                    ) {

                        // replace if exists already, add otherwise
                        val msr = MyScanResult2(scanResult)
                        if (mScanResArrayList.contains(msr)) mScanResArrayList[mScanResArrayList.indexOf(
                            msr
                        )] = msr else mScanResArrayList.add(0, msr)
                        mScanResArrayAdapter!!.notifyDataSetChanged()
                    }
                }
            ) { throwable ->
                Log.e(LOG_TAG, "scan error: $throwable")
                // Handle an error here.

                // Re-enable scan buttons, just like with ScanStop
                onScanStopClicked(null)
            }
    }

    fun onScanStopClicked(view: View?) {
        if (mScanSubscription != null) {
            mScanSubscription!!.dispose()
            mScanSubscription = null
        }
        findViewById<View>(R.id.buttonScan).setVisibility(View.VISIBLE)
        findViewById<View>(R.id.buttonScanStop).setVisibility(View.GONE)
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        if (position < 0 || position >= mScanResArrayList.size) return
        val device = mScanResArrayList[position]
        if (!device.isConnected()) {
            // Stop scanning
            onScanStopClicked(null)

            // And connect to the device
            connectBLEDevice(device)
        } else {
            // Device is connected, trigger showing /Info
            subscribeToSensor(device.connectedSerial!!)
        }
    }

    private fun subscribeToSensor(connectedSerial: String) {
        // Clean up existing subscription (if there is one)
        if (mdsSubscription != null) {
            unsubscribe()
        }

        // Build JSON doc that describes what resource and device to subscribe
        // Here we subscribe to 13 hertz accelerometer data
        val sb = StringBuilder()
        val strContract =
            sb.append("{\"Uri\": \"").append(connectedSerial).append(URI_MEAS_ACC_13).append("\"}")
                .toString()
        Log.d(LOG_TAG, strContract)
        val sensorUI: View = findViewById<View>(R.id.sensorUI)
        subscribedDeviceSerial = connectedSerial
        mdsSubscription = Mds.builder().build(this).subscribe(
            URI_EVENTLISTENER,
            strContract, object : MdsNotificationListener {
                override fun onNotification(data: String) {
                    Log.d(LOG_TAG, "onNotification(): $data")

                    // If UI not enabled, do it now
                    if (sensorUI.visibility == View.GONE) sensorUI.visibility = View.VISIBLE
                    val accResponse: AccDataResponse2 =
                        Gson().fromJson(data, AccDataResponse2::class.java)
                    if (accResponse != null && accResponse.body.array.size > 0) {
                        val accStr = java.lang.String.format(
                            "%.02f, %.02f, %.02f",
                            accResponse.body.array.get(0).x,
                            accResponse.body.array.get(0).y,
                            accResponse.body.array.get(0).z
                        )
                        (findViewById<View>(R.id.sensorMsg) as TextView).text = accStr
                    }
                }

                override fun onError(error: MdsException) {
                    Log.e(LOG_TAG, "subscription onError(): ", error)
                    unsubscribe()
                }
            })
    }


    override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
        if (position < 0 || position >= mScanResArrayList.size)
            return false
        val device = mScanResArrayList[position]

        Log.d(LOG_TAG, "onItemLongClick, ${device.connectedSerial} vs $subscribedDeviceSerial")
        if (device.connectedSerial == subscribedDeviceSerial)
            unsubscribe()
        Log.i(LOG_TAG, "Disconnecting from BLE device: ${device.macAddress}")
        mMds!!.disconnect(device.macAddress)
        return true
    }

    private fun connectBLEDevice(device: MyScanResult2) {
        val bleDevice: RxBleDevice = bleClient!!.getBleDevice(device.macAddress)
        Log.i(LOG_TAG, "Connecting to BLE device: " + bleDevice.getMacAddress())
        mMds!!.connect(bleDevice.getMacAddress(), object : MdsConnectionListener {
            override fun onConnect(s: String) {
                Log.d(LOG_TAG, "onConnect:$s")
            }

            override fun onConnectionComplete(macAddress: String, serial: String) {
                for (sr in mScanResArrayList) {
                    if (sr.macAddress.equals(macAddress)) {
                        sr.markConnected(serial)
                        break
                    }
                }
                mScanResArrayAdapter!!.notifyDataSetChanged()
            }

            override fun onError(e: MdsException) {
                Log.e(LOG_TAG, "onError:$e")
                showConnectionError(e)
            }

            override fun onDisconnect(bleAddress: String) {
                Log.d(LOG_TAG, "onDisconnect: $bleAddress")
                for (sr in mScanResArrayList) {
                    if (bleAddress == sr.macAddress) {
                        if (sr.connectedSerial != null && sr.connectedSerial == subscribedDeviceSerial) unsubscribe()
                        sr.markDisconnected()
                    }
                }
                mScanResArrayAdapter!!.notifyDataSetChanged()
            }
        })
    }

    private fun showConnectionError(e: MdsException) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            .setTitle("Connection Error:")
            .setMessage(e.message)
        builder.create().show()
    }

    private fun unsubscribe() {
        if (mdsSubscription != null) {
            mdsSubscription!!.unsubscribe()
            mdsSubscription = null
        }
        subscribedDeviceSerial = null

        // If UI not invisible, do it now
        val sensorUI: View = findViewById<View>(R.id.sensorUI)
        if (sensorUI.visibility != View.GONE) sensorUI.visibility = View.GONE
    }

    fun onUnsubscribeClicked(view: View?) {
        unsubscribe()
    }

    companion object {
        private val LOG_TAG = MainActivity2::class.java.simpleName
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 1
        const val URI_CONNECTEDDEVICES = "suunto://MDS/ConnectedDevices"
        const val URI_EVENTLISTENER = "suunto://MDS/EventListener"
        const val SCHEME_PREFIX = "suunto://"

        // BleClient singleton
        private var mBleClient: RxBleClient? = null

        // Sensor subscription
        private const val URI_MEAS_ACC_13 = "/Meas/Acc/13"
    }
}