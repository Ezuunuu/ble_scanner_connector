package com.ezuunuu.ble_scanner_connector.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ezuunuu.ble_scanner_connector.repository.BleRepository

class BleViewModel : ViewModel() {
    private val repository = BleRepository()

    private val _deviceList = MutableLiveData<List<String>>()
    val deviceList: LiveData<List<String>> = _deviceList

    private val _isScanning = MutableLiveData<Boolean>()
    val isScanning: LiveData<Boolean> = _isScanning

    fun initializeBluetooth(context: Context) {
        repository.initializeBluetooth(context)
    }

    fun startScan() {
        repository.startScan { devices ->
            _deviceList.postValue(devices)
        }
        _isScanning.postValue(true)
    }

    fun stopScan() {
        repository.stopScan()
        _isScanning.postValue(false)
    }

    fun connectToDevice(deviceAddress: String) {
        repository.connectToDevice(deviceAddress)
    }

    fun hasPermissions(context: Context): Boolean {
        return repository.hasPermissions(context)
    }
}
