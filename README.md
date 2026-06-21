# Drowsiness Detection Android

Aplikasi Android untuk mendeteksi indikasi kantuk secara real-time berdasarkan fitur geometris wajah.

Sistem menggunakan **Eye Aspect Ratio (EAR)** untuk mendeteksi kondisi mata dan **Mouth Aspect Ratio (MAR)** untuk mendeteksi aktivitas menguap. Aplikasi juga menyediakan pilihan penggunaan **Contrast Limited Adaptive Histogram Equalization (CLAHE)** sebagai tahap preprocessing citra.

## Fitur Utama

- Deteksi wajah dan facial landmark secara real-time
- Perhitungan nilai EAR dan MAR
- Deteksi kondisi normal dan mengantuk
- Peringatan suara dan getaran
- Pilihan CLAHE aktif atau nonaktif
- Pencatatan FPS, latensi, penggunaan memori, dan suhu perangkat

## Teknologi

- Android Studio
- Kotlin
- Google ML Kit Face Mesh
- OpenCV
- CameraX

## Menjalankan Aplikasi

1. Clone atau unduh repository ini.
2. Buka folder project menggunakan Android Studio.
3. Tunggu proses Gradle Sync selesai.
4. Jalankan aplikasi pada smartphone Android.
5. Berikan izin kamera saat aplikasi pertama kali dijalankan.

## Tentang Penelitian

Source code ini digunakan dalam penelitian skripsi berjudul:

**“Perbandingan Performa Sistem Deteksi Kantuk Berbasis Fitur Geometris Wajah dengan dan tanpa CLAHE pada Berbagai Smartphone Android.”**

Pengujian dilakukan pada POCO M3, Realme X, dan ASUS ROG Phone 6.

## Penulis

Daffa Aprilian Herdikaputra  
Program Studi Teknik Komputer  
Fakultas Ilmu Komputer  
Universitas Brawijaya