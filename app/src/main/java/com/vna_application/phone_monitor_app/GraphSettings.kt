package com.vna_application.phone_monitor_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import java.lang.Integer.parseInt

class GraphSettings : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph_settings)

        // Defining References of EditTexts
        val smoothingEntry = findViewById<EditText>(R.id.smoothing_entry)

        val XmaxEntry = findViewById<EditText>(R.id.x_max_entry)

        val XminEntry = findViewById<EditText>(R.id.x_min_entry)

        val YmaxEntry = findViewById<EditText>(R.id.y_max_entry)

        val YminEntry = findViewById<EditText>(R.id.y_min_entry)

        // Defining Reference of Button
        val setButton = findViewById<Button>(R.id.setsettingsButton)

        setButton.setOnClickListener {

            // Getting text from EditText
            val smoothingAverage = smoothingEntry.text // Getting text from EditText
            val x_max = XmaxEntry.text
            val x_min = XminEntry.text
            val y_max = YmaxEntry.text
            val y_min = YminEntry.text

            if (smoothingAverage.isEmpty()){ //pass
            } else {
                DataSettings.data_smoothing = parseInt(smoothingAverage.toString())
            }

            if (x_max.isEmpty()){ //pass
            } else if ( (parseInt(x_max.toString()) > DataSettings.Xmin) and (x_min.isEmpty()) ){
                DataSettings.Xmax = parseInt(x_max.toString())
            } else if (parseInt(x_max.toString()) > parseInt(x_min.toString())) {
                DataSettings.Xmax = parseInt(x_max.toString())
            }

            if (x_min.isEmpty()){ //pass
            } else if ( (parseInt(x_min.toString()) < DataSettings.Xmax) and (x_max.isEmpty()) ){
                DataSettings.Xmin = parseInt(x_min.toString())
            } else if (parseInt(x_min.toString()) < parseInt(x_max.toString())) {
                DataSettings.Xmin = parseInt(x_min.toString())
            }

            if (y_max.isEmpty()){ //pass
            } else if ( (parseInt(y_max.toString()) > DataSettings.Ymin) and (y_min.isEmpty()) ){
                DataSettings.Ymax = parseInt(y_max.toString())
            } else if (parseInt(y_max.toString()) > parseInt(y_min.toString())) {
                DataSettings.Ymax = parseInt(y_max.toString())
            }

            if (y_min.isEmpty()){ //pass
            } else if ( (parseInt(y_min.toString()) < DataSettings.Ymax) and (y_max.isEmpty()) ){
                DataSettings.Ymin = parseInt(y_min.toString())
            } else if (parseInt(y_min.toString()) < parseInt(y_max.toString())) {
                DataSettings.Ymin = parseInt(y_min.toString())
            }
            finish()
        }



        // Disables back on phone to go back to previous Activity
        onBackPressedDispatcher.addCallback(this, object:OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                Toast.makeText(this@GraphSettings, "Save Setting Button also works as a Back Button", Toast.LENGTH_SHORT).show()
            }
        })
    }
}