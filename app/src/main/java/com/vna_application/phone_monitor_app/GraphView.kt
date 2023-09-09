package com.vna_application.phone_monitor_app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async

class GraphView : AppCompatActivity() {

    private lateinit var chart:LineChart

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph_view)

        // Value to determine connection to server.
        // Since a connection must be made to get to this activity, initial val = 1 (Connected)
        var connection = 1

        // Getting value of folderName
        val folderName = intent.getStringExtra("folderName")

        // Initializing ServerActivity Object
        val serverSession = ServerActivity(folderName.toString())

        // Initializing Chart
        chart = findViewById(R.id.LineChart)

        // Text to indicate to user nothing is wrong if the chart does not show up immediately
        chart.setNoDataText("Retrieving Data...")
        chart.setNoDataTextColor(resources.getColor(R.color.graph_color)) //setting text color

        chart.setBackgroundColor(resources.getColor(R.color.pink_background)) //set Background Color
        chart.description.isEnabled = false //disables chart description
        chart.setDrawGridBackground(false) //disables default Grid

        //Enables touch gestures
        chart.setTouchEnabled(true)
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)

        // Initializing a xAxis val and setting parameters
        val xAxis = chart.xAxis
        xAxis.enableGridDashedLine(10f,10f,0f)
        xAxis.spaceMax = 10F
        xAxis.position = XAxis.XAxisPosition.BOTTOM

        // Initializing a yAxis val and setting parameters
        val yAxis = chart.axisLeft
        yAxis.enableGridDashedLine(10f, 10f, 0f)
        yAxis.spaceMax = 10F

        // Disables right axis (Only need 1 y-axis)
        chart.axisRight.isEnabled = false

        // Initializing List of all data points
        val dataPoints: ArrayList<Entry> = ArrayList()

        // Initializes data set val
        var dataSet: LineDataSet
        println("Async called")
        // Async function used to grab graphing data for plotting
        val graphingData = lifecycleScope.async {serverSession.getServerData()}

        // Action taken when the async function is finished and an exception isn't thrown
        graphingData.invokeOnCompletion {
            if(it == null){
                val data: List<Data>? = graphingData.getCompleted()

                if(data == null){connection=0; return@invokeOnCompletion} // connection failed

                // iterates through length of data list to add to graph array
                for(i in data.indices){
                    dataPoints.add(Entry(data[i].elapsedTime.toFloat(), data[i].inflectionFrequency.toFloat()))
                }
                // Adds list of values to dataSet
                dataSet = LineDataSet(dataPoints, "Inflection Frequency")
                // Sets desired parameters
                dataSet.setDrawFilled(false)
                dataSet.setDrawValues(false)
                dataSet.setDrawHighlightIndicators(false)
                dataSet.setDrawCircleHole(false)
                dataSet.lineWidth = 3f
                dataSet.circleRadius = 3f
                dataSet.color = resources.getColor(R.color.graph_color)
                dataSet.setCircleColor(resources.getColor(R.color.graph_color))

                // Sets data object as dataset and refreshes view
                chart.data = LineData(dataSet)
                chart.invalidate()
                println("Initial Graph Done")
            }
        }


        // Handler runs the updateGraph Runnable interface
        val graphHandler = Handler(Looper.getMainLooper())

        // updateGraph will run every 5 seconds to update the graph displayed on phone
        // will check for connection, If disconnected, will attempt reconnection.
        // If attempt falls, updateGraph returns and will be run 5 secs later
        // If connected, a shorter version of gathering the data log without the session start
        // will be called
        // Code matches graphingData associated code at the top of this interface

        println("Handler called")
        graphHandler.post(object : Runnable {
            override fun run() {
                if (connection == 0) {
                    // Async function used to grab graphing data for plotting
                    val updatedData = lifecycleScope.async { serverSession.getServerData() }

                    // Action taken when the async function is finished and an exception isn't thrown
                    updatedData.invokeOnCompletion {
                        if (it == null) {
                            val data: List<Data>? = updatedData.getCompleted()

                            if (data == null) {
                                connection = 0
                                println("I am a back to back failed connection")
                                return@invokeOnCompletion // connection failed
                            }

                            connection = 1 // Connection is re-established

                            println("I am an update connection from a previous failed connection")
                            // Clearing data in order to reenter
                            dataPoints.clear()
                            // iterates through length of data list to add to graph array
                            for (i in data.indices) {
                                dataPoints.add(
                                    Entry(
                                        data[i].elapsedTime.toFloat(),
                                        data[i].inflectionFrequency.toFloat()
                                    )
                                )
                            }
                            // Adds list of values to dataSet
                            dataSet = LineDataSet(dataPoints, "Inflection Frequency")
                            // Sets desired parameters
                            dataSet.setDrawFilled(false)
                            dataSet.setDrawValues(false)
                            dataSet.setDrawHighlightIndicators(false)
                            dataSet.setDrawCircleHole(false)
                            dataSet.lineWidth = 3f
                            dataSet.circleRadius = 3f
                            dataSet.color = resources.getColor(R.color.graph_color)
                            dataSet.setCircleColor(resources.getColor(R.color.graph_color))

                            // Sets data object as dataset and refreshes view
                            chart.data = LineData(dataSet)
                            chart.invalidate()
                            println("Graph Updated")
                        }
                    }
                } else {
                    // Async function used to grab graphing data for plotting
                    val updatedData = lifecycleScope.async { serverSession.getServerDataShorter() }

                    // Action taken when the async function is finished and an exception isn't thrown
                    updatedData.invokeOnCompletion {
                        if (it == null) {
                            val data: List<Data>? = updatedData.getCompleted()

                            if (data == null) {
                                connection = 0
                                println("I failed after a previous good connection")
                                return@invokeOnCompletion // connection failed
                            }

                            println("I am an update connection from a previous good connection and I succeeded again")
                            // Clearing data in order to reenter
                            dataPoints.clear()
                            // iterates through length of data list to add to graph array
                            for (i in data.indices) {
                                dataPoints.add(
                                    Entry(
                                        data[i].elapsedTime.toFloat(),
                                        data[i].inflectionFrequency.toFloat()
                                    )
                                )
                            }
                            // Adds list of values to dataSet
                            dataSet = LineDataSet(dataPoints, "Inflection Frequency")
                            // Sets desired parameters
                            dataSet.setDrawFilled(false)
                            dataSet.setDrawValues(false)
                            dataSet.setDrawHighlightIndicators(false)
                            dataSet.setDrawCircleHole(false)
                            dataSet.lineWidth = 3f
                            dataSet.circleRadius = 3f
                            dataSet.color = resources.getColor(R.color.graph_color)
                            dataSet.setCircleColor(resources.getColor(R.color.graph_color))

                            // Sets data object as dataset and refreshes view
                            chart.data = LineData(dataSet)
                            chart.invalidate()
                            println("Graph Updated")
                        }
                    }
                }
                graphHandler.postDelayed(this, 10000)
            }
        })

        // Disables back on phone to go back to previous Activity
        onBackPressedDispatcher.addCallback(this, object:OnBackPressedCallback(true){ override fun handleOnBackPressed() {} })
    }

}