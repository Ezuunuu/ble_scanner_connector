package com.ezuunuu.ble_scanner_connector.model

enum class BluetoothStatus {
    initial,
    scanning,
    stopped,
    connecting,
    connected,
    disconnecting,
    disconnected
}