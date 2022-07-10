package redstone.libraryfucker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import redstone.libraryfucker.databinding.ActivityMainBinding
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private lateinit var userName: String
    private var userId = 0
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userName = intent.getStringExtra("userName")!!
        userId = intent.getIntExtra("userId", 0)

        binding.reserveButton1.setOnClickListener(ReserveClickListener(this, userId))
        binding.reserveButton2.setOnClickListener(ReserveClickListener(this, userId))
        binding.reserveButton3.setOnClickListener(ReserveClickListener(this, userId))
        binding.reserveButton4.setOnClickListener(ReserveClickListener(this, userId))

        initMenu()
        startRefresher()
    }

    private fun initMenu() {
        binding.materialToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuAccountMgmt -> {
                    MaterialAlertDialogBuilder(this).setTitle(userName).setMessage("退出登录?")
                        .setNegativeButton("不行", null)
                        .setPositiveButton(
                            "好"
                        ) { _, _ ->
                            val anIntent =
                                Intent(this@MainActivity, LoginActivity()::class.java).apply {
                                    putExtra("userName", userName)
                                }
                            with(getPreferences(MODE_PRIVATE).edit()) {
                                putBoolean("logout", true)
                                apply()
                            }
                            startActivity(anIntent)
                            exitProcess(0)
                        }
                        .show()
                    true
                }
                else -> false
            }
        }
    }

    private fun startRefresher() {
        val httpClient = OkHttpClient()
        val request =
            Request.Builder().url("${BuildConfig.serverAddress}/seat_status?userid=$userId").get()
                .build()
        Thread {
            while (true) {
                try {
                        val response = httpClient.newCall(request).execute().body?.string()
                        val statusJSON = JSONObject(response!!)
                        val statusList = arrayListOf<Array<Int>>()
                        var selfReserved = 0
                        if (statusJSON.getBoolean("valid")) {
                            val statusArray = statusJSON.getJSONArray("seats")
                            for (foo in 0 until statusArray.length()) {
                                val thisStatus = statusArray.get(foo) as JSONObject
                                if (thisStatus.getInt("status") == 3) selfReserved =
                                    thisStatus.getInt("seatNum")
                                statusList.add(
                                    arrayOf(
                                        thisStatus.getInt("seatNum"),
                                        thisStatus.getInt("status")
                                    )
                                )
                            }
                            for (aStatus in statusList) {
                                runOnUiThread { setStatus(aStatus[0], aStatus[1], selfReserved) }
                            }
                        } else {
                            Log.i("statusJSON", statusJSON.toString())
                        }
                        Thread.sleep(1000)
                } catch (e: Exception) {
                    runOnUiThread { Toast.makeText(this, "连不上服务器", Toast.LENGTH_SHORT).show() }
                    e.printStackTrace()


                }
            }
        }.start()
    }

    private fun setStatus(n: Int, status: Int, selfRes: Int) {
        val statusDict = arrayOf("空闲", "正在占用", "被预约", "由你预约")
        val colorDict = arrayOf(
            getColor(R.color.desk_avail),
            getColor(R.color.desk_occupied),
            getColor(R.color.desk_reserved),
            getColor(R.color.desk_reserved)
        )
        when (n) {
            1 -> {
                binding.textSeat1Status.text = statusDict[status]
                binding.layoutSeat1.setBackgroundColor(colorDict[status])
                binding.reserveButton1.text = if (status == 3) "取消预约" else "预约"
                binding.reserveButton1.isEnabled =
                    ((selfRes == n) and (status == 3)) or ((status == 0) and (selfRes == 0))
            }
            2 -> {
                binding.textSeat2Status.text = statusDict[status]
                binding.layoutSeat2.setBackgroundColor(colorDict[status])
                binding.reserveButton2.text = if (status == 3) "取消预约" else "预约"
                binding.reserveButton2.isEnabled =
                    ((selfRes == n) and (status == 3)) or ((status == 0) and (selfRes == 0))
            }
            3 -> {
                binding.textSeat3Status.text = statusDict[status]
                binding.layoutSeat3.setBackgroundColor(colorDict[status])
                binding.reserveButton3.text = if (status == 3) "取消预约" else "预约"
                binding.reserveButton3.isEnabled =
                    ((selfRes == n) and (status == 3)) or ((status == 0) and (selfRes == 0))
            }
            4 -> {
                binding.textSeat4Status.text = statusDict[status]
                binding.layoutSeat4.setBackgroundColor(colorDict[status])
                binding.reserveButton4.text = if (status == 3) "取消预约" else "预约"
                binding.reserveButton4.isEnabled =
                    ((selfRes == n) and (status == 3)) or ((status == 0) and (selfRes == 0))
            }
        }

    }
}


class ReserveClickListener(
    private val activity: Activity,
    private val userId: Int
) :
    View.OnClickListener {
    override fun onClick(p0: View?) {
        p0 as Button
        val deskName: String
        when (p0.id) {
            R.id.reserveButton1 -> {
                deskName = "1号座位"
            }
            R.id.reserveButton2 -> {
                deskName = "2号座位"

            }
            R.id.reserveButton3 -> {
                deskName = "3号座位"

            }
            else -> {
                deskName = "4号座位"

            }
        }

        MaterialAlertDialogBuilder(activity).setTitle(deskName)
            .setMessage(if (p0.text == "预约") "确定预约？\n将保留15分钟" else "取消预约")
            .setNegativeButton("不行", null)
            .setPositiveButton(
                "好"
            ) { _, _ ->
                //submit reserve
                val httpClient = OkHttpClient()
                Thread {
                    val formBodyBuilder = FormBody.Builder()
                    formBodyBuilder.add("userId", userId.toString())
                    formBodyBuilder.add(
                        "seatNum", when (p0.id) {
                            R.id.reserveButton1 -> "1"
                            R.id.reserveButton2 -> "2"
                            R.id.reserveButton3 -> "3"
                            else -> "4"
                        }
                    )
                    val formBody = formBodyBuilder.build()
                    try {
                        if (p0.text == "预约") {//Reserving
                            val request =
                                Request.Builder().url("${BuildConfig.serverAddress}/reserve")
                                    .post(formBody).build()

                            val response = httpClient.newCall(request).execute()

                            val res = response.body?.string()
                            if (res == "OCCUPIED") {
                                activity.runOnUiThread {
                                    Toast.makeText(activity, "已经被占用/预约了", Toast.LENGTH_SHORT).show()
                                }
                            }

                            if (res == "SUC") {
                                activity.runOnUiThread {
                                    Toast.makeText(activity, "预约成功了", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {//Cancelling

                            val request =
                                Request.Builder().url("${BuildConfig.serverAddress}/cancel")
                                    .post(formBody).build()

                            val response = httpClient.newCall(request).execute()

                            val res = response.body?.string()
                            if (res == "UNRESERVED") {
                                activity.runOnUiThread {
                                    Toast.makeText(activity, "还没被你被预约", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }

                            if (res == "SUC") {
                                activity.runOnUiThread {
                                    Toast.makeText(activity, "预约取消了", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                    } catch (e: Exception) {
                        activity.runOnUiThread {
                            Toast.makeText(
                                activity,
                                "连不上服务器",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        e.printStackTrace()
                    }
                }.start()


            }.show()
    }
}

