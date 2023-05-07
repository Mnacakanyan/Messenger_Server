package com.example.bbb

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bbb.Keys.privateKey
import com.example.bbb.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ServerSocket
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private var Bdash: Double = 0.0
    private val messagesList = mutableListOf<MessageType>()
    private val adapter = MessagesAdapter(messagesList)
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var socket: Socket
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        setupSocket()
        setupRecycler()
    }

    private fun setupSocket() {
        viewBinding.frameLayout.setOnClickListener {
            try {
                if (socket == null) {
                    return@setOnClickListener
                }
            }catch (e: UninitializedPropertyAccessException){
                return@setOnClickListener
            }

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val text = viewBinding.message.text.toString()
                    socket.getOutputStream()
                        .write(text.length + Bdash.toInt())
                    val bytes = ByteArray(text.length)
                    for (i in bytes.indices) {
                        bytes[i] = (text[i].code+Bdash.toInt()).toByte()
                    }
                    socket.getOutputStream()
                        .write(bytes)
                    withContext(Dispatchers.Main) {
                        messagesList.add(
                            MessageType(text, Sender.Server)
                        )
                        adapter.notifyItemInserted(messagesList.size)
                        viewBinding.messages.smoothScrollToPosition(messagesList.size)
                        viewBinding.message.text.clear()
                    }
                }
            }
        }
        val thread = Thread {

            val serverSocket = ServerSocket(4411)
            socket = serverSocket.accept()
            //for deffie-helman
            val clientP = socket.getInputStream().read().toDouble()
            val clientG = socket.getInputStream().read().toDouble()
            val clientA = socket.getInputStream().read().toDouble()
            val B = ((Math.pow(clientG, privateKey.toDouble())) % clientP)
            socket.getOutputStream().write(B.toInt())
            Bdash = ((Math.pow(clientA, privateKey.toDouble())) % clientP)

            val str = StringBuilder()
            while (true){
                val received = socket.getInputStream().read()-Bdash.toInt()
                for (i in 1..received) {
                    str.append((socket.getInputStream().read()-Bdash.toInt()).toChar().toString())
                }
                messagesList.add(MessageType(str.toString(), Sender.User))
                runOnUiThread {
                    adapter.notifyItemInserted(messagesList.size)
                    viewBinding.messages.smoothScrollToPosition(messagesList.size)
                }
                str.clear()
            }
        }
        thread.isDaemon = true
        thread.start()
    }

    private fun setupRecycler() {
        with(viewBinding.messages) {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }
}