package com.example.smarthomeapp.ui.floorplan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.smarthomeapp.data.model.Room
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


/** Smallest drag, in pixels, that counts as a room rather than a stray touch. */
private const val MIN_ROOM_PX = 40f

/**
 * Draw-by-dragging surface for the floor plan editor.
 *
 * Touch positions arrive in canvas pixels but are reported to [onRoomDrawn] as **fractions of the
 * canvas, 0..1**. The editor canvas, the dashboard thumbnail and the floor screen are all
 * different sizes, so a room stored in pixels would land somewhere different on each of them —
 * and on a different phone, somewhere different again.
 */
@Composable
fun RoomCanvas(
    rooms: List<Room>,
    onRoomDrawn: (
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) -> Unit,
    modifier: Modifier = Modifier,
) {

    // Where user first touches
    var dragStart by remember {
        mutableStateOf<Offset?>(null)
    }

    // Current finger/mouse position
    var dragCurrent by remember {
        mutableStateOf<Offset?>(null)
    }


    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F8))
            .pointerInput(Unit) {

                detectDragGestures(

                    // User starts touching
                    onDragStart = { startOffset ->

                        dragStart = startOffset
                        dragCurrent = startOffset
                    },

                    // User moves finger
                    onDrag = { change, _ ->

                        change.consume()

                        dragCurrent = change.position
                    },

                    // User releases finger
                    onDragEnd = {

                        val start = dragStart
                        val end = dragCurrent

                        if (start != null && end != null) {

                            val left = min(
                                start.x,
                                end.x
                            )

                            val top = min(
                                start.y,
                                end.y
                            )

                            val width = abs(
                                end.x - start.x
                            )

                            val height = abs(
                                end.y - start.y
                            )


                            val canvasWidth = size.width.toFloat()
                            val canvasHeight = size.height.toFloat()


                            /*
                             * Prevent accidental tiny rooms.
                             */
                            if (
                                width >= MIN_ROOM_PX &&
                                height >= MIN_ROOM_PX &&
                                canvasWidth > 0f &&
                                canvasHeight > 0f
                            ) {

                                /*
                                 * Convert to fractions of the canvas, and clamp: a drag that
                                 * ran off the edge would otherwise store a room reaching
                                 * outside the plan.
                                 */
                                val fx = (left / canvasWidth).coerceIn(0f, 1f)
                                val fy = (top / canvasHeight).coerceIn(0f, 1f)

                                onRoomDrawn(
                                    fx,
                                    fy,
                                    (width / canvasWidth).coerceIn(0f, 1f - fx),
                                    (height / canvasHeight).coerceIn(0f, 1f - fy)
                                )
                            }
                        }


                        dragStart = null
                        dragCurrent = null
                    },

                    // Gesture cancelled
                    onDragCancel = {

                        dragStart = null
                        dragCurrent = null
                    }
                )
            }
    ) {


        /*
         * -------------------------
         * DRAW BACKGROUND GRID
         * -------------------------
         */

        val gridSize = 50f

        var x = 0f

        while (x <= size.width) {

            drawLine(
                color = Color(0xFFD3D7DC),
                start = Offset(
                    x,
                    0f
                ),
                end = Offset(
                    x,
                    size.height
                ),
                strokeWidth = 1f
            )

            x += gridSize
        }


        var y = 0f

        while (y <= size.height) {

            drawLine(
                color = Color(0xFFD3D7DC),
                start = Offset(
                    0f,
                    y
                ),
                end = Offset(
                    size.width,
                    y
                ),
                strokeWidth = 1f
            )

            y += gridSize
        }


        /*
         * -------------------------
         * DRAW SAVED ROOMS
         * -------------------------
         */

        rooms.forEach { room ->


            // Stored as fractions of the plan, so scale back up to this canvas.
            val topLeft = Offset(
                room.x * size.width,
                room.y * size.height
            )

            val roomSize = Size(
                room.width * size.width,
                room.height * size.height
            )


            // Light inside
            drawRect(
                color = Color(0xFFE3F2FD),

                topLeft = topLeft,

                size = roomSize
            )


            // Dark room border
            drawRect(
                color = Color(0xFF455A64),

                topLeft = topLeft,

                size = roomSize,

                style = Stroke(
                    width = 5f
                )
            )
        }


        /*
         * -------------------------
         * DRAW ROOM CURRENTLY
         * BEING CREATED
         * -------------------------
         */

        val start = dragStart
        val current = dragCurrent


        if (
            start != null &&
            current != null
        ) {

            val left = min(
                start.x,
                current.x
            )

            val top = min(
                start.y,
                current.y
            )

            val width = abs(
                current.x - start.x
            )

            val height = abs(
                current.y - start.y
            )


            // Preview fill
            drawRect(
                color = Color(
                    0x5533A1FD
                ),

                topLeft = Offset(
                    left,
                    top
                ),

                size = Size(
                    max(width, 1f),
                    max(height, 1f)
                )
            )


            // Preview border
            drawRect(
                color = Color(
                    0xFF1976D2
                ),

                topLeft = Offset(
                    left,
                    top
                ),

                size = Size(
                    max(width, 1f),
                    max(height, 1f)
                ),

                style = Stroke(
                    width = 5f
                )
            )
        }
    }
}