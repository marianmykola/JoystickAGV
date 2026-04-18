package com.example.JoystickAGV

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.apache.plc4x.java.core.PlcDriverManager

class PlcSettingsActivity : AppCompatActivity() {

    private lateinit var etIp: EditText
    private lateinit var etRack: EditText
    private lateinit var etSlot: EditText
    private lateinit var etDbNumber: EditText
    private lateinit var etOffset: EditText
    private lateinit var etValue: EditText
    private lateinit var tvResponse: TextView
    private lateinit var btnRead: Button
    private lateinit var btnWrite: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plc_settings)

        etIp = findViewById(R.id.etIp)
        etRack = findViewById(R.id.etRack)
        etSlot = findViewById(R.id.etSlot)
        etDbNumber = findViewById(R.id.etDbNumber)
        etOffset = findViewById(R.id.etOffset)
        etValue = findViewById(R.id.etValue)
        tvResponse = findViewById(R.id.tvResponse)
        btnRead = findViewById(R.id.btnRead)
        btnWrite = findViewById(R.id.btnWrite)

        // Default values
        etIp.setText("192.168.0.40")
        etRack.setText("0")
        etSlot.setText("1")

        btnRead.setOnClickListener {
            performPlcOperation("READ")
        }

        btnWrite.setOnClickListener {
            performPlcOperation("WRITE")
        }
    }

    private fun performPlcOperation(operation: String) {
        Thread {
            try {
                val ip = etIp.text.toString()
                val rack = etRack.text.toString().toIntOrNull() ?: 0
                val slot = etSlot.text.toString().toIntOrNull() ?: 1
                val db = etDbNumber.text.toString().toIntOrNull() ?: 0
                val offset = etOffset.text.toString().toIntOrNull() ?: 0

                val s7Driver = PlcDriverManager.getDriverManager()
                val connectionString = "s7://$ip:102?rack=$rack&slot=$slot"
                val connection = s7Driver.getConnection(connectionString)
                connection.connect().get()

                if (operation == "READ") {
                    val readRequest = connection.readRequestBuilder()
                        .addItem("value", "DBD$db:$offset:INT")
                        .build()
                    val response = readRequest.execute().get()
                    val value = (response.getObject("value") as? Number)?.toInt() ?: 0
                    runOnUiThread {
                        tvResponse.text = "Read value: $value"
                        Toast.makeText(this, "Read successful", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val value = etValue.text.toString().toIntOrNull() ?: 0
                    val writeRequest = connection.writeRequestBuilder()
                        .addItem("value", "DBD$db:$offset:INT", value)
                        .build()
                    writeRequest.execute().get()
                    runOnUiThread {
                        tvResponse.text = "Write value: $value"
                        Toast.makeText(this, "Write successful", Toast.LENGTH_SHORT).show()
                    }
                }

                connection.close()
            } catch (e: Exception) {
                runOnUiThread {
                    tvResponse.text = "Error: ${e.message}"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
