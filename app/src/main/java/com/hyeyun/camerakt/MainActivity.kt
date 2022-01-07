package com.hyeyun.camerakt

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.hyeyun.camerakt.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {




    val REQUEST_IMAGE_CAPTURE = 1 // 카메라 사진촬영 요청코드
    lateinit var curPhotoPath: String //문자열 형태의 사진 경로 값..lateinit = 늦게 초기화된다.


    val btn_camera = findViewById<Button>(R.id.btn_camera)
    val iv_profile = findViewById<ImageView>(R.id.iv_profile)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        setPermission()//최초의 권한을 수행한다.
        btn_camera.setOnClickListener {

            takeCapture() // 기본 카메라 앱을 실행하여 사진 촬영
        }

    }


    //카메라 촬영 메소드
    private fun takeCapture() {
        //기본 카메라 앱 실행
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also{
            takepictureIntent ->
            takepictureIntent.resolveActivity(packageManager)?.also{
                val photoFile: File? = try{
                    createImageFile()
                }catch(ex: IOException){
                    null
                }
                photoFile?.also{
                    val photoURI : Uri = FileProvider.getUriForFile(
                        this,
                        "com.hyeyun.camerakt.fileprovider",
                        it
                    )
                    takepictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoURI)
                    startActivityForResult(takepictureIntent,REQUEST_IMAGE_CAPTURE)

                }
            }
        }
    }

    //이미지 파일 생성
    private fun createImageFile(): File? {

        val timestamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timestamp}_",".jpg",storageDir)
            .apply { curPhotoPath = absolutePath}
    }

    //테드퍼미션 설정
    private fun setPermission() {
        val permission = object : PermissionListener {
            override fun onPermissionGranted() { // 설정해놓은 위험 권한들이 허용되었을 경우 이곳을 수행합니다.
                Toast.makeText(this@MainActivity, "권한이 허용되었습니다", Toast.LENGTH_SHORT).show()
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) { //설정해놓은 위험 권한들중 거부를 한 경우 이곳을 수행합니다.
                Toast.makeText(this@MainActivity, "권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
            }
        }

        TedPermission.with(this)
            .setPermissionListener(permission)
            .setRationaleMessage("카메라앱을 사용하시려면 권한을 허용해주세요.")
            .setDeniedMessage("권한을 거부하셨습니다.[앱설정]->[권한]항목에서 허용해주세요")
            .setPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,android.Manifest.permission.CAMERA)
            .check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { //스타트액티비티폴리저트를 통해서 기본 카메라 앱으로부터 받아온 사진 결과값
        super.onActivityResult(requestCode, resultCode, data)

        //이미지를 성공적으로 가져왔다면..
        if(requestCode ==  REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK){
            val bitmap : Bitmap
            val file = File(curPhotoPath)
            if(Build.VERSION.SDK_INT < 28)//안드로이드 Pie 버전보다 낮을 경우
            {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver,Uri.fromFile(file))
                iv_profile.setImageBitmap(bitmap)

            }else{
                val decode = ImageDecoder.createSource(
                    this.contentResolver,
                    Uri.fromFile(file)
                )
                bitmap = ImageDecoder.decodeBitmap(decode)
                iv_profile.setImageBitmap(bitmap)

            }
            savePhoto(bitmap)
        }

    }

    //갤러리에 저장
    private fun savePhoto(bitmap: Bitmap) {

        val folderFath = Environment.getExternalStorageDirectory().absolutePath + "/Pictures/" // 사진 폴더로 저장하기 위한 경로 설정
        val timestamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val fileName = "$(timestamp).jpeg"
        val folder = File (folderFath)
        if(!folder.isDirectory) //현재 해당 경로에서 폴더가 존재하는지..
        {
            folder.mkdirs()//make directory

        }
        val out = FileOutputStream(folderFath + fileName)
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,out)
        Toast.makeText(this,"사진이 앨범에 저장되었습니다",Toast.LENGTH_SHORT).show()

    }
}