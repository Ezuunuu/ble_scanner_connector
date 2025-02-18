# BLE Scanner & Connector (MVVM 기반)
Bluetooth Low Energy(BLE) 장치를 스캔하고 연결할 수 있는 안드로이드 애플리케이션(Kotlin)입니다.  

---

## 주요 기능 (Features)
**BLE 장치 스캔** - 주변 BLE 장치를 검색하고 리스트로 표시  
**BLE 장치 연결** - 선택한 장치에 연결 후 데이터 송수신  
**MVVM 아키텍처 적용** - ViewModel과 LiveData를 활용한 구조  
**RecyclerView UI 적용** - 카드 형태로 BLE 장치 표시  

---

## 기술 스택 (Tech Stack)
- **언어**: Kotlin
- **아키텍처**: MVVM (Model-View-ViewModel)
- **Bluetooth API**: Android Bluetooth LE API
- **UI 구성**:
  - **Material Design 3**
  - **RecyclerView**
  - **LiveData & ViewModel**
- **기타**:
  - Android 12+ 지원 (BLE 권한 처리 포함)

---

## 필수 권한 (Required Permission)
```
  <uses-permission android:name="android.permission.BLUETOOTH" />
  <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
  <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
  <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

---

![image](https://github.com/user-attachments/assets/18ab253b-14c1-4b4f-8ca1-90d43847e076)
