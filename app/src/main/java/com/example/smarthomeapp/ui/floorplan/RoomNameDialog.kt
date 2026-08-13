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


            Button(

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