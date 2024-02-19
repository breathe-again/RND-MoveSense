package com.R.movsenseapplication

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.movesense.mds.Mds
import com.movesense.mds.MdsConnectionListener
import com.movesense.mds.MdsException
import com.polidea.rxandroidble3.RxBleClient
import com.polidea.rxandroidble3.RxBleConnection
import com.polidea.rxandroidble3.RxBleDevice
import com.polidea.rxandroidble3.exceptions.BleException
import com.polidea.rxandroidble3.scan.ScanSettings
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {
    private var mMds: Mds? = null

    private lateinit var mScanSubscription: Disposable
    private lateinit var mScanResultListView: ListView
    private lateinit var mScanResArrayList: ArrayList<MyScanResult>
    private lateinit var mScanResArrayAdapter: ArrayAdapter<MyScanResult>
    private lateinit var rxBleClient: RxBleClient
    private lateinit var buttonScan: Button
    private lateinit var buttonScanStop: Button
    private val LOCATION_PERMISSION_REQUEST_CODE = 100
    private val BLUETOOTH_PERMISSION_REQUEST_CODE = 101


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initMds()
        mScanResultListView = findViewById(R.id.listScanResult)
        buttonScan = findViewById(R.id.buttonScan)
        buttonScanStop = findViewById(R.id.buttonScanStop)
        mScanResArrayList = ArrayList()
        mScanResArrayAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, mScanResArrayList)
        mScanResultListView.adapter = mScanResArrayAdapter
        mScanResultListView.setOnItemClickListener { parent, view, position, id ->
            onItemClick(
                parent,
                view,
                position,
                id
            )
        }
        mScanResultListView.setOnItemLongClickListener { parent, view, position, id ->
            onItemLongClick(
                parent,
                view,
                position,
                id
            )
        }

        buttonScan.setOnClickListener {
            findViewById<View>(R.id.buttonScan).visibility = View.GONE
            findViewById<View>(R.id.buttonScanStop).visibility = View.VISIBLE
            onScanClicked()
        }
        buttonScanStop.setOnClickListener {
           findViewById<View>(R.id.buttonScan).visibility = View.VISIBLE
            findViewById<View>(R.id.buttonScanStop).visibility = View.GONE
            //onScanStopClicked()

           testConnect()
        }
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        // Check and request Bluetooth permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH)
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.NEARBY_WIFI_DEVICES
                ),
                BLUETOOTH_PERMISSION_REQUEST_CODE
            )
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Location permission granted
                } else {
                    // Location permission denied
                }
            }

            BLUETOOTH_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // Bluetooth permissions granted
                } else {
                    // Bluetooth permissions denied
                }
            }
        }
    }

    private fun initMds() {
        mMds = Mds.builder().build(this)
    }


    fun onScanClicked() {
        // Start with empty list
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
//        //requesting permission only for Android 12 and above
//        AndroidManifest.permission.BLUETOOTH_SCAN
//        Manifest.permission.BLUETOOTH_CONNECT,
//        Manifest.permission.BLUETOOTH_ADVERTISE,
//    }
//    await Permission.bluetoothConnect.request().isGrant
        mScanResArrayList.clear()
        mScanResArrayAdapter.notifyDataSetChanged()
        rxBleClient = RxBleClient.create(this)


        mScanSubscription = rxBleClient.scanBleDevices(
            ScanSettings.Builder()
//             .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // change if needed
//             .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // change if needed
                .build() // add filters if needed
        )
            .subscribe(
                { scanResult ->

                    Log.d(
                        "LOG_TAG",
                        "mac: ${scanResult.bleDevice.macAddress}" + "name: ${scanResult.bleDevice.name}"
                    )
                    scanResult.bleDevice?.let { bleDevice ->
                        Log.d(
                            "LOG_TAG",
                            "mac: ${scanResult.bleDevice.macAddress}" + "name: ${scanResult.bleDevice.name}" + "name2: ${bleDevice.name}"
                        )
                        if (bleDevice.name != null && bleDevice.name!!.contains("Movesense")) {
                            val msr = MyScanResult(scanResult)
                            if (mScanResArrayList.contains(msr))
                                mScanResArrayList[mScanResArrayList.indexOf(msr)] = msr
                            else
                                mScanResArrayList.add(0, msr)
                            //mScanResArrayList.add(0, scanResult)

//                        mScanResArrayList.add(0, msr)
                            mScanResArrayAdapter.notifyDataSetChanged()
                            onScanStopClicked()
                        }
                    }


                    //Log.d("LOG_TAG", "scanResult: $scanResult")
//                scanResult.bleDevice?.let { bleDevice ->
//                    if (bleDevice.name != null && bleDevice.name!!.startsWith("Movesense")) {
//                        val msr = MyScanResult(scanResult)
//                        if (mScanResArrayList.contains(msr))
//                            mScanResArrayList[mScanResArrayList.indexOf(msr)] = msr
//                        else
//                            mScanResArrayList.add(0, msr)
//                        runOnUiThread { mScanResArrayAdapter.notifyDataSetChanged() }
//                    }
//                }
                }, { throwable ->
                    Log.e("LOG_TAG", "scan error: $throwable")
                    onScanStopClicked()
                })


        /* mScanSubscription = rxBleClient.observeStateChanges()
             .switchMap<Any> { state: RxBleClient.State? ->
                 when (state) {
                     RxBleClient.State.READY ->                 // everything should work
                         return@switchMap rxBleClient.scanBleDevices()

                     RxBleClient.State.BLUETOOTH_NOT_AVAILABLE, RxBleClient.State.LOCATION_PERMISSION_NOT_GRANTED, RxBleClient.State.BLUETOOTH_NOT_ENABLED, RxBleClient.State.LOCATION_SERVICES_NOT_ENABLED -> return@switchMap Observable.empty()
                     else -> return@switchMap Observable.empty()
                 }
             }
             .subscribe(
                 { rxBleScanResult: Any? ->
                         if (bleDevice.name != null && bleDevice.name.startsWith("Movesense")) {
                             val msr = MyScanResult(scanResult)
                             if (mScanResArrayList.contains(msr))
                                 mScanResArrayList[mScanResArrayList.indexOf(msr)] = msr
                             else
                                 mScanResArrayList.add(0, msr)
                             runOnUiThread { mScanResArrayAdapter.notifyDataSetChanged() }
                         }
                 }
             ) { throwable: Throwable? ->
                 Log.e("LOG_TAG", "scan error: $throwable")
                 onScanStopClicked(view)
             }*/
    }

    fun onScanStopClicked() {
        try {
            mScanSubscription.dispose()
        } catch (ex: Exception) {

        }

    }

    private fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
//    onScanClicked(view)
    }

    private fun testConnect() {
        val device = rxBleClient.getBleDevice("0C:8C:DC:41:E2:2B")
        mScanSubscription = device
            .establishConnection(false)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(
                { conn ->
                    Log.v("TAG", "BLE connected")
                },
                { throwable -> Log.e("TAG", "BLE connection failed", throwable) },
                {
                    Log.v("TAG", "BLE completed")
                }
            )











//
////        val bleDevice: RxBleDevice = rxBleClient.getBleDevice("0C:8C:DC:41:E2:2B")
////        val device = rxBleClient.getBleDevice("0C:8C:DC:41:E2:2B")
//        mScanSubscription = rxBleClient.getBleDevice("0C:8C:DC:41:E2:2B")
//            .establishConnection(false)
//            .subscribeOn(Schedulers.io())
//            .observeOn(Schedulers.io())
//            .subscribe(
//                { conn -> Log.v("TAG", "BLE connected") },
//                { throwable -> Log.e("TAG", "BLE connection failed", throwable) },
//                { Log.v("TAG", "BLE completed") }
//            )
//        Observable.timer(250, TimeUnit.MILLISECONDS)
//            .subscribeOn(Schedulers.io())
//            .observeOn(Schedulers.io())
//            .subscribe {
//                Log.v("TAG", "BLE cancelling connection")
//                mScanSubscription.dispose()
//            }
//
//        /*mScanSubscription = device.establishConnection(false) // <-- autoConnect flag
//            .subscribe(
//                { rxBleConnection: RxBleConnection? ->
//                    mMds!!.connect("0C:8C:DC:41:E2:2B", object : MdsConnectionListener {
//                        override fun onConnect(s: String) {
//                            Log.d("LOG_TAG", "onConnect: $s")
//                        }
//
//                        override fun onConnectionComplete(
//                            macAddress: String,
//                            serial: String
//                        ) {
//                            for (sr in mScanResArrayList) {
//                                if (sr.macAddress.equals(macAddress)) {
//                                    sr.markConnected(serial)
//                                    break
//                                }
//                            }
//                            runOnUiThread {
//                                mScanResArrayAdapter.notifyDataSetChanged()
//                            }
//                        }
//
//                        override fun onError(e: MdsException) {
//                            Log.e("LOG_TAG", "onError: $e")
//                            showConnectionError(e)
//                        }
//
//                        override fun onDisconnect(bleAddress: String) {
//                            Log.d("LOG_TAG", "onDisconnect: $bleAddress")
//                            for (sr in mScanResArrayList) {
//                                if (bleAddress == sr.macAddress.toString())
//                                    sr.markDisconnected()
//                            }
//                            runOnUiThread {
//                                mScanResArrayAdapter.notifyDataSetChanged()
//                            }
//                        }
//                    })
//                }
//            ) { throwable: Throwable? ->
////                mMds!!.disconnect(bleDevice)
//            }
//
////        mMds!!.connect("0C:8C:DC:41:E2:2B", object : MdsConnectionListener {
////            override fun onConnect(s: String) {
////                Log.d("LOG_TAG", "onConnect: $s")
////            }
////
////            override fun onConnectionComplete(macAddress: String, serial: String) {
////                for (sr in mScanResArrayList) {
////                    if (sr.macAddress.equals(macAddress)) {
////                        sr.markConnected(serial)
////                        break
////                    }
////                }
////                runOnUiThread {
////                    mScanResArrayAdapter.notifyDataSetChanged()
////                }
////            }
////
////            override fun onError(e: MdsException) {
////                Log.e("LOG_TAG", "onError: $e")
////                showConnectionError(e)
////            }
////
////            override fun onDisconnect(bleAddress: String) {
////                Log.d("LOG_TAG", "onDisconnect: $bleAddress")
////                for (sr in mScanResArrayList) {
////                    if (bleAddress == sr.macAddress.toString())
////                        sr.markDisconnected()
////                }
////                runOnUiThread {
////                    mScanResArrayAdapter.notifyDataSetChanged()
////                }
////            }
////        })*/
    }


    private fun onItemLongClick(
        parent: AdapterView<*>,
        view: View,
        position: Int,
        id: Long
    ): Boolean {
        mScanResultListView.setOnItemLongClickListener { parent, view, position, id ->
            if (position < 0 || position >= mScanResArrayList.size)
                return@setOnItemLongClickListener false
            val device = mScanResArrayList[position]
            val bleDevice = device.macAddress.bleDevice.macAddress
            if (!device.isConnected) {
                val device = rxBleClient.getBleDevice(bleDevice)
                mScanSubscription = device
                    .establishConnection(false)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(
                        { conn ->
                            Log.v("TAG", "BLE connected")
                        },
                        { throwable -> Log.e("TAG", "BLE connection failed", throwable) },
                        {
                            Log.v("TAG", "BLE completed")

                            for (sr in mScanResArrayList) {
                                if (sr.macAddress.equals(bleDevice)) {
//                                    sr.markConnected(serial)
                                    break
                                }
                            }
                            runOnUiThread {
                                mScanResArrayAdapter.notifyDataSetChanged()
                            }
                        }
                    )

                Log.v("TAG", "BLE connected")



               /* Observable.timer(250, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe {
                        Log.v("TAG", "BLE cancelling connection")
                        mScanSubscription.dispose()
                    }*/
              /*  mScanSubscription = device.establishConnection(false) // <-- autoConnect flag
                    .subscribe(
                        { rxBleConnection: RxBleConnection? ->
                            mMds!!.connect(bleDevice, object : MdsConnectionListener {
                                override fun onConnect(s: String) {
                                    Log.d("LOG_TAG", "onConnect: $s")
                                }

                                override fun onConnectionComplete(
                                    macAddress: String,
                                    serial: String
                                ) {
                                    for (sr in mScanResArrayList) {
                                        if (sr.macAddress.equals(macAddress)) {
                                            sr.markConnected(serial)
                                            break
                                        }
                                    }
                                    runOnUiThread {
                                        mScanResArrayAdapter.notifyDataSetChanged()
                                    }
                                }

                                override fun onError(e: MdsException) {
                                    Log.e("LOG_TAG", "onError: $e")
                                    showConnectionError(e)
                                }

                                override fun onDisconnect(bleAddress: String) {
                                    Log.d("LOG_TAG", "onDisconnect: $bleAddress")
                                    for (sr in mScanResArrayList) {
                                        if (bleAddress == sr.macAddress.toString())
                                            sr.markDisconnected()
                                    }
                                    runOnUiThread {
                                        mScanResArrayAdapter.notifyDataSetChanged()
                                    }
                                }
                            })
                        }
                    ) { throwable: Throwable? ->
                        mMds!!.disconnect(bleDevice)
                    }*/

                //val bleDevice =  rxBleClient.getBleDevice(device.macAddress.bleDevice)
                /*  val bleDevice =  device.macAddress.bleDevice
                  Log.i("LOG_TAG", "Connecting to BLE device: ${bleDevice.macAddress}")
                  try {
                      mMds!!.connect(bleDevice.macAddress, object : MdsConnectionListener {
                          override fun onConnect(s: String) {
                              Log.d("LOG_TAG", "onConnect: $s")
                          }

                          override fun onConnectionComplete(macAddress: String, serial: String) {
                              for (sr in mScanResArrayList) {
                                  if (sr.macAddress.equals(macAddress)) {
                                      sr.markConnected(serial)
                                      break
                                  }
                              }
                              runOnUiThread {
                                  mScanResArrayAdapter.notifyDataSetChanged()
                              }
                          }

                          override fun onError(e: MdsException) {
                              Log.e("LOG_TAG", "onError: $e")
                              showConnectionError(e)
                          }

                          override fun onDisconnect(bleAddress: String) {
                              Log.d("LOG_TAG", "onDisconnect: $bleAddress")
                              for (sr in mScanResArrayList) {
                                  if (bleAddress == sr.macAddress.toString())
                                      sr.markDisconnected()
                              }
                              runOnUiThread {
                                  mScanResArrayAdapter.notifyDataSetChanged()
                              }
                          }
                      })
                  }catch (ex:Exception){
                      Log.d("LOG_TAG", "onConnect--1: $ex")
                  }*/

            } else {
                Log.i("LOG_TAG", "Disconnecting from BLE device: ${device.macAddress}")
                mMds!!.disconnect(bleDevice)
            }
            true
        }
        return true
    }


    private fun showConnectionError(e: MdsException) {
        val builder = AlertDialog.Builder(this)
            .setTitle("Connection Error:")
            .setMessage(e.message)

        builder.create().show()
    }

}


