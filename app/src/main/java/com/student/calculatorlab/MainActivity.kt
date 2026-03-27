package com.student.calculatorlab

import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val display = findViewById<TextView>(R.id.display)
        val grid = findViewById<GridLayout>(R.id.buttons)

        updateDisplay(display)

        for (i in 0 until grid.childCount) {
            val button = grid.getChildAt(i) as Button

            button.setOnClickListener {
                val text = button.text.toString()

                when (text) {
                    in "0".."9" -> viewModel.inputDigit(text)
                    "." -> viewModel.inputDot()
                    "+", "-", "*", "/" -> viewModel.applyOperation(text)
                    "=" -> viewModel.calculate()
                    "C" -> viewModel.clear()
                }

                updateDisplay(display)
            }
        }
    }

    private fun updateDisplay(display: TextView) {
        display.text = viewModel.displayText
    }
}