package com.R.movsenseapplication


import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.movesense.mds.Mds
import com.movesense.mds.MdsConnectionListener
import com.movesense.mds.MdsException
import com.movesense.mds.MdsResponseListener
//import com.polidea.rxandroidble2.RxBleClient
//import com.polidea.rxandroidble2.scan.ScanResult
//import com.polidea.rxandroidble2.scan.ScanSettings
//import io.reactivex.disposables.Disposable


class ActivityMovsense : AppCompatActivity() {

    private lateinit var mMds: Mds
//    private lateinit var mScanSubscription: Disposable
    private lateinit var mScanResultListView: ListView
//    private val mScanResArrayList: ArrayList<MyScanResult> = ArrayList()
//    private lateinit var mScanResArrayAdapter: ArrayAdapter<MyScanResult>
//    private lateinit var bleClient: RxBleClient
    private val LOG_TAG = "ActivityMovsense"
    val SCHEME_PREFIX = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mMds = Mds.builder().build(this)
//        bleClient = RxBleClient.create(this)
//        mScanResultListView = findViewById(R.id.listScanResult)
//        mScanResArrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mScanResArrayList)
//        mScanResultListView.adapter = mScanResArrayAdapter
//        mScanResultListView.setOnItemLongClickListener(this)

//        mScanResultListView.setOnItemClickListener(this)
    }


    // Define a dummy getBleClient method
   /* private fun getBleClient(): RxBleClient {
        // Return your RxBleClient instance or handle the creation of it
        return bleClient
    }

    fun onScanClicked(view: View) {
        findViewById<View>(R.id.buttonScan).visibility = View.GONE
        findViewById<View>(R.id.buttonScanStop).visibility = View.VISIBLE

        // Start with empty list
        mScanResArrayList.clear()
        mScanResArrayAdapter.notifyDataSetChanged()

        mScanSubscription = getBleClient().scanBleDevices(
            ScanSettings.Builder()
                // .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // change if needed
                // .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // change if needed
                .build()
            // add filters if needed
        )
            .subscribe(
                { scanResult ->
                    Log.d(LOG_TAG, "scanResult: $scanResult")

                    // Process scan result here. filter movesense devices.
                    if (scanResult.bleDevice != null &&
                        scanResult.bleDevice.name != null &&
                        scanResult.bleDevice.name!!.startsWith("Movesense")
                    ) {
                        // Replace if exists already, add otherwise
                        val msr = MyScanResult(scanResult)
                        if (mScanResArrayList.contains(msr))
                            mScanResArrayList[mScanResArrayList.indexOf(msr)] = msr
                        else
                            mScanResArrayList.add(0, msr)

                        runOnUiThread {
                            mScanResArrayAdapter.notifyDataSetChanged()
                        }
                    }
                },
                { throwable ->
                    Log.e(LOG_TAG, "scan error: $throwable")
                    // Handle an error here.

                    // Re-enable scan buttons, just like with ScanStop
                    onScanStopClicked(view)
                }
            )
    }

    private fun MyScanResult(macAddress: ScanResult?): MyScanResult {
        TODO("Not yet implemented")
    }

    fun onScanStopClicked(view: View) {
        mScanSubscription.dispose()

        findViewById<View>(R.id.buttonScan).visibility = View.VISIBLE
        findViewById<View>(R.id.buttonScanStop).visibility = View.GONE
    }

    override fun onItemLongClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean {
        if (position < 0 || position >= mScanResArrayList.size)
            return false

        val device = mScanResArrayList[position]
        if (!device.isConnected) {
            val bleDevice = getBleClient().getBleDevice(device.macAddress.toString())
            Log.i(LOG_TAG, "Connecting to BLE device: " + bleDevice.macAddress)
            mMds.connect(bleDevice.macAddress, object : MdsConnectionListener {
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
                    runOnUiThread {
                        mScanResArrayAdapter.notifyDataSetChanged()
                    }
                }

                override fun onError(e: MdsException) {
                    Log.e(LOG_TAG, "onError:$e")
                    showConnectionError(e)
                }

                override fun onDisconnect(bleAddress: String) {
                    Log.d(LOG_TAG, "onDisconnect: $bleAddress")
                    for (sr in mScanResArrayList) {
                        if (bleAddress.equals(sr.macAddress))
                            sr.markDisconnected()
                    }
                    runOnUiThread {
                        mScanResArrayAdapter.notifyDataSetChanged()
                    }
                }
            })
        } else {
            Log.i(LOG_TAG, "Disconnecting from BLE device: ${device.macAddress}")
            mMds.disconnect(device.macAddress.toString())
        }
        return true
    }

  *//*  override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (position < 0 || position >= mScanResArrayList.size)
            return

        val device = mScanResArrayList[position]
        if (!device.isConnected) {
            return
        }

        val uri = "$SCHEME_PREFIX${device.connectedSerial}/Info"
        mMds.get(uri, null, object : MdsResponseListener {
            override fun onSuccess(s: String) {
                Log.i(LOG_TAG, "Device ${device.connectedSerial} /info request successful: $s")
                // Display info in alert dialog
                AlertDialog.Builder(this@ActivityMovsense)
                    .setTitle("Device info:")
                    .setMessage(s)
                    .show()
            }

            override fun onError(e: MdsException) {
                Log.e(LOG_TAG, "Device ${device.connectedSerial} /info returned error: $e")
            }
        })
    }*//*

    private fun showConnectionError(e: MdsException) {
        AlertDialog.Builder(this)
            .setTitle("Connection Error:")
            .setMessage(e.message)
            .create()
            .show()
    }*/

}
