package com.ezuunuu.ble_scanner_connector

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private lateinit var deviceList: MutableList<String>
    private lateinit var adapter: ArrayAdapter<String>
    private var bluetoothGatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())
    private var status: BluetoothStatus = BluetoothStatus.initial

    private var startButton: Button? = null

    private val scanTimeout: Long = 10000 // 10 seconds

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ui 세팅
        init()

        if (!hasPermissions()) {
            requestPermissions()
        } else {
            initializeBluetooth()
        }
    }

    private fun init() {
        setContentView(R.layout.activity_main)

        val listView: ListView = findViewById(R.id.device_list)
        deviceList = mutableListOf()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            stopScan()
            val deviceInfo = deviceList[position]
            val deviceAddress = deviceInfo.split(" - ").last()

            connectToDevice(deviceAddress)
        }

        startButton = findViewById(R.id.scan_button)
        startButton?.setOnClickListener {
            if (status == BluetoothStatus.scanning) {
                stopScan()
            } else {
                startScan()
            }
        }
    }

    // 권한 부여 여부 체크
    private fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    // 권한 부여 요청
    private fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeBluetooth()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null || bluetoothAdapter?.isEnabled == false) {
            Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show()
            return
        }

        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    private fun startScan() {
        if (status == BluetoothStatus.scanning) return  // 중복 스캔 방지
        deviceList.clear()
        adapter.notifyDataSetChanged()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Missing required permissions", Toast.LENGTH_SHORT).show()
                    return
                }
            } else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Missing required permissions", Toast.LENGTH_SHORT).show()
                    // 권한이 없으므로 권한 요청 시도
                    requestPermissions()
                    return
                }
            }
            bluetoothLeScanner?.startScan(scanCallback)
            status = BluetoothStatus.scanning
            startButton?.text = "Stop Scan"
            handler.postDelayed({ stopScan() }, scanTimeout) // timeout 적용
            Toast.makeText(this, "Scanning for BLE devices...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Permission denied: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopScan() {
        if (status == BluetoothStatus.scanning) {
            try {
                bluetoothLeScanner?.stopScan(scanCallback)
            } catch (e: SecurityException) {
                Toast.makeText(this, "Unable to stop scan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            Toast.makeText(this, "Scan stopped", Toast.LENGTH_SHORT).show()
        } else if (status == BluetoothStatus.connected || status == BluetoothStatus.connecting) {
            disconnect()
        }
        handler.removeCallbacksAndMessages(null)
        status = BluetoothStatus.stopped
        startButton?.text = "Start Scan"
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device: BluetoothDevice = result.device
            val deviceName = try {
                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    device.name ?: "Unknown Device"
                } else {
                    "Permission Required"
                }
            } catch (e: Exception) {
                "Unable to read device name: ${e.message}"
            }
            val deviceInfo = "$deviceName - ${device.address}"
            if (!deviceList.contains(deviceInfo)) {
                deviceList.add(deviceInfo)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun connectToDevice(deviceAddress: String) {
        status = BluetoothStatus.connecting
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        try {
            if (device != null) {
                bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                        if (newState == BluetoothGatt.STATE_CONNECTED) {
                            this@MainActivity.status = BluetoothStatus.connected
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Connected to $deviceAddress", Toast.LENGTH_SHORT).show()
                            }
                        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                            this@MainActivity.status = BluetoothStatus.disconnected
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Disconnected from $deviceAddress", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                })
            } else {
                Toast.makeText(this, "Failed to connect to device", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Unable to connect to ${device?.name}: ${e.message}", Toast.LENGTH_SHORT).show()
        }

    }

    private fun disconnect() {
        try {
            bluetoothGatt?.let {
                it.disconnect()
                it.close()
                bluetoothGatt = null
                runOnUiThread {
                    Toast.makeText(this, "Disconnected from device", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Unable to disconnecting: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            stopScan()
            disconnect()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Unable to stop scan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}