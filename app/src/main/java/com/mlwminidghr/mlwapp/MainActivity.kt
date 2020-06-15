package com.mlwminidghr.mlwapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

const val REQUEST_IMAGE_CAPTURE = 1
const val IMAGE_PICK = 2

class MainActivity : AppCompatActivity() {
    private val colorChosen = "#5B44A2"
    private val colorAccent = "#53ABE3"
    private enum class Mode {
        PET, FACE, TRAFFIC
    }
    private var mode = Mode.FACE
    private lateinit var image : ImageView
    private lateinit var fab : FloatingActionButton
    private lateinit var text : TextView
    private lateinit var petButton : ImageButton
    private lateinit var trafficButton : ImageButton
    private lateinit var faceButton : ImageButton
    private var imageData: ByteArray? = null
    private val postURL: String = "http://161.35.118.47:8000/prediction/"
    private lateinit var currentPhotoPath: String
    private lateinit var currentPhotoUri : Uri

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpeg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                null
            }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.mlwminidghr.mlwapp.fileprovider",
                        it
                    )
                    currentPhotoUri = photoURI
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
        }
        }
    }

    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/"
        startActivityForResult(intent, IMAGE_PICK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main);
        text = findViewById(R.id.response_text)
        image = findViewById(R.id.thumb_image)
        faceButton = findViewById(R.id.face_button)
        fab = findViewById(R.id.fab)
        petButton = findViewById(R.id.pet_button)
        trafficButton = findViewById(R.id.traffic_button)
        faceButton.setOnClickListener {
            faceButton.setColorFilter(Color.parseColor(colorChosen))
            petButton.setColorFilter(Color.parseColor(colorAccent))
            trafficButton.setColorFilter(Color.parseColor(colorAccent))
            mode = Mode.FACE
        }
        petButton.setOnClickListener {
            petButton.setColorFilter(Color.parseColor(colorChosen))
            faceButton.setColorFilter(Color.parseColor(colorAccent))
            trafficButton.setColorFilter(Color.parseColor(colorAccent))
            mode = Mode.PET
        }
        trafficButton.setOnClickListener {
            trafficButton.setColorFilter(Color.parseColor(colorChosen))
            petButton.setColorFilter(Color.parseColor(colorAccent))
            faceButton.setColorFilter(Color.parseColor(colorAccent))
            mode = Mode.TRAFFIC
        }
        fab.setOnClickListener{
            val mAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate)
            fab.startAnimation(mAnimation)
        }
        image.setOnClickListener {
            dispatchTakePictureIntent()
        }
        image.setOnLongClickListener {
            launchGallery()
            return@setOnLongClickListener true
        }
    }
    private fun scaleBitmap(bitmap: Bitmap) : Bitmap {
        if(bitmap.height.toFloat() < 4096 && bitmap.width.toFloat() < 4096) {
            return bitmap
        }
        if(bitmap.width.toFloat() >= bitmap.height.toFloat()){
            return Bitmap.createScaledBitmap(bitmap, 4000, (bitmap.height.toFloat() * 4000 / bitmap.width.toFloat()).toInt(), false)
        } else {
            return Bitmap.createScaledBitmap(bitmap, (bitmap.width.toFloat() * 4000 / bitmap.height.toFloat()).toInt(), 4000,false)
        }
    }
    private fun uploadImage() {
        imageData?: return
        val request = object : VolleyFileUploadRequest(
            Method.POST,
            postURL,
            Response.Listener {
                text.setText("THE RESPONSE IS HERE $it")
            },
            Response.ErrorListener {
                text.setText("THE ERROR HAS ARRIVED $it")
            }
        ) {
            override fun getByteData():MutableMap<String, FileDataPart> {
                val params = HashMap<String, FileDataPart>()
                params["myfile"] = FileDataPart("image", imageData!!, "jpeg")
                return params
            }
        }
        request.retryPolicy = DefaultRetryPolicy(10000, 10, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        Volley.newRequestQueue(this).add(request)
    }
    @Throws(IOException::class)
    private fun createImageData(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        inputStream?.buffered()?.use {
            imageData = it.readBytes()
        }
    }
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            var bitmap = scaleBitmap(BitmapFactory.decodeFile(currentPhotoPath))
            image.setImageBitmap(bitmap)
            createImageData(currentPhotoUri)
        }
        else if(requestCode == IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if(uri != null) {
                var bitmap = scaleBitmap(MediaStore.Images.Media.getBitmap(this.contentResolver, uri))
                image.setImageBitmap(bitmap)
                createImageData(uri)
            }
        }
    }

}