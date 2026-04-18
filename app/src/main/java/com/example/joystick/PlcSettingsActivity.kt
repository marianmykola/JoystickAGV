package com.example.JoystickAGV

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import eu.mellotec.moka7.Moka7
import eu.mellotec.moka7.types.S7Client

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
    private var s7Client: S7Client? = null

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
            readFromPLC()
        }

        btnWrite.setOnClickListener {
            writeToPLC()
        }
    }

    private fun readFromPLC() {
        Thread {
            try {
                val ip = etIp.text.toString()
                val rack = etRack.text.toString().toIntOrNull() ?: 0
                val slot = etSlot.text.toString().toIntOrNull() ?: 1
                val db = etDbNumber.text.toString().toIntOrNull() ?: 0
                val offset = etOffset.text.toString().toIntOrNull() ?: 0

                s7Client = S7Client.createClient()
                val result = s7Client!!.connectTo(ip, rack, slot)

                if (result == 0) {
                    val buffer = ByteArray(2)
                    s7Client!!.readArea(Moka7.S7AreaDB, db, offset, 1, Moka7.S7WLByte, buffer)
                    val value = ((buffer[0].toInt() and 0xFF) shl 8) or (buffer[1].toInt() and 0xFF)
                    s7Client!!.disconnect()

                    runOnUiThread {
                        tvResponse.text = "Read from DB$db at offset $offset\nValue: $value"
                        Toast.makeText(this, "Read successful: $value", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        tvResponse.text = "Connection error: $result"
                        Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvResponse.text = "Error: ${e.message}"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun writeToPLC() {
        Thread {
            try {
                val ip = etIp.text.toString()
                val rack = etRack.text.toString().toIntOrNull() ?: 0
                val slot = etSlot.text.toString().toIntOrNull() ?: 1
                val db = etDbNumber.text.toString().toIntOrNull() ?: 0
                val offset = etOffset.text.toString().toIntOrNull() ?: 0
                val value = etValue.text.toString().toIntOrNull() ?: 0

                s7Client = S7Client.createClient()
                val result = s7Client!!.connectTo(ip, rack, slot)

                if (result == 0) {
                    val buffer = ByteArray(2)
                    buffer[0] = ((value shr 8) and 0xFF).toByte()
                    buffer[1] = (value and 0xFF).toByte()
                    s7Client!!.writeArea(Moka7.S7AreaDB, db, offset, 1, Moka7.S7WLByte, buffer)
                    s7Client!!.disconnect()

                    runOnUiThread {
                        tvResponse.text = "Write to DB$db at offset $offset\nValue: $value"
                        Toast.makeText(this, "Write successful: $value", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        tvResponse.text = "Connection error: $result"
                        Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvResponse.text = "Error: ${e.message}"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onDestroy() {
        s7Client?.disconnect()
        super.onDestroy()
    }
}
