package redstone.libraryfucker

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import redstone.libraryfucker.BuildConfig.serverAddress
import redstone.libraryfucker.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var sharedPref: SharedPreferences

    //private val serverAddress = "http://192.168.2.104:2333"
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //check if there's a login history
        sharedPref = getPreferences(Context.MODE_PRIVATE)
        var userName = sharedPref.getString("userName", "NO_FUCKING_USERNAME")
        val isLogout = sharedPref.getBoolean("logout", false)
        if (isLogout) {
            userName = intent.getStringExtra("userName")
            binding.textUsernme.setText(userName)
        } else {
            if (userName != "NO_FUCKING_USERNAME") {
                val passwd = sharedPref.getString("passwd", "shit")
                binding.textUsernme.setText(userName)
                submitLogin(userName!!, passwd!!)
            }
        }

        //the login/reg switch
        binding.buttonSW.setOnClickListener {
            if (binding.buttonSW.text == "注册") {
                binding.buttonSW.text = "登录"
                binding.loginLayout.visibility = View.GONE
                binding.regLayout.visibility = View.VISIBLE

            } else {
                binding.buttonSW.text = "注册"
                binding.regLayout.visibility = View.GONE
                binding.loginLayout.visibility = View.VISIBLE

            }
        }

        //the login button
        binding.buttonLogin.setOnClickListener {
            val userNameInput = binding.textUsernme.text.toString()
            val passwdInput = binding.textPasswd.text.toString()
            if (userNameInput == "") {
                Toast.makeText(this, "输用户名！", Toast.LENGTH_SHORT).show()
            } else {
                if (passwdInput == "") {
                    Toast.makeText(this, "输密码！", Toast.LENGTH_SHORT).show()
                } else {

                    submitLogin(userNameInput, passwdInput)
                }
            }

        }
        //the reg button
        binding.buttonReg.setOnClickListener {
            val userNameInput = binding.textUsernmeReg.text.toString()
            val passwdInput = binding.textPasswdReg.text.toString()
            val passwdCInput = binding.textPasswdConfirm.text.toString()
            if (userNameInput == "") {
                Toast.makeText(this, "输用户名！", Toast.LENGTH_SHORT).show()
            } else {
                if (passwdInput == "") {
                    Toast.makeText(this, "输密码！", Toast.LENGTH_SHORT).show()
                } else {
                    if (passwdInput.length < 6) {
                        Toast.makeText(this, "密码至少6位！", Toast.LENGTH_SHORT).show()
                    } else {
                        if (passwdInput != passwdCInput) {
                            Toast.makeText(this, "密码输得不一样！", Toast.LENGTH_SHORT).show()
                        } else {
                            submitReg(userNameInput, passwdInput)
                        }
                    }
                }
            }
        }
    }

    private fun submitLogin(userName: String, passwd: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonLogin.isEnabled = false
        binding.buttonSW.isEnabled = false
        val httpClient = OkHttpClient()
        Thread {
            val formBodyBuilder = FormBody.Builder()
            formBodyBuilder.add("userName", userName)
            formBodyBuilder.add("passwd", passwd)
            val formBody = formBodyBuilder.build()
            val request = Request.Builder().url("$serverAddress/login").post(formBody).build()
            try {
                val response = httpClient.newCall(request).execute()
                val resJSON = response.body?.string()?.let { JSONObject(it) }
                if (resJSON!!.getString("status") == "NO_USER") {
                    runOnUiThread { Toast.makeText(this, "没这用户", Toast.LENGTH_SHORT).show() }
                }
                if (resJSON.getString("status") == "INV_PASS") {
                    runOnUiThread { Toast.makeText(this, "密码不对", Toast.LENGTH_SHORT).show() }
                }
                if (resJSON.getString("status") == "SUC") {
                    runOnUiThread {
                        with(sharedPref.edit()) {
                            putString("userName", userName)
                            putString("passwd", passwd)
                            putBoolean("logout", false)
                            apply()
                        }
                        val intent = Intent(this, MainActivity()::class.java).apply {
                            putExtra("userName", userName)
                            putExtra("userId", resJSON.getInt("userId"))
                        }
                        startActivity(intent)
                        finish()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "连不上服务器", Toast.LENGTH_SHORT).show() }
                e.printStackTrace()
            }
            runOnUiThread {
                binding.buttonLogin.isEnabled = true
                binding.buttonSW.isEnabled = true
                binding.progressBar.visibility = View.INVISIBLE
            }

        }.start()

    }

    private fun submitReg(userName: String, passwd: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonLogin.isEnabled = false
        binding.buttonSW.isEnabled = false
        val httpClient = OkHttpClient()
        val formBodyBuilder = FormBody.Builder()
        formBodyBuilder.add("userName", userName)
        formBodyBuilder.add("passwd", passwd)
        val formBody = formBodyBuilder.build()
        val request = Request.Builder().url("$serverAddress/register").post(formBody).build()
        val call = httpClient.newCall(request)
        Thread {
            if (!call.isExecuted()) {
                try {
                    val response = call.execute()
                    val resJSON = response.body?.string()?.let { JSONObject(it) }

                    if (resJSON!!.getString("status") == "USER_EXIST") {
                        runOnUiThread { Toast.makeText(this, "已经有这个名了", Toast.LENGTH_SHORT).show() }
                    }

                    if (resJSON.getString("status") == "SUC") {
                        runOnUiThread {
                            Toast.makeText(this, "注册完事了", Toast.LENGTH_SHORT).show()
                            with(sharedPref.edit()) {
                                putString("userName", userName)
                                putString("passwd", passwd)
                                putBoolean("logout", false)
                                apply()
                            }
                            val intent = Intent(this, MainActivity()::class.java).apply {
                                putExtra("userName", userName)
                                putExtra("userId", resJSON.getInt("userId"))
                            }
                            startActivity(intent)
                            finish()
                        }
                    }

                } catch (e: Exception) {
                    runOnUiThread { Toast.makeText(this, "连不上服务器", Toast.LENGTH_SHORT).show() }
                    e.printStackTrace()
                }

                runOnUiThread {
                    binding.progressBar.visibility = View.INVISIBLE
                    binding.buttonLogin.isEnabled = true
                    binding.buttonSW.isEnabled = true
                }
            }
        }.start()
    }
}