package com.example.calculadoraapp

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.calculadoraapp.R
import java.lang.Math.log
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.log10 // Import for log operation

class MainActivity : AppCompatActivity() {
    private lateinit var tvExpressao: TextView
    private lateinit var tvResultado: TextView

    private var currentInput: String = ""
    private var operand: Double? = null
    private var pendingOp: String? = null
    private var expression: String = ""
    private var justComputed: Boolean = false
    private val digits = listOf(
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

    private val ops = listOf(
        "+" to R.id.btnSomar,
        "-" to R.id.btnSubtrair,
        "×" to R.id.btnMultiplicar,
        "÷" to R.id.btnDividir,
        "%" to R.id.btnPercent,
        "aᵇ" to R.id.btnPow,
        "√" to R.id.btnRoot,
        "log" to R.id.btnLog
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvExpressao = findViewById(R.id.txtExpressao)
        tvResultado = findViewById(R.id.txtResultado)

        // Botões de dígitos
        digits.forEach { (digit, id) ->
            findViewById<Button>(id).setOnClickListener { appendDigit(digit) }
        }

        // Botões de operações
        ops.forEach { (op, id) ->
            findViewById<Button>(id).setOnClickListener { onOperator(op) }
        }

        // Botão igual
        findViewById<Button>(R.id.btnIgual).setOnClickListener { onEquals() }

        // Botão limpar tudo
        findViewById<Button>(R.id.btnClear).setOnClickListener { clearAll() }

        // Botão backspace
        findViewById<Button>(R.id.btnBackspace).setOnClickListener { backspace() }

        if (savedInstanceState != null) {
            currentInput = savedInstanceState.getString("currentInput", "")
            val opnd = savedInstanceState.getDouble("operand", Double.NaN)
            operand = if (opnd.isNaN()) null else opnd
            pendingOp = savedInstanceState.getString("pendingOp")
            expression = savedInstanceState.getString("expression", "")
            justComputed = savedInstanceState.getBoolean("justComputed", false)
        }

        // destaque inicial: expressão em destaque, resultado normal
        highlightExpression(true)
        highlightResult(false)

        updateDisplay()
    }

    private fun appendDigit(d: String) {
        if (justComputed) {
            // inicia nova conta ao digitar depois de ter calculado
            currentInput = ""
            expression = ""
            operand = null
            pendingOp = null
            justComputed = false
            highlightExpression(true)
            highlightResult(false)
        }
        if (d == "." && currentInput.contains(".")) return
        currentInput = if (currentInput == "0" && d != ".") d else currentInput + d
        expression += d
        updateDisplay()
    }

    private fun onOperator(op: String) {
        if (justComputed) {
            // usar resultado como primeiro operando
            operand = currentInput.toDoubleOrNull()
            expression = stripTrailingZero(operand ?: 0.0)
            justComputed = false
            currentInput = ""
            highlightExpression(true)
            highlightResult(false)
        }

        if (currentInput.isNotEmpty()) {
            val value = currentInput.toDoubleOrNull()
            if (value != null) {
                if (operand == null) operand = value
                else operand = performOperation(operand!!, value, pendingOp)
            }
            currentInput = ""
        }
        // substitui operador se o usuário apertar operador duas vezes
        val trimmed: String = expression.trimEnd()
        val operatorList = ops.map { it.first }  // List of operators
        val lastOperator = operatorList.filter { trimmed.endsWith(it) }.lastOrNull()  // Find the last operator

        if (lastOperator != null) {
            expression = trimmed.dropLast(lastOperator.length) + op + " "
        } else {
            expression = "(" + expression + ")" + " $op "
        }

        pendingOp = op
        updateDisplay()
    }

    private fun onEquals() {
        // se não há operação pendente, mas há um número, mostra "= número"
        val value = currentInput.toDoubleOrNull()
        if (pendingOp == null) {
            if (currentInput.isNotEmpty()) {
                // expressão permanece como está; resultado marcado com "="
                tvExpressao.text = expression.trim()
                tvResultado.text = "= ${stripTrailingZero(value ?: 0.0)}"
                justComputed = true
                highlightExpression(false)
                highlightResult(true)
            }
            return
        }

        if (operand == null && value != null) {
            operand = value
        }

        if (operand != null) {
            val b = value ?: operand!!
            val result = performOperation(operand!!, b, pendingOp)

            // expressão fica no topo (sem '='); resultado abaixo com '=' na frente
            tvExpressao.text = expression.trim()
            tvResultado.text = "= ${stripTrailingZero(result)}"

            // prepara estado para próximo uso
            currentInput = stripTrailingZero(result)
            operand = null
            pendingOp = null
            justComputed = true

            // destaque agora vai para o resultado
            highlightExpression(false)
            highlightResult(true)
        }
    }

    private fun performOperation(a: Double, b: Double, op: String?): Double {
        return when (op) {
            "+" -> a + b
            "-" -> a - b
            "×" -> a * b
            "%" -> a * b / 100 // Percentage operation
            "√" -> if (b > 0) a.pow(1 / b) else {
                Toast.makeText(this, "Raiz com índice inválido", Toast.LENGTH_SHORT).show()
                0.0
            }
            "aᵇ" -> a.pow(b) // Power operation
            "log" -> if (b > 0 && a > 0) ln(a) / ln(b) else {
                Toast.makeText(this, "Logaritmo de número não positivo", Toast.LENGTH_SHORT).show()
                0.0
            }
            "÷" -> if (b == 0.0) {
                Toast.makeText(this, "Divisão por zero", Toast.LENGTH_SHORT).show()
                a
            } else a / b
            else -> b
        }
    }

    private fun calculatePreview() {
        // mostra cálculo parcial no tvResultado (sem destaque e sem '=')
        if (operand != null && pendingOp != null && currentInput.isNotEmpty()) {
            val value = currentInput.toDoubleOrNull()
            if (value != null) {
                val result = performOperation(operand!!, value, pendingOp)
                tvResultado.text = stripTrailingZero(result)
                return
            }
        }

        // se não há operação parcial, mostra o número atual ou operando
        tvResultado.text = if (currentInput.isNotEmpty()) currentInput else (operand?.let { stripTrailingZero(it) } ?: "0")
    }

    private fun clearAll() {
        currentInput = ""
        operand = null
        pendingOp = null
        expression = ""
        justComputed = false
        highlightExpression(true)
        highlightResult(false)
        updateDisplay()
    }

    private fun backspace() {
        if (justComputed) {
            // limpa tudo se tentar apagar após cálculo
            clearAll()
            return
        }
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            if (expression.isNotEmpty()) {
                expression = expression.dropLast(1)
            }
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        tvExpressao.text = expression.trim()
        if (justComputed) {
            // mostra resultado final com '=' na frente
            tvResultado.text = "= $currentInput"
        } else {
            calculatePreview()
        }
    }

    private fun stripTrailingZero(d: Double): String {
        return if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
    }

    private fun highlightExpression(on: Boolean) {
        if (on) {
            tvExpressao.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
            tvExpressao.setTypeface(null, Typeface.BOLD)
            // resultado volta a estilo normal
            tvResultado.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            tvResultado.setTypeface(null, Typeface.NORMAL)
        } else {
            tvExpressao.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            tvExpressao.setTypeface(null, Typeface.NORMAL)
        }
    }

    private fun highlightResult(on: Boolean) {
        if (on) {
            tvResultado.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36f)
            tvResultado.setTypeface(null, Typeface.BOLD)
            // expressão volta a estilo normal
            tvExpressao.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            tvExpressao.setTypeface(null, Typeface.NORMAL)
        } else {
            tvResultado.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            tvResultado.setTypeface(null, Typeface.NORMAL)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentInput", currentInput)
        outState.putDouble("operand", operand ?: Double.NaN)
        outState.putString("pendingOp", pendingOp)
        outState.putString("expression", expression)
        outState.putBoolean("justComputed", justComputed)
    }
}
