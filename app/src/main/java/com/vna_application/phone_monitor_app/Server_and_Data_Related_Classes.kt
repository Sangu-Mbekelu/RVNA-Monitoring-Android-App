package com.vna_application.phone_monitor_app

import androidx.appcompat.app.AppCompatActivity
import kossh.impl.SSH
import kossh.impl.SSHFtp
import kossh.impl.SSHOptions
import kossh.impl.SSHScp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.InputStream
import org.apache.commons.csv.CSVFormat
import java.io.ByteArrayInputStream

// Class used by the app to access the server.
// Function used to cd to directory on server to confirm existence
// Function used to connect to server, then retrieve and return file data
// Function used to just retrieve and return file data
class ServerActivity(private val dirname: String) : AppCompatActivity(), CoroutineScope by MainScope() {

    companion object{
        var previous_data = mutableListOf<Data>()
    }
    // These two variables are initialized in order to allow for reconnection
    private lateinit var serverSession: SSH // SSH session variable
    private lateinit var serverSCP: SSHScp // SSH file transfer session variable
    private var serverInfo = ServerUser()

    // FUNCTION USED TO CONFIRM FOLDER EXISTS ON SERVER
    suspend fun confirmFolderExistence(): Int {
        val errorValue = withContext(Dispatchers.IO) {
            try {
                // SSH parameters used to start session
                val serverLogin = SSHOptions(
                    host = serverInfo.host,
                    username = serverInfo.user,
                    password = serverInfo.password
                )

                // Starts SSH session
                serverSession = SSH(serverLogin)

                // Starts file transfer session using FTP protocol
                val serverFTP = SSHFtp(serverSession)

                try {
                    // Session changes directory to User specified directory on server
                    serverFTP.cd(serverInfo.remotepath + dirname)
                    // Closes FTP file transfer session
                    serverFTP.close()

                } catch (e: Exception) {
                    return@withContext 1 // Catches cd command Exception (Folder does not exist)
                }

                serverSession.close()

            } catch (e: Exception) {
                return@withContext 0 // Catches ssh Exceptions (Cannot Connect)
            }

            return@withContext 2 // Returns when no Exceptions
        }
        return errorValue   // Error value being used to determine how to handle user feedback/app progress
    }                       // If errorValue = 2, ssh connected and folder exists
                            // If errorValue = 1, ssh connected, but folder does not exist
                            // If errorValue = 0, ssh did not connect


    // FUNCTION USED TO CONNECT TO GET DATA LOG FROM SERVER
    suspend fun getServerData(): Pair<List<Data>?, MutableList<Data>> {
        val graphingData = coroutineScope {

            val graphingData = withContext(Dispatchers.IO) {

                val dataLog: ByteArray?  // Initializing the dataLog string that will include file data

                try {
                    // SSH parameters used to start session
                    val serverLogin = SSHOptions(
                        host = serverInfo.host,
                        username = serverInfo.user,
                        password = serverInfo.password
                    )

                    // Starts SSH session
                    serverSession = SSH(serverLogin)

                    // Starts file transfer session using SCP protocol
                    serverSCP = SSHScp(serverSession)

                    // Session gets data log file from server, turns information into a Byte Array
                    dataLog = serverSCP.getBytes(serverInfo.remotepath + dirname + "/0_data_log.txt")

                } catch (e: Exception) {
                    return@withContext Pair(null, previous_data) // If connection or file transfer goes wrong, return
                }
                val data = readCsv(ByteArrayInputStream(dataLog))
                previous_data = data.toMutableList()
                // Generated List will be returned as graphingData
                return@withContext Pair(data, previous_data)
            }

            return@coroutineScope graphingData
        }
        return graphingData
    }

    // FUNCTION USED TO CONNECT TO GET DATA LOG FROM SERVER
    suspend fun getServerDataShorter(): Pair<List<Data>?, MutableList<Data>> {
        val graphingData = coroutineScope {

            val graphingData = withContext(Dispatchers.IO) {

                val dataLog: ByteArray?  // Initializing the dataLog string that will include file data

                try {
                    // Session gets data log file from server, turns information into a Byte Array
                    dataLog = serverSCP.getBytes(serverInfo.remotepath + dirname + "/0_data_log.txt")

                } catch (e: Exception) {
                    return@withContext Pair(null, previous_data) // If connection or file transfer goes wrong, return
                }
                val data = readCsv(ByteArrayInputStream(dataLog))
                previous_data = data.toMutableList()
                // Generated List will be returned as graphingData
                return@withContext Pair(data, previous_data)
            }

            return@coroutineScope graphingData
        }
        return graphingData
    }

    // Function taken from https://www.baeldung.com/kotlin/csv-files using Apache CSV Library
    // to read and parse CSV Byte Array
    private fun readCsv(inputStream: InputStream):
        List<Data> =
            CSVFormat.Builder.create(CSVFormat.DEFAULT).apply {
                setIgnoreSurroundingSpaces(true)
            }.build().parse(inputStream.reader())
                .drop(1)// Drops the header
                .map {
                    Data(
                        elapsedTime = it[3].toDouble() / 60, // data in 3rd column
                        inflectionFrequency = it[4].toDouble()/1E6 // data in 4th column
                    )
                }
}

// Class used to contain list of values from CSV
data class Data(val elapsedTime: Double, val inflectionFrequency: Double)
