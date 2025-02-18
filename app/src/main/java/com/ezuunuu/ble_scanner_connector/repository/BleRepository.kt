package com.ezuunuu.ble_scanner_connector.repository

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.UUID

class BleRepository {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var scanCallback: ScanCallback? = null

    companion object {
        private val SERVICE_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
        private val CHARACTERISTIC_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    }

    fun initializeBluetooth(context: Context) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    fun hasPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(callback: (List<String>) -> Unit) {
        try {
            val deviceList = mutableListOf<String>()
            scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val device = result.device
                    val deviceName = device.name ?: "Unknown Device"

                    val deviceInfo = "$deviceName - ${device.address}"
                    Log.d("Device name", deviceName)
                    Log.d("Device Info", deviceInfo)
                    if (!deviceList.contains(deviceInfo)) {
                        deviceList.add(deviceInfo)
                        callback(deviceList)
                    }
                }
            }

            bluetoothLeScanner?.startScan(scanCallback)
            handler.postDelayed({ stopScan() }, 10000) // scan duration 10 sec
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun stopScan() {
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            handler.removeCallbacksAndMessages(null)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceAddress: String) {
        try {
            val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
            device?.connectGatt(null, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        gatt.discoverServices()
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        val service = gatt.getService(SERVICE_UUID)
                        writeCharacteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
                        bluetoothGatt = gatt
                        sendPacket(byteArrayOf(0x06)) // send ACK
                    }
                }

                override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                    if (characteristic.uuid == CHARACTERISTIC_UUID) {
                        val receivedData = characteristic.value
                        onReceivePacket(receivedData)
                    }
                }
            })
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

    }

    fun sendPacket(data: ByteArray) {
        try {
            writeCharacteristic?.let {
                it.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                bluetoothGatt?.let { gatt ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeCharacteristic(it, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    } else {
                        it.value = data
                        gatt.writeCharacteristic(it)
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun onReceivePacket(data: ByteArray) {
        println("Received Data: ${data.joinToString(",")}")
    }
}
