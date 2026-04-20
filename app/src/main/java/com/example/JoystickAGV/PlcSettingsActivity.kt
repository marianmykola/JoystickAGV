package com.example.JoystickAGV

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import si.trina.moka7.live.PLC
import com.sourceforge.snap7.moka7.S7

class PlcSettingsActivity : AppCompatActivity() {

    private lateinit var etIp: EditText
    private lateinit var etRack: EditText
    private lateinit var etSlot: EditText
    private lateinit var etDbNumber: EditText
    private lateinit var etOffset: EditText
    private lateinit var etBitPosition: EditText
    private lateinit var etValue: EditText
    private lateinit var etDirectAddress: EditText
    private lateinit var btnParseAddress: Button
    private lateinit var spinnerDataType: Spinner
    private lateinit var tvResponse: TextView
    private lateinit var btnRead: Button
    private lateinit var btnWrite: Button
    private var plc: PLC? = null

    private val dataTypes = arrayOf("INT (2 bytes)", "DINT (4 bytes)", "BOOL (1 bit)")
    private val dataTypeValues = arrayOf("INT", "DINT", "BOOL")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plc_settings)

        etIp = findViewById(R.id.etIp)
        etRack = findViewById(R.id.etRack)
        etSlot = findViewById(R.id.etSlot)
        etDbNumber = findViewById(R.id.etDbNumber)
        etOffset = findViewById(R.id.etOffset)
        etBitPosition = findViewById(R.id.etBitPosition)
        val bitPositionLayout = findViewById<LinearLayout>(R.id.bitPositionLayout)
        etValue = findViewById(R.id.etValue)
        etDirectAddress = findViewById(R.id.etDirectAddress)
        btnParseAddress = findViewById(R.id.btnParseAddress)
        spinnerDataType = findViewById(R.id.spinnerDataType)
        tvResponse = findViewById(R.id.tvResponse)
        btnRead = findViewById(R.id.btnRead)
        btnWrite = findViewById(R.id.btnWrite)

        // Setup bit position field visibility
        spinnerDataType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isBoolType = dataTypeValues[position] == "BOOL"
                bitPositionLayout.visibility = if (isBoolType) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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

        btnParseAddress.setOnClickListener {
            parseDirectAddress()
        }
    }

    private fun parseDirectAddress() {
        val address = etDirectAddress.text.toString().trim()
        
        if (address.isEmpty()) {
            Toast.makeText(this, "Please enter an address", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Parse DB5.DBX380.0 format
            val dbPattern = Regex("""DB(\d+)\.(DBB|DBW|DBD|DBX)(\d+)(?:\.(\d+))?""")
            val match = dbPattern.find(address.uppercase())
            
            if (match != null) {
                val dbNumber = match.groupValues[1].toInt()
                val areaType = match.groupValues[2] // DBB, DBW, DBD, DBX
                val offset = match.groupValues[3].toInt()
                val bitPosition = match.groupValues[4].toIntOrNull()
                
                // Set DB number
                etDbNumber.setText(dbNumber.toString())
                
                // Set offset
                etOffset.setText(offset.toString())
                
                // Determine data type and set bit position if needed
                when (areaType) {
                    "DBX" -> {
                        // Bit access - need bit position
                        if (bitPosition != null && bitPosition in 0..7) {
                            spinnerDataType.setSelection(dataTypeValues.indexOf("BOOL"))
                            etBitPosition.setText(bitPosition.toString())
                        } else {
                            throw IllegalArgumentException("Invalid bit position for DBX. Must be 0-7")
                        }
                    }
                    "DBB" -> {
                        // Byte access - can be INT or BOOL
                        spinnerDataType.setSelection(dataTypeValues.indexOf("INT"))
                    }
                    "DBW" -> {
                        // Word access - INT
                        spinnerDataType.setSelection(dataTypeValues.indexOf("INT"))
                    }
                    "DBD" -> {
                        // Double word access - DINT
                        spinnerDataType.setSelection(dataTypeValues.indexOf("DINT"))
                    }
                }
                
                Toast.makeText(this, "Address parsed successfully!", Toast.LENGTH_SHORT).show()
                
            } else {
                throw IllegalArgumentException("Invalid address format. Use: DB5.DBX380.0 or DB5.DBB372")
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Parse error: ${e.message}", Toast.LENGTH_LONG).show()
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
                val dataType = dataTypeValues[dataTypeIndex]

                // Create PLC instance with correct parameters for S7-1500
                plc = PLC("AGV_PLC", ip, 1024, 1024, db, db, doubleArrayOf(), rack, slot, S7.S7AreaDB, S7.S7AreaDB)

                val value = when (dataType) {
                    "INT" -> {
                        val intValue = plc!!.getInt(true, offset)
                        "INT: $intValue"
                    }
                    "DINT" -> {
                        val dintValue = plc!!.getDInt(true, offset)
                        "DINT: $dintValue"
                    }
                    "BOOL" -> {
                        val bitPos = etBitPosition.text.toString().toIntOrNull() ?: 0
                        if (bitPos < 0 || bitPos > 7) {
                            throw IllegalArgumentException("Bit position must be 0-7")
                        }
                        val boolValue = plc!!.getBool(true, offset, bitPos)
                        "BOOL (bit $bitPos): $boolValue"
                    }
                    else -> "Unknown data type"
                }

                runOnUiThread {
                    tvResponse.text = "Read from DB$db at offset $offset\nData Type: ${dataTypes[dataTypeIndex]}\nValue: $value"
                    Toast.makeText(this@PlcSettingsActivity, "Read successful: $value", Toast.LENGTH_SHORT).show()
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
                val dataType = dataTypeValues[dataTypeIndex]

                // Create PLC instance with correct parameters for S7-1500
                plc = PLC("AGV_PLC", ip, 1024, 1024, db, db, doubleArrayOf(), rack, slot, S7.S7AreaDB, S7.S7AreaDB)

                when (dataType) {
                    "INT" -> {
                        val value = valueStr.toIntOrNull() ?: 0
                        plc!!.putInt(false, offset, value.toShort())
                    }
                    "DINT" -> {
                        val value = valueStr.toIntOrNull() ?: 0
                        plc!!.putDInt(false, offset, value)
                    }
                    "BOOL" -> {
                        val value = valueStr.toBoolean()
                        val bitPos = etBitPosition.text.toString().toIntOrNull() ?: 0
                        if (bitPos < 0 || bitPos > 7) {
                            throw IllegalArgumentException("Bit position must be 0-7")
                        }
                        plc!!.putBool(false, offset, bitPos, value)
                    }
                }

                runOnUiThread {
                    tvResponse.text = "Write to DB$db at offset $offset\nData Type: ${dataTypes[dataTypeIndex]}\nValue: $valueStr"
                    Toast.makeText(this@PlcSettingsActivity, "Write successful: $valueStr", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvResponse.text = "Error: ${e.message}"
                    Toast.makeText(this@PlcSettingsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onDestroy() {
        plc?.let {
            try {
                // Clean up PLC connection
                it.connected = false
            } catch (e: Exception) {
                // Log error but don't crash
                e.printStackTrace()
            }
        }
        plc = null
        super.onDestroy()
    }
}
