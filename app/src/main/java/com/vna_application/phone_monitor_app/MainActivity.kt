package com.vna_application.phone_monitor_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kossh.impl.SSH
import kossh.impl.SSHFtp
import kossh.impl.SSHOptions
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async


class MainActivity : AppCompatActivity() {

    private lateinit var serverSSH: SSH // SSH session variable
    private var serverInfo = ServerUser()

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Defining Reference of SSH Button
        val sshButton = findViewById<Button>(R.id.SshButton)

        // Defining Reference of EditText meant for folder name
        val editFolderName = findViewById<EditText>(R.id.EnterFolder)

        sshButton.setOnClickListener {

            val folderName = editFolderName.text // Getting text from EditText

            // Exits function if EditText is empty and alerts user nothing is entered
            if (folderName.isEmpty()) {Toast.makeText(this, "No Folder Name Entered", Toast.LENGTH_SHORT).show(); return@setOnClickListener}


            // Connecting Notice
            Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show()

            // Coroutine started to confirm access to server with folder name
            val sshResult = GlobalScope.async {
                try {
                    // SSH parameters used to start session
                    val serverLogin = SSHOptions(
                        host = serverInfo.host,
                        username = serverInfo.user,
                        password = serverInfo.password
                    )

                    // Starts SSH session
                    serverSSH = SSH(serverLogin)

                    // Starts file transfer session using FTP protocol
                    val serverFTP = SSHFtp(serverSSH)

                    try {
                        // Session changes directory to User specified directory on server
                        serverFTP.cd(serverInfo.remotepath + folderName.toString())
                        // Closes FTP file transfer session
                        serverFTP.close()

                    } catch (e: Exception) {
                        return@async 1 // Catches cd command Exception (Folder does not exist)
                    }

                    serverSSH.close()

                } catch (e: Exception) {
                    return@async 0 // Catches ssh Exceptions (Cannot Connect)
                }

                return@async 2 // Returns when no Exceptions
            }

            // Action taken when the async function is finished and an exception isn't thrown
            sshResult.invokeOnCompletion {
                if(it == null){
                    val errVal: Int = sshResult.getCompleted()

                    if(errVal == 2) {
                        // Intent initializes and executes with startActivity to start another Activity
                        val toGraphView = Intent(this, GraphView::class.java)
                        // Passing folderName to second activity
                        toGraphView.putExtra("folderName", folderName.toString())
                        startActivity(toGraphView)
                    }

                    if(errVal ==1) {
                        // User button press feedback if folder does not exist
                        Toast.makeText(this, "Folder Does Not Exist", Toast.LENGTH_SHORT).show()
                    }

                    if(errVal == 0){
                        // User button press feedback
                        Toast.makeText(this, "Cannot Connect to Server", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }

    }
}


// Class used to store graph variables
class DataSettings {
    companion object{
        var data_smoothing: Int = 15

        var Xmin: Int = 0

        var Xmax: Int = 10

        var Ymin: Int = 2200

        var Ymax: Int = 2700
    }
}