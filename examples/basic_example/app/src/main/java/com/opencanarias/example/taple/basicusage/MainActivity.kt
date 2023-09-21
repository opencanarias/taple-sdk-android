package com.opencanarias.example.taple.basicusage

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opencanarias.example.taple.basicusage.ui.theme.TAPLETheme
import kotlinx.coroutines.Job
import com.opencanarias.taple.android.db.sqlite.SqLiteManager
import com.opencanarias.taple.ffi.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.lang.Exception

val OrangeOpen = Color(0xfff1a93c)

private lateinit var job: Job

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TAPLETheme {
                TapleApp(this)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

@Composable
fun TapleApp(context: Context) {
    ContentApp(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
        context
    )
}

@Composable
fun ContentApp(modifier: Modifier = Modifier, context: Context) {
    var subject: UserSubject? by remember { mutableStateOf(null) }
    var loading by remember { mutableStateOf(false) }
    var activeNode by remember { mutableStateOf(false) }
    var activeSubject by remember { mutableStateOf(false) }
    var node: TapleNode? by remember { mutableStateOf(null) }
    val alertDialogBuilder = AlertDialog.Builder(context)
    alertDialogBuilder.setTitle("Taple Alert")

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.aplet_01),
            contentDescription = null,
            modifier = Modifier
                .width(100.dp)
                .height(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Start
        Button(
            enabled = !activeNode,
            onClick = {
                    loading = true
                    // If the database already exists, then we will reset it
                    val databaseName = "myNodeDB"
                    context.deleteDatabase(databaseName)

                    // First, we create the settings
                    // As this is an example, we will use a fixed private key.
                    val sql = SqLiteManager(context, databaseName)
                    val bytesKey: Array<UByte> = arrayOf(
                        208u, 11u, 51u, 245u, 25u, 231u, 253u, 203u, 40u, 145u, 71u, 192u, 25u, 196u, 48u, 173u,
                        250u, 2u, 146u, 203u, 182u, 139u, 214u, 41u, 183u, 99u, 101u, 196u, 117u, 222u, 126u, 59u
                    )
                    val privateKey = bytesKey.asList()
                    val settings = TapleSettings(
                        listenAddr = listOf("/ip4/0.0.0.0/tcp/40040"),
                        keyDerivator = TapleKeyDerivator.ED25519,
                        privateKey = privateKey,
                        knownNodes = remoteKnowsNodesMultiAddr
                    )
                    // Node initialization
                    try {
                        node = start(sql, settings)
                    } catch (e: Exception) {
                        alertDialogBuilder.setMessage("Taple error: $e")
                        alertDialogBuilder.show()
                        return@Button
                    }

                    val api = node!!.getApi()

                    try {
                        // Add Subject and preauthorize it
                        val handler = Handler(Looper.getMainLooper())
                        api.addPreauthorizeSubject(governanceId, remoteNodes)

                        job = CoroutineScope(Dispatchers.Default).launch {
                            // First we have to wait for the new subject notification
                            while (true) {
                                if (!isActive) {
                                    return@launch
                                }
                                when (node!!.receiveBlocking()) {
                                    is TapleNotification.NewSubject -> {
                                        break
                                    }
                                    else -> continue
                                }
                            }
                            // Now we will wait for the first event of the governance, which is also the las one
                            // This event will allow us to create new subject and events
                            while (true) {
                                if (!isActive) {
                                    return@launch
                                }
                                when (val notification = node!!.receiveBlocking()) {
                                    is TapleNotification.NewEvent -> {
                                        if (notification.sn == 1UL) {
                                            break
                                        }
                                    }
                                    else -> continue
                                }
                            }
                            if (!isActive) {
                                return@launch
                            }
                            // The governance has been obtained, so the node has been properly initialized
                            activeNode = true
                            loading = false
                            handler.post{
                                alertDialogBuilder.setMessage("Taple Node is Up and running")
                                alertDialogBuilder.show()
                            }
                        }

                        GlobalScope.launch {
                            try {
                                withTimeout(1 * 60 * 1000) {
                                    job.join()
                                }
                            } catch (e: TimeoutCancellationException) {
                                job.cancel()
                                loading = false
                                handler.post{
                                    alertDialogBuilder.setMessage("Max waiting time elapsed")
                                    alertDialogBuilder.show()
                                }
                            }
                        }

                    } catch (e: Throwable) {
                        alertDialogBuilder.setMessage("Taple error: $e")
                        alertDialogBuilder.show()
                        return@Button
                    }
            }, modifier = Modifier
                .width(200.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = OrangeOpen)
        ) {
            Text(stringResource(R.string.button_start), fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Create Subject
        Button(
            enabled = activeNode,
            onClick = {
                // To create a subject we can use the utilities of the SDK, to be more precise, the SubjectBuilder utility
                val builder = node!!.getSubjectBuilder()
                // If we want, we can set the name and namespace of the subject
                builder.withName("MySubject")

                // After that, we can call the build method, which will generate a UserSubject. This methods needs
                // the schema to use and the governance too
                try {
                    subject = builder.build(governanceId, schemaId)
                } catch (e: Exception) {
                    alertDialogBuilder.setMessage("Taple error: $e")
                    alertDialogBuilder.show()
                    return@Button
                }
                // Although we have the subject, its data may not bet available yet. We should use the
                // notification channel to detect when the subject has been really created. In this case,
                // we need to wait for the newSubject notification
                val id: String
                while (true) {
                    when (node!!.receiveBlocking()) {
                        is TapleNotification.NewSubject -> {
                            // Now we can update our subject data
                            subject!!.refresh()
                            id = subject!!.getSubjectId()!!
                            // After this step, our subject variable has the same data that the inner subject managed by TAPLE.
                            break
                        }
                        else -> continue
                    }
                }
                activeSubject = true
                alertDialogBuilder.setMessage("Subject Created: $id")
                alertDialogBuilder.show()
            }, modifier = Modifier
                .width(200.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = OrangeOpen)
        ) {
            Text(stringResource(R.string.button_create_subject), fontSize = 18.sp)
        }


        Spacer(modifier = Modifier.height(16.dp))
        // Create Event
        Button(
            enabled = activeSubject,
            onClick = {
                // To create an event we can use the same subject variable created in the previous step
                // We will need again the notification channel to detect when the event has finalized, this is,
                // when the event has been approved by the network (validated).
                val api = node!!.getApi()
                // We need to pass the payload, which need to be a JsonString
                val payload = "{\"ModTwo\":{\"data\":1000}}"

                // The operation returns the ID of the Event Request. This can be used to check the state of
                // the request for example. However, we don't need it in this case.
                val requestId = subject!!.newFactEvent(payload)
                while (true) {
                    when (node!!.receiveBlocking()) {
                        is TapleNotification.NewEvent -> {
                            when (api.getRequest(requestId).state) {
                                TapleRequestState.ERROR -> {
                                    // We can check the SN of our subject
                                    alertDialogBuilder.setMessage("Event created with error")
                                    alertDialogBuilder.show()
                                    break
                                }
                                TapleRequestState.FINISHED -> {
                                    // Now we can update our subject data
                                    subject!!.refresh()
                                    // Now the content of our subject has been updated
                                    // We can check the SN of our subject
                                    val sn = subject!!.getSn()
                                    alertDialogBuilder.setMessage("Event created: $sn")
                                    alertDialogBuilder.show()
                                    break
                                }
                                TapleRequestState.PROCESSING -> {
                                    continue
                                }
                            }
                        }
                        else -> continue
                    }
                }
            }, modifier = Modifier
                .width(200.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = OrangeOpen)
        ) {
            Text(stringResource(R.string.button_create_event), fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (loading) {
            Text("Obtaining governance...", fontSize = 18.sp)
            Spacer(modifier = Modifier.height(10.dp))
            CircularProgressIndicator(
                modifier = Modifier.width(64.dp),
                color = OrangeOpen,
            )
        }
    }
}