package com.student.calculatorlab

import androidx.lifecycle.ViewModel

class CalculatorViewModel : ViewModel() {

    var displayText: String = "0"
    var firstOperand: Double? = null
    var operation: String? = null
    var waitingForSecond = false

    fun inputDigit(digit: String) {
        if (displayText == "0" || waitingForSecond) {
            displayText = digit
            waitingForSecond = false
        } else {
            displayText += digit
        }
    }

    fun inputDot() {
        if (!displayText.contains(".")) {
            displayText += "."
        }
    }

    fun applyOperation(op: String) {
        firstOperand = displayText.toDoubleOrNull()
        operation = op
        waitingForSecond = true
    }

    fun calculate() {
        val secondOperand = displayText.toDoubleOrNull()
        val first = firstOperand

        if (first == null || secondOperand == null || operation == null) {
            return
        }

        val result = when (operation) {
            "+" -> first + secondOperand
            "-" -> first - secondOperand
            "*" -> first * secondOperand
            "/" -> {
                if (secondOperand == 0.0) {
                    displayText = "Error"
                    return
                }
                first / secondOperand
            }
            else -> return
        }

        displayText = result.toString()
        firstOperand = null
        operation = null
    }

    fun clear() {
        displayText = "0"
        firstOperand = null
        operation = null
        waitingForSecond = false
    }
}