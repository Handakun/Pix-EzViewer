package com.perol.asdpl.pixivez.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.databinding.ActivitySaucenaoBinding
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.services.GlideApp
import com.perol.asdpl.pixivez.services.SaucenaoService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_saucenao.*
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.jsoup.Jsoup
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.io.File


class SaucenaoActivity : RinkActivity() {

    private val IMAGE = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.Themeinit(this)
        val binding = DataBindingUtil.setContentView<ActivitySaucenaoBinding>(this, R.layout.activity_saucenao)
        fab.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, IMAGE)

        }
        val httpLoggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                Log.d("aaa", "$message")
            }
        })
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val builder1 = OkHttpClient.Builder()
        builder1.addInterceptor(httpLoggingInterceptor)
        val client = builder1.build()
        val service: Retrofit = Retrofit.Builder()
                .baseUrl(
                        "https://saucenao.com")
                .client(client)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        ssl.isChecked = false
//        ssl.setOnCheckedChangeListener { buttonView, isChecked ->
//            if (!isChecked) {
//                service = Retrofit.Builder()
//                        .baseUrl(
//                                "http://saucenao.com")
//                        .client(client)
//                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
//                        .build();
//
//            } else {
//                service = Retrofit.Builder()
//                        .baseUrl(
//                                "https://saucenao.com")
//                        .client(client)
//                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
//                        .build();
//
//            }
//            api = service.create(SaucenaoService::class.java)
//        }

        api = service.create(SaucenaoService::class.java)

        if (intent != null) {
            val action = intent.action;
            val type = intent.getType();
            if (action != null && type != null)
                if (action.equals(Intent.ACTION_SEND) && type.startsWith("image/")) {
                    val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM);
                    if (uri != null) {
                        var res: String? = null
                        val proj = arrayOf(MediaStore.Images.Media.DATA)
                        val cursor = getContentResolver().query(uri, proj, null, null, null)
                        if (cursor == null) {
                            return
                        }
                        if (cursor.moveToFirst()) {
                            val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                            res = cursor.getString(column_index)
                        }
                        cursor.close();
                        Toasty.success(this, "解析成功正在上传", Toast.LENGTH_SHORT).show()
                        val file = File(res)
                        val builder = MultipartBody.Builder()
                        builder.setType(MultipartBody.FORM)
                        val body = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
                        builder.addFormDataPart("file", file.name, body)
                        api.searchpicforresult(builder.build().part(0)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
                            Toasty.success(this, "上传成功，正在进行匹配", Toast.LENGTH_SHORT).show()
                            trytoParseHtml(it.string())
                        }, { Toasty.error(this, "服务器或本地发生错误" + it.message).show() }, {
                        })
                    }
                }
        }

    }

    lateinit var api: SaucenaoService
    fun trytosearch(path: String) {
        val file = File(path)
        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)
        val body = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
        builder.addFormDataPart("file", file.name, body)
        api.searchpicforresult(builder.build().part(0)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
            Toasty.success(this, "上传成功，正在进行匹配", Toast.LENGTH_SHORT).show()
            trytoParseHtml(it.string())
        }, { Toasty.error(this, "服务器或本地发生错误" + it.message).show() }, {
        })
    }

    fun trytoParseHtml(string: String) {
        val arrayList = ArrayList<Long>()
        runBlocking {
            val doc = Jsoup.parse(string)
            val el = doc.select("a[href]");
            for (i in el.indices) {
                val string = el[i].attr("href")
                Log.d("w", string)
                if (string.contains("https://www.pixiv.net/member_illust.php?mode=medium&illust_id=")) {
                    val id = string.replace("https://www.pixiv.net/member_illust.php?mode=medium&illust_id=", "").toLong()
                    arrayList.add(id)
                }

            }
        }
        val bundle = Bundle()

if (arrayList.isNotEmpty()){
    val it = arrayList.toLongArray()
    bundle.putLongArray("illustlist", it)
    bundle.putLong("illustid", it[0])
    val intent2 = Intent(applicationContext, PictureActivity::class.java)
    intent2.putExtras(bundle)
    startActivity(intent2)
}else         GlideApp.with(this).load(ContextCompat.getDrawable(this, R.drawable.buzhisuocuo)).into(imageview)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()

        }
        return super.onOptionsItemSelected(item)
    }

    var path: String? = null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImage = data.getData();
            val filePathColumns = arrayOf(MediaStore.Images.Media.DATA);
            val c = getContentResolver().query(selectedImage!!, filePathColumns, null, null, null);
            c!!.moveToFirst();
            val columnIndex = c.getColumnIndex(filePathColumns[0]);
            val imagePath = c.getString(columnIndex);
            trytosearch(imagePath);
            c.close();
        }
    }
}