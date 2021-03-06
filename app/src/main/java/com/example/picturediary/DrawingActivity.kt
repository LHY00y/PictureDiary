package com.example.picturediary

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.jar.Manifest


class DrawingActivity : AppCompatActivity() {
    private var drawingView:DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    var customProgressDialog: Dialog? = null

    val openGalleryLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
        if (result.resultCode == RESULT_OK && result.data != null){
            val imageBackground: ImageView = findViewById(R.id.iv_background)
            imageBackground.setImageURI(result.data?.data)
        }
    }

    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val perMissionName = it.key
                val isGranted = it.value
                if (isGranted ) {
                    Toast.makeText(
                        this,
                        "Permission granted now you can read the storage files.",
                        Toast.LENGTH_SHORT
                    ).show()
                    //perform operation
                    //create an intent to pick image from external storage

                } else {
                    if (perMissionName == android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        Toast.makeText(
                            this,
                            "Oops you just denied the permission.",
                            Toast.LENGTH_LONG
                        ).show()
                }
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drawing_main)
        drawingView = findViewById(R.id.drawing_view)
        val ibBrush: ImageButton = findViewById(R.id.ib_brush)
        ibBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }
        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)
        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint?.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.pallet_pressed
            )
        )

        val ibGallery: ImageButton = findViewById(R.id.ib_gallery)

        ibGallery.setOnClickListener {
            if(isReadStorageAllowed())
            {
                val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                //using the intent launcher created above launch the pick intent
                openGalleryLauncher.launch(pickIntent)
            }
            else{
                requestStoragePermission()
            }


        }

        val ibUndo: ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener {
            drawingView?.onClickUndo()
        }

        val ibRedo: ImageButton = findViewById(R.id.ib_redo)
        ibRedo.setOnClickListener {
            drawingView?.onClickRedo()
        }

        val ibSave: ImageButton = findViewById(R.id.ib_save)
        ibSave.setOnClickListener {
            if(isReadStorageAllowed())
            {
                showProgressDialog()
                lifecycleScope.launch{
                    val fl: FrameLayout = findViewById(R.id.fl_drawing_view_container)
                    saveBitmapFile(getBitmapFromView(fl))
                }
            }
            else{
                requestStoragePermission()
            }
        }
        val ibReset: ImageButton = findViewById(R.id.ib_reset)
        ibReset.setOnClickListener {
            drawingView?.onReset()
            val imageBackground: ImageView = findViewById(R.id.iv_background)
            imageBackground.setImageResource(0)
        }

    }
    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size :")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener(View.OnClickListener {
            drawingView?.setBrushSize(10.toFloat())
            brushDialog.dismiss()
        })
        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener(View.OnClickListener {
            drawingView?.setBrushSize(20.toFloat())
            brushDialog.dismiss()
        })

        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener(View.OnClickListener {
            drawingView?.setBrushSize(30.toFloat())
            brushDialog.dismiss()
        })
        brushDialog.show()
    }
    fun paintClicked(view: View?) {
        if (view !== mImageButtonCurrentPaint) {
            // Update the color
            val imageButton = view as ImageButton
            // Here the tag is used for swaping the current color with previous color.
            // The tag stores the selected view
            val colorTag = imageButton.tag.toString()
            // The color is set as per the selected tag here.
            drawingView?.setColor(colorTag)
            // Swap the backgrounds for last active and currently active image button.
            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_normal
                )
            )

            //Current view is updated with selected view in the form of ImageButton.
            mImageButtonCurrentPaint = view
        }
    }

    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission(){
        if (
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
        ){
            showRationaleDialog("Paint","Paint " +
                    "needs to Access Your External Storage")
        }
        else {
            requestPermission.launch(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }

    }

    private fun showRationaleDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }
    private fun getBitmapFromView(view : View): Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable!=null) {
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap): String{
        var result =  ""
        withContext(Dispatchers.IO){
            if(mBitmap!=null){
                try{
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)

                    val f = File(externalCacheDir?.absoluteFile.toString()+File.separator+"Paints_"+System.currentTimeMillis()/1000+".png")
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result = f.absolutePath
                    runOnUiThread {
                        cancelProgressDialog()
                        if(result.isNotEmpty()){
                            Toast.makeText(this@DrawingActivity,"File saved successfully: $result",Toast.LENGTH_SHORT).show()
                        }
                        else{
                            Toast.makeText(this@DrawingActivity,"File not saved.",Toast.LENGTH_SHORT).show()
                        }
                    }


                }
                catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }
    private fun showProgressDialog() {
        customProgressDialog = Dialog(this)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        //Start the dialog and display it on screen.
        customProgressDialog?.show()
    }


    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }
}