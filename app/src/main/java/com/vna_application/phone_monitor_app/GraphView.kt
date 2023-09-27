package com.vna_application.phone_monitor_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
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

        // Defining Reference of SSH Button
        val settingsButton = findViewById<ImageButton>(R.id.settings)

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

        chart.setBackgroundColor(resources.getColor(R.color.white)) //set Background Color
        chart.description.isEnabled = false //disables chart description
        chart.setDrawGridBackground(false) //disables default Grid

        //Disabling touch gestures
        chart.setTouchEnabled(false)

        // Setting xAxis parameters
        chart.xAxis.enableGridDashedLine(10f,10f,0f)
        chart.xAxis.spaceMax = 10F
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.axisMinimum = DataSettings.Xmin.toFloat()
        chart.xAxis.axisMinimum = DataSettings.Xmax.toFloat()

        // Setting axisLeft parameters
        chart.axisLeft.enableGridDashedLine(10f, 10f, 0f)
        chart.axisLeft.spaceMax = 10F
        chart.axisLeft.axisMinimum = DataSettings.Ymin.toFloat()
        chart.axisLeft.axisMaximum = DataSettings.Ymax.toFloat()

        // Disables right axis (Only need 1 y-axis)
        chart.axisRight.isEnabled = false

        // Initializing List of all data points
        val dataPoints: ArrayList<Entry> = ArrayList()

        // Initializes data set val
        var dataSet: LineDataSet
        // Async function used to grab graphing data for plotting
        val graphingData = lifecycleScope.async {serverSession.getServerData()}

        // Action taken when the async function is finished and an exception isn't thrown
        graphingData.invokeOnCompletion {
            if(it == null){
                val data_pair: Pair<List<Data>?, MutableList<Data>> = graphingData.getCompleted()

                var data = data_pair.first

                if(data_pair.first == null){connection=0; data = data_pair.second} // connection failed

                if(data_pair.first!!.size < data_pair.second.size) {data = data_pair.second}

                val elapsedTimeList = mutableListOf<Float>()
                val frequencyList = mutableListOf<Float>()

                if (data != null) {
                    for(i in data.indices){
                        elapsedTimeList.add(data[i].elapsedTime.toFloat())
                        frequencyList.add(data[i].inflectionFrequency.toFloat())
                    }
                } else {return@invokeOnCompletion}

                // If statement placed here so that the data smoothing only starts as soon as there's enough data
                if (data.size > DataSettings.data_smoothing) {
                    val frequencyMovingAverage = frequencyList.windowed(DataSettings.data_smoothing, 1) { it.average() }

                    // iterates through length of data list to add to graph array
                    for (i in frequencyMovingAverage.indices) {
                        dataPoints.add(Entry(elapsedTimeList[i + (DataSettings.data_smoothing - 1)], frequencyMovingAverage[i].toFloat()))
                    }
                } else {
                    for (i in frequencyList.indices) {
                        dataPoints.add(Entry(elapsedTimeList[i], frequencyList[i]))
                    }
                }

                // Adds list of values to dataSet
                dataSet = LineDataSet(dataPoints, "Inflection Frequency")
                // Sets desired parameters
                dataSet.setDrawFilled(false)
                dataSet.setDrawValues(false)
                dataSet.setDrawHighlightIndicators(false)
                dataSet.setDrawCircleHole(false)
                dataSet.setDrawCircles(false)
                dataSet.lineWidth = 3f
                dataSet.color = resources.getColor(R.color.graph_color)

                // Sets data object as dataset and refreshes view
                chart.data = LineData(dataSet)
                chart.invalidate()
            }
        }


        // Handler runs the updateGraph Runnable interface
        val graphHandler = Handler(Looper.getMainLooper())

        // updateGraph will run every 10 seconds to update the graph displayed on phone
        // will check for connection, If disconnected, will attempt reconnection.
        // If attempt falls, updateGraph returns and will be run 5 secs later
        // If connected, a shorter version of gathering the data log without the session start
        // will be called
        // Code matches graphingData associated code at the top of this interface

        graphHandler.post(object : Runnable {
            override fun run() {
                if (connection == 0) {
                    // Async function used to grab graphing data for plotting
                    val updatedData = lifecycleScope.async { serverSession.getServerData() }

                    // Action taken when the async function is finished and an exception isn't thrown
                    updatedData.invokeOnCompletion {
                        if (it == null) {
                            val data_pair: Pair<List<Data>?, MutableList<Data>> = graphingData.getCompleted()

                            var data = data_pair.first

                            if(data_pair.first == null){
                                connection=0
                                data = data_pair.second
                            } // connection failed

                            if(data_pair.first!!.size < data_pair.second.size) {data = data_pair.second}

                            connection = 1 // Connection is re-established

                            // Clearing data in order to reenter
                            dataPoints.clear()

                            val elapsedTimeList = mutableListOf<Float>()
                            val frequencyList = mutableListOf<Float>()

                            if (data != null) {
                                for(i in data!!.indices){
                                    elapsedTimeList.add(data!![i].elapsedTime.toFloat())
                                    frequencyList.add(data!![i].inflectionFrequency.toFloat())
                                }
                            } else {return@invokeOnCompletion}

                            // If statement placed here so that the data smoothing only starts as soon as there's enough data
                            if (data!!.size > DataSettings.data_smoothing) {
                                val frequencyMovingAverage = frequencyList.windowed(DataSettings.data_smoothing, 1) { it.average() }

                                // iterates through length of data list to add to graph array
                                for (i in frequencyMovingAverage.indices) {
                                    dataPoints.add(Entry(elapsedTimeList[i + (DataSettings.data_smoothing - 1)], frequencyMovingAverage[i].toFloat()))
                                }
                            } else {
                                for (i in frequencyList.indices) {
                                    dataPoints.add(Entry(elapsedTimeList[i], frequencyList[i]))
                                }
                            }

                            // Adds list of values to dataSet
                            dataSet = LineDataSet(dataPoints, "Inflection Frequency")
                            // Sets desired parameters
                            dataSet.setDrawFilled(false)
                            dataSet.setDrawValues(false)
                            dataSet.setDrawHighlightIndicators(false)
                            dataSet.setDrawCircleHole(false)
                            dataSet.setDrawCircles(false)
                            dataSet.lineWidth = 3f
                            dataSet.color = resources.getColor(R.color.graph_color)

                            // Sets data object as dataset and refreshes view
                            chart.data = LineData(dataSet)
                            chart.invalidate()
                        }
                    }
                } else {
                    // Async function used to grab graphing data for plotting
                    val updatedData = lifecycleScope.async { serverSession.getServerDataShorter() }

                    // Action taken when the async function is finished and an exception isn't thrown
                    updatedData.invokeOnCompletion {
                        if (it == null) {
                            val data_pair: Pair<List<Data>?, MutableList<Data>> = graphingData.getCompleted()

                            var data = data_pair.first

                            if(data_pair.first == null){
                                connection=0
                                data = data_pair.second
                            } // connection failed

                            if(data_pair.first!!.size < data_pair.second.size) {data = data_pair.second}

                            // Clearing data in order to reenter
                            dataPoints.clear()

                            val elapsedTimeList = mutableListOf<Float>()
                            val frequencyList = mutableListOf<Float>()

                            if (data != null) {
                                for(i in data!!.indices){
                                    elapsedTimeList.add(data!![i].elapsedTime.toFloat())
                                    frequencyList.add(data!![i].inflectionFrequency.toFloat())
                                }
                            } else {return@invokeOnCompletion}

                            // If statement placed here so that the data smoothing only starts as soon as there's enough data
                            if (data!!.size > DataSettings.data_smoothing) {
                                val frequencyMovingAverage = frequencyList.windowed(DataSettings.data_smoothing, 1) { it.average() }

                                // iterates through length of data list to add to graph array
                                for (i in frequencyMovingAverage.indices) {
                                    dataPoints.add(Entry(elapsedTimeList[i + (DataSettings.data_smoothing - 1)], frequencyMovingAverage[i].toFloat()))
                                }
                            } else {
                                for (i in frequencyList.indices) {
                                    dataPoints.add(Entry(elapsedTimeList[i], frequencyList[i]))
                                }
                            }

                            // Adds list of values to dataSet
                            dataSet = LineDataSet(dataPoints, "Inflection Frequency")
                            // Sets desired parameters
                            dataSet.setDrawFilled(false)
                            dataSet.setDrawValues(false)
                            dataSet.setDrawHighlightIndicators(false)
                            dataSet.setDrawCircleHole(false)
                            dataSet.setDrawCircles(false)
                            dataSet.lineWidth = 3f
                            dataSet.color = resources.getColor(R.color.graph_color)

                            // Sets data object as dataset and refreshes view
                            chart.data = LineData(dataSet)
                            chart.invalidate()
                        }
                    }
                }
                graphHandler.postDelayed(this, 10000) // Handler runs the same code again after 10 seconds
            }
        })

        settingsButton.setOnClickListener{
            // Intent initializes and executes with startActivity to start another Activity
            val toSettings = Intent(this, GraphSettings::class.java)
            startActivity(toSettings)
        }


        // Disables back on phone to go back to previous Activity
        onBackPressedDispatcher.addCallback(this, object:OnBackPressedCallback(true){ override fun handleOnBackPressed() {} })
    }

    override fun onResume() {
        super.onResume()
        chart.xAxis.axisMinimum = DataSettings.Xmin.toFloat()
        chart.xAxis.axisMaximum = DataSettings.Xmax.toFloat()
        chart.axisLeft.axisMinimum = DataSettings.Ymin.toFloat()
        chart.axisLeft.axisMaximum = DataSettings.Ymax.toFloat()
    }
}