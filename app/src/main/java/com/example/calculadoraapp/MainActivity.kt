package com.example.calculadoraapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.calculadoraapp.R

class MainActivity : AppCompatActivity() {
    private lateinit var tvDisplay: TextView

    private var currentInput: String = ""
    private var operand: Double? = null
    private var pendingOp: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // TextView de display
        tvDisplay = findViewById(R.id.txtResultado)

        // Botões de dígitos
        val digits = listOf(
            "0" to R.id.btn0,
            "1" to R.id.btn1,
            "2" to R.id.btn2,
            "3" to R.id.btn3,
            "4" to R.id.btn4,
            "5" to R.id.btn5,
            "6" to R.id.btn6,
            "7" to R.id.btn7,
            "8" to R.id.btn8,
            "9" to R.id.btn9,
            "." to R.id.btnPonto
        )
        digits.forEach { (digit, id) ->
            findViewById<Button>(id).setOnClickListener { appendDigit(digit) }
        }

        // Botões de operações
        val ops = listOf(
            "+" to R.id.btnSomar,
            "-" to R.id.btnSubtrair,
            "×" to R.id.btnMultiplicar,
            "÷" to R.id.btnDividir
        )
        ops.forEach { (op, id) ->
            findViewById<Button>(id).setOnClickListener { onOperator(op) }
        }

        // Botão igual
        findViewById<Button>(R.id.btnIgual).setOnClickListener { onEquals() }

        // Botão limpar tudo
        findViewById<Button>(R.id.btnClear).setOnClickListener { clearAll() }

        // Botão backspace
        findViewById<Button>(R.id.btnBackspace).setOnClickListener { backspace() }

        updateDisplay()
    }

    private fun appendDigit(d: String) {
        if (d == "." && currentInput.contains(".")) return
        currentInput = if (currentInput == "0") d else currentInput + d
        updateDisplay()
    }

    private fun onOperator(op: String) {
        if (currentInput.isNotEmpty()) {
            val value = currentInput.toDoubleOrNull()
            if (value != null) {
                if (operand == null) operand = value
                else operand = performOperation(operand!!, value, pendingOp)
            }
            currentInput = ""
        }
        pendingOp = op
        updateDisplay()
    }

    private fun onEquals() {
        if (operand != null && currentInput.isNotEmpty()) {
            val value = currentInput.toDoubleOrNull() ?: return
            val result = performOperation(operand!!, value, pendingOp)
            operand = null
            pendingOp = null
            currentInput = result.toString()
            updateDisplay()
        }
    }

    private fun performOperation(a: Double, b: Double, op: String?): Double {
        return when (op) {
            "+" -> a + b
            "-" -> a - b
            "×" -> a * b
            "÷" -> if (b == 0.0) {
                Toast.makeText(this, "Divisão por zero", Toast.LENGTH_SHORT).show()
                a
            } else a / b
            else -> b
        }
    }

    private fun clearAll() {
        currentInput = ""
        operand = null
        pendingOp = null
        updateDisplay()
    }

    private fun backspace() {
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        tvDisplay.text = if (currentInput.isNotEmpty()) currentInput else (operand?.toString() ?: "0")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentInput", currentInput)
        outState.putDouble("operand", operand ?: Double.NaN)
        outState.putString("pendingOp", pendingOp)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentInput = savedInstanceState.getString("currentInput", "")
        val opnd = savedInstanceState.getDouble("operand", Double.NaN)
        operand = if (opnd.isNaN()) null else opnd
        pendingOp = savedInstanceState.getString("pendingOp")
        updateDisplay()
    }
}
