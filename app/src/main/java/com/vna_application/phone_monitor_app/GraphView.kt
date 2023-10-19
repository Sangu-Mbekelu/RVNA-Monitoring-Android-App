package com.vna_application.phone_monitor_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kossh.impl.SSH
import kossh.impl.SSHOptions
import kossh.impl.SSHScp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.apache.commons.csv.CSVFormat
import java.io.ByteArrayInputStream
import java.io.InputStream

class GraphView : AppCompatActivity() {

    private lateinit var chart:LineChart
    private lateinit var serverSSH: SSH // SSH session variable
    private lateinit var serverSCP: SSHScp // SSH file transfer session variable
    private var serverInfo = ServerUser()

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
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

        // Handler runs the updateGraph Runnable interface
        val graphHandler = Handler(Looper.getMainLooper())
        println("Here?")
        // updateGraph will run every 10 seconds to update the graph displayed on phone
        // will check for connection, If disconnected, will attempt reconnection.
        // If attempt falls, updateGraph returns and will be run 5 secs later
        // If connected, a shorter version of gathering the data log without the session start
        // will be called
        // Code matches graphingData associated code at the top of this interface

        graphHandler.post(object : Runnable {
            override fun run() {
                // Async function used to grab graphing data for plotting
                val updatedData = GlobalScope.async {

                    try {
                        // SSH parameters used to start session
                        val serverLogin = SSHOptions(
                            host = serverInfo.host,
                            username = serverInfo.user,
                            password = serverInfo.password
                        )

                        // Starts SSH session
                        serverSSH = SSH(serverLogin)

                        // Starts file transfer session using SCP protocol
                        serverSCP = SSHScp(serverSSH)
                        // Session gets data log file from server, turns information into a Byte Array
                        val dataLog =
                            serverSCP.getBytes(serverInfo.remotepath + folderName + "/0_data_log.txt")

                        return@async readCsv(ByteArrayInputStream(dataLog))

                    } catch (e: Exception) {
                        return@async null // If connection or file transfer goes wrong, return
                    }
                }

                updatedData.invokeOnCompletion {
                    if (it == null) {
                        val data: List<Data>? = updatedData.getCompleted()

                        if(data == null){ println("RETURNING?"); return@invokeOnCompletion}

                        val elapsedTimeList = mutableListOf<Float>()
                        val frequencyList = mutableListOf<Float>()

                        for(i in data.indices){
                            elapsedTimeList.add(data[i].elapsedTime.toFloat())
                            frequencyList.add(data[i].inflectionFrequency.toFloat())
                        }

                        // Clearing data in order to reenter
                        dataPoints.clear()

                        // If statement placed here so that the data smoothing only starts as soon as there's enough data
                        if (data.size > DataSettings.data_smoothing) {
                            val frequencyMovingAverage = frequencyList.windowed(DataSettings.data_smoothing, 1) { it.average() }
                            // iterates through length of data list to add to graph array
                            for (i in frequencyMovingAverage.indices) {
                                dataPoints.add(Entry(elapsedTimeList[i + (DataSettings.data_smoothing - 1)], frequencyMovingAverage[i].toFloat()))
                            }
                        } else {
                            // iterates through length of data list to add to graph array
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

                graphHandler.postDelayed(this, 10000) // Handler runs the same code again after 5 seconds
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

    // Function taken from https://www.baeldung.com/kotlin/csv-files using Apache CSV Library
    // to read and parse CSV Byte Array
    fun readCsv(inputStream: InputStream): List<Data> =
        CSVFormat.Builder.create(CSVFormat.DEFAULT).apply {
            setIgnoreSurroundingSpaces(true)
        }.build().parse(inputStream.reader())
            .drop(1)// Drops the header
            .map {
                Data(
                    elapsedTime = it[3].toDouble() / 60,
                    inflectionFrequency = it[4].toDouble()/1E6
                )
            }


    override fun onResume() {
        super.onResume()
        chart.xAxis.axisMinimum = DataSettings.Xmin.toFloat()
        chart.xAxis.axisMaximum = DataSettings.Xmax.toFloat()
        chart.axisLeft.axisMinimum = DataSettings.Ymin.toFloat()
        chart.axisLeft.axisMaximum = DataSettings.Ymax.toFloat()
    }
}



// Class used to contain list of values from CSV
data class Data(val elapsedTime: Double, val inflectionFrequency: Double)