package edu.farmingdale.alrajab.week12_auth_ml_api_demo

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import edu.farmingdale.alrajab.week12_auth_ml_api_demo.databinding.ActivityLandingBinding

class LandingActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityLandingBinding

    private val galleryResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageURI = result.data?.data
            selectedImageURI?.let { uri ->
                val imageBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
                binding.imageHolder.setImageBitmap(imageBitmap)
                currentBitmap = imageBitmap
            }
        }
    }

    private val cameraResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val photo = result.data?.extras?.get("data") as Bitmap
            binding.imageHolder.setImageBitmap(photo)
            currentBitmap=photo
        }
    }

    private var currentBitmap: Bitmap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.logoutBtn.setOnClickListener { logout() }
        binding.loadImageBtn.setOnClickListener { openImageSourceChoice() }
        binding.labelButton.setOnClickListener {
            currentBitmap?.let { bitmap ->
                labelImage(bitmap)
            }
        }


        firebaseAuth = FirebaseAuth.getInstance()

    }

    private fun logout() {
        firebaseAuth.signOut()
            startActivity(Intent(this@LandingActivity,LoginActivity::class.java))
        finish()
    }

    private fun openImageSourceChoice() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Select Image Source")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openCamera()
                1 -> openGallery()
            }
        }
        builder.show()
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryResultLauncher.launch(galleryIntent)
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraResultLauncher.launch(cameraIntent)
    }

    private fun labelImage(imageBitmap: Bitmap) {
        val image = InputImage.fromBitmap(imageBitmap, 0)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                displayLabels(labels)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Labeling failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun displayLabels(labels: List<ImageLabel>) {
        val labelsStr = StringBuilder()
        for (label in labels) {
            labelsStr.append(label.text).append(" (").append(label.confidence).append(")\n")
        }

        binding.labelsTextView.text = labelsStr.toString()
    }

}


