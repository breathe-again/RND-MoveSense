package com.R.movsenseapplication

import android.app.AlertDialog
import android.bluetooth.le.ScanSettings
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.ComponentActivity
import com.movesense.mds.Mds
import com.movesense.mds.MdsConnectionListener
import com.movesense.mds.MdsException
import org.reactivestreams.Subscription

class MainActivity : ComponentActivity() {
    private var mMds: Mds? = null

    private lateinit var mScanSubscription: Subscription
    private lateinit var mScanResultListView: ListView
    private lateinit var mScanResArrayList: ArrayList<MyScanResult>
    private lateinit var mScanResArrayAdapter: ArrayAdapter<MyScanResult>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initMds()
        mScanResultListView = findViewById(R.id.listScanResult)
        mScanResArrayList = ArrayList()
        mScanResArrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mScanResArrayList)
        mScanResultListView.adapter = mScanResArrayAdapter
        mScanResultListView.setOnItemClickListener { parent, view, position, id -> onItemClick(parent, view, position, id) }
        mScanResultListView.setOnItemLongClickListener { parent, view, position, id -> onItemLongClick(parent, view, position, id) }



    }

    private fun initMds() {
        mMds = Mds.builder().build(this)
    }


fun onScanClicked(view: View) {
    findViewById<View>(R.id.buttonScan).visibility = View.GONE
    findViewById<View>(R.id.buttonScanStop).visibility = View.VISIBLE
    // Start with empty list
    mScanResArrayList.clear()
    mScanResArrayAdapter.notifyDataSetChanged()

    mScanSubscription = getBleClient()?.scanBleDevices(
        ScanSettings.Builder().build()
    )?.subscribe({ scanResult ->
        Log.d("LOG_TAG", "scanResult: $scanResult")

        // Process scan result here. filter movesense devices.
        scanResult.bleDevice?.let { bleDevice ->
            if (bleDevice.name != null && bleDevice.name.startsWith("Movesense")) {
                val msr = MyScanResult(scanResult)
                if (mScanResArrayList.contains(msr))
                    mScanResArrayList[mScanResArrayList.indexOf(msr)] = msr
                else
                    mScanResArrayList.add(0, msr)
                runOnUiThread { mScanResArrayAdapter.notifyDataSetChanged() }
            }
        }
    }, { throwable ->
        Log.e("LOG_TAG", "scan error: $throwable")
        onScanStopClicked(view)
    })
}

fun onScanStopClicked(view: View) {
//    mScanSubscription.unsubscribe()
//    mScanSubscription = null
    findViewById<View>(R.id.buttonScan).visibility = View.VISIBLE
    findViewById<View>(R.id.buttonScanStop).visibility = View.GONE
}

private fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
    onScanClicked(view)
}

private fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
      mScanResultListView.setOnItemLongClickListener { parent, view, position, id ->
        if (position < 0 || position >= mScanResArrayList.size)
            return@setOnItemLongClickListener false

        val device = mScanResArrayList[position]
        if (!device.isConnected) {
            val bleDevice = getBleClient().getBleDevice(device.macAddress)
            Log.i("LOG_TAG", "Connecting to BLE device: ${bleDevice.macAddress}")
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
        } else {
            Log.i("LOG_TAG", "Disconnecting from BLE device: ${device.macAddress}")
            mMds!!.disconnect(device.macAddress.toString())
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


