package com.example.smarthomeapp.ui.floorplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smarthomeapp.data.model.Room


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorPlanEditorScreen(
    floorId: String,
    onBack: () -> Unit,
    onSave: (List<Room>) -> Unit
) {


    var rooms by remember {
        mutableStateOf<List<Room>>(emptyList())
    }


    var pendingRoom by remember {
        mutableStateOf<PendingRoom?>(null)
    }


    var showRoomNameDialog by remember {
        mutableStateOf(false)
    }



    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("Draw Floor Plan")
                },


                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(

                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,

                            contentDescription = "Back"

                        )

                    }

                }

            )

        }


    ) { innerPadding ->



        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)

        ) {



            Text(

                text = "Draw rooms manually",

                style = MaterialTheme.typography.titleMedium

            )



            Spacer(
                modifier = Modifier.height(4.dp)
            )



            Text(

                text = "Touch and drag on the grid to create a room.",

                color = MaterialTheme.colorScheme.onSurfaceVariant

            )



            Spacer(
                modifier = Modifier.height(12.dp)
            )




            Row(

                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement = Arrangement.spacedBy(8.dp)

            ) {



                OutlinedButton(

                    enabled = rooms.isNotEmpty(),

                    onClick = {

                        if (rooms.isNotEmpty()) {

                            rooms = rooms.dropLast(1)

                        }

                    }

                ) {


                    Icon(

                        imageVector = Icons.AutoMirrored.Filled.Undo,

                        contentDescription = "Undo"

                    )


                    Text(" Undo")

                }






                OutlinedButton(

                    enabled = rooms.isNotEmpty(),

                    onClick = {

                        rooms = emptyList()

                    }

                ) {



                    Icon(

                        imageVector = Icons.Default.Delete,

                        contentDescription = "Clear"

                    )



                    Text(" Clear")

                }

            }





            Spacer(
                modifier = Modifier.height(12.dp)
            )






            Card(

                modifier = Modifier

                    .fillMaxWidth()

                    .height(420.dp),



                colors = CardDefaults.cardColors(

                    containerColor =
                        MaterialTheme.colorScheme.surfaceContainerLow

                )

            ) {



                RoomCanvas(

                    rooms = rooms,


                    modifier = Modifier.fillMaxSize(),



                    onRoomDrawn = { x, y, width, height ->



                        pendingRoom = PendingRoom(

                            x = x,

                            y = y,

                            width = width,

                            height = height

                        )



                        showRoomNameDialog = true


                    }

                )

            }







            Spacer(
                modifier = Modifier.height(12.dp)
            )





            Text(

                text = "Rooms: ${rooms.size}"

            )





            Spacer(
                modifier = Modifier.height(8.dp)
            )







            Button(
                modifier = Modifier.fillMaxWidth(),

                enabled = rooms.isNotEmpty(),

                onClick = {

                    onSave(rooms)

                }
            ) {



                Text("Save Floor Plan")

            }


        }


    }







    if (

        showRoomNameDialog &&

        pendingRoom != null

    ) {



        RoomNameDialog(


            onSave = { roomName ->



                val rectangle =
                    pendingRoom ?: return@RoomNameDialog




                if(roomName.isNotBlank()) {



                    rooms = rooms + Room(


                        id = System.currentTimeMillis()
                            .toString(),


                        name = roomName.trim(),


                        x = rectangle.x,


                        y = rectangle.y,


                        width = rectangle.width,


                        height = rectangle.height

                    )


                }





                pendingRoom = null

                showRoomNameDialog = false



            },





            onCancel = {


                pendingRoom = null

                showRoomNameDialog = false


            }


        )


    }



}






private data class PendingRoom(

    val x: Float,

    val y: Float,

    val width: Float,

    val height: Float

)