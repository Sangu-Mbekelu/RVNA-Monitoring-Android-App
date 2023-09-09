package com.vna_application.phone_monitor_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async


class MainActivity : AppCompatActivity() {

    @OptIn(ExperimentalCoroutinesApi::class)
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

            // Initializing ServerActivity object that will work network operations asynchronously
            val serverSession = ServerActivity(folderName.toString())

            // Connecting Notice
            Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show()

            // Coroutine started to confirm access to server with folder name
            val sshResult = lifecycleScope.async {serverSession.confirmFolderExistence()}

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