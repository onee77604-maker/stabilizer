package com.ridiitols.virtualeis

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var preview: PreviewView
    private lateinit var status: TextView
    private lateinit var recordButton: Button
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var sensorAvailable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        preview = findViewById(R.id.preview)
        status = findViewById(R.id.status)
        recordButton = findViewById(R.id.record)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorAvailable = rotationSensor != null
        status.text = if (sensorAvailable) "Rotation Vector tersedia. Kamera siap."
            else "Rotation Vector tidak tersedia; kamera tetap bisa merekam."
        recordButton.setOnClickListener { toggleRecording() }
        if (hasPermissions()) startCamera()
        else ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 100)
    }

    private fun hasPermissions(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && hasPermissions()) startCamera()
        else status.text = "Izin kamera/mikrofon diperlukan."
    }

    override fun onResume() {
        super.onResume()
        rotationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                val previewUseCase = Preview.Builder().build().also { it.surfaceProvider = preview.surfaceProvider }
                val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.FHD)).build()
                videoCapture = VideoCapture.withOutput(recorder)
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase, videoCapture)
                status.text = if (sensorAvailable) "Kamera siap • Rotation Vector terdeteksi" else "Kamera siap"
            } catch (e: Exception) {
                status.text = "Gagal membuka kamera: ${e.localizedMessage ?: "unknown"}"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleRecording() {
        val capture = videoCapture ?: run { status.text = "Kamera belum siap."; return }
        if (recording != null) {
            recording?.stop()
            recordButton.isEnabled = false
            return
        }
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "VirtualEIS_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/VirtualEIS")
        }
        val output = MediaStoreOutputOptions.Builder(contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(values).build()
        var pending = capture.output.prepareRecording(this, output)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
            pending = pending.withAudioEnabled()
        recording = pending.start(ContextCompat.getMainExecutor(this)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    recordButton.text = "STOP REKAM"
                    status.text = if (sensorAvailable) "Merekam • sensor rotasi tersedia" else "Merekam video"
                }
                is VideoRecordEvent.Finalize -> {
                    recording = null
                    recordButton.isEnabled = true
                    recordButton.text = "MULAI REKAM"
                    status.text = if (event.hasError()) "Rekaman gagal (kode ${event.error})"
                        else "Tersimpan di Movies/VirtualEIS"
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {}
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
