package com.example.textrecognition

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.textrecognition.CameraXActivity.Companion.CAMERAX_RESULT
import com.example.textrecognition.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : AppCompatActivity() {
    // for binding
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    // for view model
    private val viewModel: MainViewModel by viewModels()

    // for permission
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permission request granted", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }

    // for gallery
    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.currentImageUriStr.value = uri.toString()
        } else {
            Toast.makeText(this, "No media selected", Toast.LENGTH_LONG).show()
        }
    }

    // for camera
    private val launcherIntentCameraX = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == CAMERAX_RESULT) {
            viewModel.currentImageUriStr.value = it.data?.getStringExtra(CameraXActivity.EXTRA_CAPTURED_IMAGE)?.toUri()!!.toString()
        }
    }

    // for permission
    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        if (!allPermissionsGranted()) requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        performButton()
        performObserve()
    }

    private fun performButton() {
        binding.cameraBtn.setOnClickListener {
            val intent = Intent(this, CameraXActivity::class.java)
            launcherIntentCameraX.launch(intent)
        }
        binding.galleryBtn.setOnClickListener {
            launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.extractBtn.setOnClickListener {
            analyzeImage(viewModel.currentImageUriStr.value!!.toUri())
        }
        binding.copyBtn.setOnClickListener {
            copyTextToClipboard(this, binding.resultTv.text.toString())
        }
    }

    private fun performObserve() {
        viewModel.currentImageUriStr.observe(this) {
            if (it != null && it != "") {
                binding.imageView.setImageURI(it.toUri())
                binding.extractBtn.isEnabled = true
            } else {
                binding.extractBtn.isEnabled = false
                binding.imageView.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.ic_gallery_350)
                )
            }
        }
        viewModel.resultText.observe(this) {
            if (it.isNullOrBlank()) {
                binding.cardView4.visibility = View.GONE
            } else {
                binding.cardView4.visibility = View.VISIBLE
                binding.resultTv.text = it
            }
        }
        viewModel.isProgressbarVisible.observe(this) {
            binding.progressBar.visibility = if (it == true) View.VISIBLE else View.GONE
        }
    }

    private fun analyzeImage(uri: Uri) {
        try {
            viewModel.isProgressbarVisible.value = true
            val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val inputImage: InputImage = InputImage.fromFilePath(this, uri)
            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText: Text ->
                    val detectedText: String = visionText.text
                    if (detectedText.isNotBlank()) {
                        viewModel.isProgressbarVisible.value = false
                        viewModel.resultText.value = detectedText
                    } else {
                        viewModel.isProgressbarVisible.value = false
                        viewModel.resultText.value = ""
                        Toast.makeText(this, "no text detected", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    viewModel.isProgressbarVisible.value = false
                    viewModel.resultText.value = ""
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun copyTextToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text From Text Recognition App", text)
        clipboard.setPrimaryClip(clip)

        // Optional: Show a toast message to inform the user
        Toast.makeText(context, "Text copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQUIRED_PERMISSION = android.Manifest.permission.CAMERA
    }
}
