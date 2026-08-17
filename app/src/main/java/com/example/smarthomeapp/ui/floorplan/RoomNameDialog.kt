package com.example.smarthomeapp.ui.floorplan


import androidx.compose.material3.*
import androidx.compose.runtime.*



@Composable
fun RoomNameDialog(

    onSave:(String)->Unit,

    onCancel:()->Unit

){


    var name by remember {

        mutableStateOf("")

    }



    AlertDialog(

        onDismissRequest = {

            onCancel()

        },


        title = {

            Text("Room Name")

        },


        text = {


            TextField(

                value = name,

                onValueChange = {

                    name = it

                },

                label = {

                    Text("Enter room name")

                }

            )

        },


        confirmButton = {


            /*
             * Disabled on a blank name rather than accepting one.
             *
             * The caller drops a room it cannot name, so a Save that was tappable with an empty
             * field threw away the rectangle the user had just dragged and closed the dialog with
             * no explanation — the room simply never appeared. Cancel is the way to abandon a
             * drag; Save should not be a second, silent one.
             */
            Button(

                enabled = name.isNotBlank(),

                onClick = {

                    onSave(name)

                }

            ){

                Text("Save")

            }


        },


        dismissButton = {


            Button(

                onClick = {

                    onCancel()

                }

            ){

                Text("Cancel")

            }


        }


    )


}