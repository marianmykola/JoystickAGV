package com.example.JoystickAGV

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import si.trina.moka7.live.S7Client

class PlcSettingsActivity : AppCompatActivity() {

    private lateinit var etIp: EditText
    private lateinit var etRack: EditText
    private lateinit var etSlot: EditText
    private lateinit var etDbNumber: EditText
    private lateinit var etOffset: EditText
    private lateinit var etValue: EditText
    private lateinit var spinnerDataType: Spinner
    private lateinit var tvResponse: TextView
    private lateinit var btnRead: Button
    private lateinit var btnWrite: Button
    private var s7Client: S7Client? = null

    private val dataTypes = arrayOf("INT (2 bytes)", "WORD (2 bytes)", "BYTE (1 byte)", "REAL (4 bytes)")
    private val dataTypeValues = arrayOf(S7Client.S7WLInt, S7Client.S7WLWord, S7Client.S7WLByte, S7Client.S7WLReal)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plc_settings)

        etIp = findViewById(R.id.etIp)
        etRack = findViewById(R.id.etRack)
        etSlot = findViewById(R.id.etSlot)
        etDbNumber = findViewById(R.id.etDbNumber)
        etOffset = findViewById(R.id.etOffset)
        etValue = findViewById(R.id.etValue)
        spinnerDataType = findViewById(R.id.spinnerDataType)
        tvResponse = findViewById(R.id.tvResponse)
        btnRead = findViewById(R.id.btnRead)
        btnWrite = findViewById(R.id.btnWrite)

        // Setup data type spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dataTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDataType.adapter = adapter
        spinnerDataType.setSelection(0) // Default to INT

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
                val dataTypeIndex = spinnerDataType.selectedItemPosition
                val wordLen = dataTypeValues[dataTypeIndex]

                s7Client = S7Client.createClient()
                val result = s7Client!!.connectTo(ip, rack, slot)

                if (result == 0) {
                    val buffer = when (wordLen) {
                        S7Client.S7WLByte -> ByteArray(1)
                        S7Client.S7WLWord, S7Client.S7WLInt -> ByteArray(2)
                        S7Client.S7WLReal -> ByteArray(4)
                        else -> ByteArray(2)
                    }

                    s7Client!!.readArea(S7Client.S7AreaDB, db, offset, 1, wordLen, buffer)
                    val value = parseValueFromBuffer(buffer, wordLen)
                    s7Client!!.disconnect()

                    runOnUiThread {
                        tvResponse.text = "Read from DB$db at offset $offset\nData Type: ${dataTypes[dataTypeIndex]}\nValue: $value"
                        Toast.makeText(this@PlcSettingsActivity, "Read successful: $value", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        tvResponse.text = "Connection error: $result"
                        Toast.makeText(this@PlcSettingsActivity, "Connection failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvResponse.text = "Error: ${e.message}"
                    Toast.makeText(this@PlcSettingsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
                val valueStr = etValue.text.toString()
                val dataTypeIndex = spinnerDataType.selectedItemPosition
                val wordLen = dataTypeValues[dataTypeIndex]

                s7Client = S7Client.createClient()
                val result = s7Client!!.connectTo(ip, rack, slot)

                if (result == 0) {
                    val buffer = createBufferFromValue(valueStr, wordLen)

                    s7Client!!.writeArea(S7Client.S7AreaDB, db, offset, 1, wordLen, buffer)
                    s7Client!!.disconnect()

                    runOnUiThread {
                        tvResponse.text = "Write to DB$db at offset $offset\nData Type: ${dataTypes[dataTypeIndex]}\nValue: $valueStr"
                        Toast.makeText(this@PlcSettingsActivity, "Write successful: $valueStr", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        tvResponse.text = "Connection error: $result"
                        Toast.makeText(this@PlcSettingsActivity, "Connection failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvResponse.text = "Error: ${e.message}"
                    Toast.makeText(this@PlcSettingsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun parseValueFromBuffer(buffer: ByteArray, wordLen: Int): String {
        return when (wordLen) {
            S7Client.S7WLByte -> {
                (buffer[0].toInt() and 0xFF).toString()
            }
            S7Client.S7WLWord -> {
                ((buffer[0].toInt() and 0xFF) or ((buffer[1].toInt() and 0xFF) shl 8)).toString()
            }
            S7Client.S7WLInt -> {
                val value = ((buffer[0].toInt() and 0xFF) or ((buffer[1].toInt() and 0xFF) shl 8))
                if (value > 32767) (value - 65536) else value
            }.toString()
            S7Client.S7WLReal -> {
                val bits = ((buffer[0].toInt() and 0xFF) or
                           ((buffer[1].toInt() and 0xFF) shl 8) or
                           ((buffer[2].toInt() and 0xFF) shl 16) or
                           ((buffer[3].toInt() and 0xFF) shl 24))
                java.lang.Float.intBitsToFloat(bits).toString()
            }
            else -> "Unknown"
        }
    }

    private fun createBufferFromValue(valueStr: String, wordLen: Int): ByteArray {
        return when (wordLen) {
            S7Client.S7WLByte -> {
                val value = valueStr.toIntOrNull() ?: 0
                byteArrayOf((value and 0xFF).toByte())
            }
            S7Client.S7WLWord -> {
                val value = valueStr.toIntOrNull() ?: 0
                byteArrayOf(
                    (value and 0xFF).toByte(),
                    ((value shr 8) and 0xFF).toByte()
                )
            }
            S7Client.S7WLInt -> {
                val value = valueStr.toIntOrNull() ?: 0
                val intValue = if (value < 0) value + 65536 else value
                byteArrayOf(
                    (intValue and 0xFF).toByte(),
                    ((intValue shr 8) and 0xFF).toByte()
                )
            }
            S7Client.S7WLReal -> {
                val value = valueStr.toFloatOrNull() ?: 0.0f
                val bits = java.lang.Float.floatToIntBits(value)
                byteArrayOf(
                    (bits and 0xFF).toByte(),
                    ((bits shr 8) and 0xFF).toByte(),
                    ((bits shr 16) and 0xFF).toByte(),
                    ((bits shr 24) and 0xFF).toByte()
                )
            }
            else -> byteArrayOf(0, 0)
        }
    }

    override fun onDestroy() {
        s7Client?.disconnect()
        super.onDestroy()
    }
}
