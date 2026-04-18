package com.example.JoystickAGV

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.s7connector.api.S7Connector
import com.github.s7connector.api.factory.S7ConnectorFactory
import com.github.s7connector.impl.utils.S7Type

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

                val connector: S7Connector = S7ConnectorFactory.buildTCPConnector()
                    .withHost(ip)
                    .withRack(rack)
                    .withSlot(slot)
                    .build()

                connector.connect()

                if (operation == "READ") {
                    val data = connector.read(S7Type.DB, db, offset, S7Type.INT, 1)
                    val value = data[0] as Int
                    runOnUiThread {
                        tvResponse.text = "Read value: $value"
                        Toast.makeText(this, "Read successful", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val value = etValue.text.toString().toIntOrNull() ?: 0
                    connector.write(S7Type.DB, db, offset, S7Type.INT, value)
                    runOnUiThread {
                        tvResponse.text = "Write value: $value"
                        Toast.makeText(this, "Write successful", Toast.LENGTH_SHORT).show()
                    }
                }

                connector.close()
            } catch (e: Exception) {
                runOnUiThread {
                    tvResponse.text = "Error: ${e.message}"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}