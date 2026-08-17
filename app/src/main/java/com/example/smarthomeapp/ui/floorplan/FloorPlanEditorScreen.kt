package com.example.smarthomeapp.ui.floorplan

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.smarthomeapp.data.model.Floor
import com.example.smarthomeapp.data.model.Room
import com.example.smarthomeapp.ui.screens.floorPlanResource
import java.util.UUID


/**
 * Draws the rooms of one floor.
 *
 * Takes the [floor] rather than just its id so the canvas can be seeded with what is already
 * saved: opening the editor on a floor that has rooms must show them, otherwise every save
 * silently replaces the plan instead of editing it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorPlanEditorScreen(
    floor: Floor?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSave: (Map<String, Room>) -> Unit
) {


    /*
     * Keyed by room id, and insertion-ordered — `Map + pair` yields a LinkedHashMap — so Undo
     * removes the room drawn most recently rather than an arbitrary one.
     */
    var rooms by remember {
        mutableStateOf<Map<String, Room>>(emptyMap())
    }


    /*
     * Seed once the floor first arrives, keyed on its id rather than on the room list —
     * re-seeding on every database emission would wipe a drawing in progress.
     */
    LaunchedEffect(floor?.id) {

        floor?.let {
            rooms = it.rooms
        }

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


        /*
         * The floor is resolved from the shared house state, so it is briefly absent on first
         * composition — and permanently absent if it was deleted from another device while this
         * screen was open. Drawing onto a floor that is not there would save into nothing.
         */
        if (floor == null) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),

                contentAlignment = Alignment.Center
            ) {

                Text(
                    if (isLoading) "Loading…" else "This floor no longer exists."
                )

            }

            return@Scaffold
        }



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

                        rooms.keys.lastOrNull()?.let { newest ->

                            rooms = rooms - newest

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

                        rooms = emptyMap()

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






            /*
             * Same aspect ratio as the grid on FloorScreen, from the floor's own
             * gridCols/gridRows. Room geometry is stored as fractions of whatever box it was
             * drawn in, so drawing on a differently shaped canvas would stretch every room on
             * the way to the floor screen — a square drawn here would arrive as a rectangle.
             */
            Card(

                modifier = Modifier

                    .fillMaxWidth()

                    .aspectRatio(
                        floor.gridCols.coerceAtLeast(1).toFloat() /
                                floor.gridRows.coerceAtLeast(1).toFloat()
                    ),



                colors = CardDefaults.cardColors(

                    containerColor =
                        MaterialTheme.colorScheme.surfaceContainerLow

                )

            ) {


                val planRes = floorPlanResource(floor.planImageAsset)


                Box(modifier = Modifier.fillMaxSize()) {


                    /*
                     * The bundled plan, when there is one, so rooms are traced onto the layout
                     * they belong to rather than drawn blind.
                     */
                    planRes?.let {

                        Image(
                            painter = painterResource(it),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )

                    }


                    RoomCanvas(

                        rooms = rooms.values,


                        backgroundColor =
                            if (planRes != null) Color.Transparent
                            else Color(0xFFF4F6F8),


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







            /*
             * Deliberately enabled even with no rooms: clearing a plan and saving that is a
             * legitimate edit, and gating Save on a non-empty canvas would make an existing
             * plan impossible to remove.
             */
            Button(
                modifier = Modifier.fillMaxWidth(),

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


                    /*
                     * A random key rather than a timestamp: two rooms drawn inside the same
                     * millisecond would otherwise collide, and the second would overwrite the
                     * first instead of being added.
                     */
                    rooms = rooms + (
                        UUID.randomUUID().toString() to Room(


                            name = roomName.trim(),


                            x = rectangle.x,


                            y = rectangle.y,


                            width = rectangle.width,


                            height = rectangle.height

                        )
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