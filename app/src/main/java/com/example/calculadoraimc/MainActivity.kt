package com.example.calculadoraimc

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.calculadoraimc.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.NumberFormat

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Binding elements
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Accessing elements
             //hide btn download
        binding.btnDownload.hide()
            // calculate
        binding.btnCalcular.setOnClickListener{ calculateIMC() }
            // on enter hide keyboard
        binding.inputPeso.setOnKeyListener { view, keyCode, _ -> handleKeyEvent(view, keyCode) }

        // PERMISSIONS TO SCREENSHOT
            // Write permission to access the storage
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        binding.btnDownload.setOnClickListener{
            // SCREENSHOT
            val bitmap = getScreenShotFromView(binding.card)
            // IF EXISTS, SAVE
            if (bitmap != null) {
                saveMediaToStorage(bitmap)
            }
        }
    }

    // FUNCTION FOR CALCULATE IMC
    private fun calculateIMC() {
        val inputAltura = binding.inputAltura.editText?.text.toString()
        val altura = inputAltura.toDoubleOrNull()

        val inputPeso = binding.inputPeso.editText?.text.toString()
        val peso = inputPeso.toDoubleOrNull()

        // validate if the input was filled
        if ( (altura === null || peso === null) || (altura == 0.00 || peso == 0.00) ) {
            display(0.00)
            return
        }

        val imc = (peso / (altura * altura))
        display(imc)

        // show button download
        binding.btnDownload.show()
    }

    // FUNCTION FOR VERIFY CLASSIFICATION
    private fun classificationIMC(imc: Double): String {
        return if(imc == 0.00) { "" }
        else if(imc <= 18.5) { "Abaixo do peso" }
        else if(imc > 18.6 && imc <= 24.9) { "Saudável" }
        else if(imc > 25 && imc <= 29.9) { "Peso em Excesso" }
        else if(imc > 30 && imc <= 34.9) { "Obesidade Leve" }
        else if(imc > 35 && imc < 39.9) { "Obesidade Severa" }
        else { "Obesidade Mórbida" }
    }

    // FUNCTION FOR DISPLAY INFO ON SCREEN
    private fun display(imc: Double){
        // Format output
        val formattedIMC = NumberFormat.getInstance().format(imc)
        val classificacao = classificationIMC(imc)
        // binding results
        binding.indice.text = getString(R.string.indice, formattedIMC)
        binding.classificacao.text = getString(R.string.classificacao, classificacao)
    }

    // FUNCTION FOR HIDE THE KEYBOARD ON ENTER CLICK
    private fun handleKeyEvent(view: View, keyCode: Int): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            // Hide the keyboard
            val inputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
            return true
        }
        return false
    }

    //FUNCTION FOR SCREENSHOT
    private fun getScreenShotFromView(v: View): Bitmap? {
        // Create a bitmap object
        var screenshot: Bitmap? = null
        try {
            // Inflate screenshot object with Bitmap.
            screenshot = Bitmap.createBitmap(v.measuredWidth, v.measuredHeight, Bitmap.Config.ARGB_8888)
            // Draw this bitmap on a canvas
            val canvas = Canvas(screenshot)
            v.draw(canvas)
        } catch (e: Exception) {
            Log.e("GFG", "Failed to capture screenshot because:" + e.message)
        }
        return screenshot
    }


    // FUNCTION FOR SAVE THE SCREENSHOT
    private fun saveMediaToStorage(bitmap: Bitmap) {
        // Generating a file name
        val filename = "${System.currentTimeMillis()}.jpg"

        // Output stream
        var fos: OutputStream? = null

        // For devices running android >= Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // getting the contentResolver
            this.contentResolver?.also { resolver ->

                // Content resolver will process the contentvalues
                val contentValues = ContentValues().apply {

                    // putting file information in content values
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                // Inserting the contentValues to
                // contentResolver and getting the Uri
                val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                // Opening an outputstream with the Uri that we got
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            // These for devices running on android < Q
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            // Writing the bitmap to the output stream
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            Toast.makeText(this , "IMAGEM SALVA NA GALERIA" , Toast.LENGTH_SHORT).show()
        }
    }
}