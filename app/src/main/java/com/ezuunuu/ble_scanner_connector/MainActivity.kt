package com.ezuunuu.ble_scanner_connector

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import com.ezuunuu.ble_scanner_connector.viewmodel.BleViewModel

class MainActivity : AppCompatActivity() {
    private val bleViewModel: BleViewModel by viewModels()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var deviceList: MutableList<String>
    private var startButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initUI()
        observeViewModel()

        if (!bleViewModel.hasPermissions(this)) {
            requestPermissions()
        } else {
            bleViewModel.initializeBluetooth(this)
        }
    }

    private fun initUI() {
        val listView: ListView = findViewById(R.id.device_list)
        deviceList = mutableListOf()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val deviceInfo = deviceList[position]
            val deviceAddress = deviceInfo.split(" - ").last()
            bleViewModel.connectToDevice(deviceAddress)
        }

        startButton = findViewById(R.id.scan_button)
        startButton?.setOnClickListener {
            if (bleViewModel.isScanning.value == true) {
                bleViewModel.stopScan()
            } else {
                bleViewModel.startScan()
            }
        }
    }

    private fun observeViewModel() {
        bleViewModel.deviceList.observe(this, Observer { devices ->
            deviceList.clear()
            deviceList.addAll(devices)
            adapter.notifyDataSetChanged()
        })

        bleViewModel.isScanning.observe(this, Observer { scanning ->
            startButton?.text = if (scanning) "Stop Scan" else "Start Scan"
        })
    }

    private fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        ActivityCompat.requestPermissions(this, permissions, 1)
    }
}
