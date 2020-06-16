package com.mlwminidghr.mlwapp

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel

class AppViewModel : ViewModel() {
    var currentText = R.string.result_string.toString()
    var photoBitmap: Bitmap? = null
    var mode : MainActivity.Mode = MainActivity.Mode.FACE
    var currentPhotoPath : String? = null
    var currentPhotoUri : Uri? = null

}