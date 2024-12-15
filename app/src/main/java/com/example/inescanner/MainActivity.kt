package com.example.inescanner

import android.graphics.BitmapFactory
import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var cameraImage: ImageView
    private lateinit var captureImageButton: Button
    private lateinit var resultText: TextView
    private lateinit var copyTextButton: Button

    private var currentPhotoPath: String? = null
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>



    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("INEScanner", "Entering OnCreate")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        cameraImage = findViewById(R.id.cameraImage)
        captureImageButton = findViewById(R.id.captureImageButton)
        resultText = findViewById(R.id.resultText)
        copyTextButton = findViewById(R.id.copyTextButton)

        // Request permission for the camera
        Log.i("INEScanner", "onCreate Antes de request permission for camera")
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted ->
            Log.i("INEScanner", "onCreate WTF!!!")
            if (isGranted) {
                Log.i("INEScanner", "onCreate HAVE permission for camera")
                captureImage()
            } else {
                Log.i("INEScanner", "onCreate no permission for camera")
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }

        }
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) {
                success ->
            if (success) {
                currentPhotoPath?.let {
                        path ->
                    val bitmap = BitmapFactory.decodeFile(path)
                    cameraImage.setImageBitmap(bitmap)
                    recognizeText(bitmap)
                }
            }
        }
        captureImageButton.setOnClickListener {
            Toast.makeText(this, "CaptureButton clicked!!", Toast.LENGTH_SHORT).show()
            Log.i("CaptureButton", "Clicked Button")
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)

        }
        /*ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets*/
        Log.i("INEScanner", "Saliendo de onCreate")
   }

    private fun createImageFile(): File {
        var timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun captureImage() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show()
            null
        }
        photoFile?.also {
            val photoUri: Uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", it)
            takePictureLauncher.launch(photoUri)
        }
    }

    private fun recognizeText(bitmap: Bitmap){
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image).addOnSuccessListener { ocrText ->
            resultText.text = ocrText.text
            resultText.movementMethod = ScrollingMovementMethod.getInstance()
            copyTextButton.visibility = Button.VISIBLE
            copyTextButton.setOnClickListener {
                val clipboard = ContextCompat.getSystemService(this, android.content.ClipboardManager::class.java)
                val clipboardContent = android.content.ClipData.newPlainText("INE Information", ocrText.text)
                clipboard?.setPrimaryClip(clipboardContent)
                Toast.makeText(this, "INE Information copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to scan INE text: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}