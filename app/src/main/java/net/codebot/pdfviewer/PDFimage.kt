package net.codebot.pdfviewer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageView
import java.util.*
import kotlin.math.abs

@SuppressLint("AppCompatCustomView")
class PDFimage  // constructor
    (context: Context?, pages: Int) : ImageView(context) {
    val LOGNAME = "pdf_image"


    var pageNumber : Int = 0

    // drawing path
    var path: Path? = null

    var pencilPaths = ArrayList<ArrayList<Drawing>>()
    var highlighterPaths = ArrayList<ArrayList<Drawing>>()

    //Keeping two stacks for ease of use
    var undoStack : Stack<Markings> = Stack()
    var redoStack : Stack<Markings> = Stack()

    var isPencil : Boolean = false
    var isHighlighter : Boolean = false
    var isEraser : Boolean = false
    var isMouse : Boolean = false


    var mouseX = 0f
    var mouseY = 0f
    private var translateX : Float = 0f
    private var translateY : Float = 0f

    // image to display
    var bitmap: Bitmap? = null

    var highlighterColor = Paint()
    var pencilColor = Paint()


    var x1 = 0f
    var x2 = 0f
    var y1 = 0f
    var y2 = 0f
    var old_x1 = 0f
    var old_y1 = 0f
    var old_x2 = 0f
    var old_y2 = 0f
    var mid_x = -1f
    var mid_y = -1f
    var old_mid_x = -1f
    var old_mid_y = -1f
    var p1_id = 0
    var p1_index = 0
    var p2_id = 0
    var p2_index = 0

    var currentMatrix : Matrix = Matrix()
    var inverseM : Matrix = Matrix()



    init {
        Log.d(LOGNAME,"In PDF IMAGE Constructor")
        isMouse = true

        pencilColor.color = Color.BLUE
        pencilColor.strokeWidth = 5F
        pencilColor.style = Paint.Style.STROKE

        highlighterColor.color = Color.YELLOW
        highlighterColor.strokeWidth = 30F
        highlighterColor.style = Paint.Style.STROKE

        for(i in 0 until pages){
            pencilPaths.add(ArrayList())
            highlighterPaths.add(ArrayList())
        }
    }

    // capture touch events (down/move/up) to create a path
    // and use that to create a stroke that we can draw
    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d(LOGNAME,"In OnTouch PDF IMAGE")
        var inverted: FloatArray
        when (event.pointerCount) {
            1 -> when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (isMouse) {
                        mouseX = event.x
                        mouseY = event.y
                    }
                    path = Path()
                    path!!.moveTo(getOriginalX(event.x), getOriginalY(event.y))
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isMouse) {
                        translateX += event.x - mouseX
                        translateY += event.y - mouseY
                        mouseX = event.x
                        mouseY = event.y
                    }
                    path!!.lineTo(getOriginalX(event.x), getOriginalY(event.y))
                }
                MotionEvent.ACTION_UP -> {
                    if (isPencil) {
                        pencilPaths[pageNumber].add(Drawing(path))
                        addToUndoStack(Pencil(true, pageNumber, pencilPaths[pageNumber].size - 1))
                    } else if (isHighlighter) {
                        highlighterPaths[pageNumber].add(Drawing(path))
                        addToUndoStack(Pencil(false, pageNumber, highlighterPaths[pageNumber].size - 1))
                    } else if (isEraser) {
                        val e = Eraser(pageNumber)
                        erase(path!!, pencilPaths[pageNumber], e, true)
                        erase(path!!, highlighterPaths[pageNumber], e, false)
                        addToUndoStack(e)
                    }
                    path = null
                }
            }

            2 -> {
                p1_id = event.getPointerId(0)
                p1_index = event.findPointerIndex(p1_id)
                inverted = floatArrayOf(event.getX(p1_index), event.getY(p1_index))
                inverseM.mapPoints(inverted)
                if (old_x1 < 0 || old_y1 < 0) {
                    x1 = inverted[0]
                    old_x1 = x1
                    y1 = inverted[1]
                    old_y1 = y1
                } else {
                    old_x1 = x1
                    old_y1 = y1
                    x1 = inverted[0]
                    y1 = inverted[1]
                }

                p2_id = event.getPointerId(1)
                p2_index = event.findPointerIndex(p2_id)


                inverted = floatArrayOf(event.getX(p2_index), event.getY(p2_index))
                inverseM.mapPoints(inverted)

                if (old_x2 < 0 || old_y2 < 0) {
                    x2 = inverted[0]
                    old_x2 = x2
                    y2 = inverted[1]
                    old_y2 = y2
                } else {
                    old_x2 = x2
                    old_y2 = y2
                    x2 = inverted[0]
                    y2 = inverted[1]
                }


                mid_x = (x1 + x2) / 2
                mid_y = (y1 + y2) / 2
                old_mid_x = (old_x1 + old_x2) / 2
                old_mid_y = (old_y1 + old_y2) / 2

                // distance
                val d_old =
                    Math.sqrt(Math.pow((old_x1 - old_x2).toDouble(), 2.0) + Math.pow((old_y1 - old_y2).toDouble(), 2.0))
                        .toFloat()
                val d = Math.sqrt(Math.pow((x1 - x2).toDouble(), 2.0) + Math.pow((y1 - y2).toDouble(), 2.0)).toFloat()


                if (event.action == MotionEvent.ACTION_MOVE) {

                    val dx = mid_x - old_mid_x
                    val dy = mid_y - old_mid_y
                    currentMatrix.preTranslate(dx, dy)

                    var scale = d / d_old
                    scale = Math.max(0f, scale)
                    val pts = FloatArray(9)
                    currentMatrix.getValues(pts)

                    if (scale > 1 && pts[0] <= 3) {
                        currentMatrix.preScale(scale, scale, mid_x, mid_y)
                    } else if (scale < 1 && pts[0] > 0.5) {
                        currentMatrix.preScale(scale, scale, mid_x, mid_y)
                    }



                } else if (event.action == MotionEvent.ACTION_UP) {
                    old_x1 = -1f
                    old_y1 = -1f
                    old_x2 = -1f
                    old_y2 = -1f
                    old_mid_x = -1f
                    old_mid_y = -1f
                }
            }
        }

        return true
    }

    // set image as background
    fun setImage(bitmap: Bitmap?, pageNumber : Int) {
        translateX = 0f
        translateY = 0f
        currentMatrix = Matrix()
        this.pageNumber = pageNumber
        this.bitmap = bitmap
    }

    private fun getOriginalX(x : Float) : Float{
        val point = floatArrayOf(x, 0f)
        currentMatrix.invert(inverseM)
        inverseM.mapPoints(point)
        return point[0] - translateX
    }

    private fun getOriginalY(y : Float) : Float{
        val point = floatArrayOf(0f, y)
        currentMatrix.invert(inverseM)
        inverseM.mapPoints(point)
        return point[1] - translateY
    }

    private fun addToUndoStack (mark : Markings){
        undoStack.push(mark)
        redoStack = Stack<Markings>()

        if(undoStack.size >= 10){
            val newUndoStack = Stack<Markings>()
            val tempStack = Stack<Markings>()

            for(i in 1..5){
                tempStack.push(undoStack.peek())
                undoStack.pop()
            }
            while(!tempStack.empty()){
                newUndoStack.push(tempStack.peek())
                tempStack.pop()
            }
            undoStack = newUndoStack
        }

    }

    fun undo(): Int {
        if (undoStack.empty()) return pageNumber
        val marking: Markings = undoStack.peek()
        undoStack.pop()
        redoStack.push(marking)
        var operationPageNumber = 0
        if (marking.isDraw()) {
            val undoOperation: Pencil = marking as Pencil
            operationPageNumber = undoOperation.pageNumber
            if (undoOperation.type) {
                pencilPaths[undoOperation.pageNumber][undoOperation.index].valid = false
            } else {
                highlighterPaths[undoOperation.pageNumber][undoOperation.index].valid = false
            }
        } else {
            val undoOperation: Eraser = marking as Eraser
            val pencilIndex: ArrayList<Int> = undoOperation.penIndex
            var len = pencilIndex.size
            val undoPageNumber: Int = undoOperation.pageNumber
            operationPageNumber = undoPageNumber
            for (i in 0 until len) {
                pencilPaths[undoPageNumber][pencilIndex[i]].valid = true
            }
            val highlighterIndex: ArrayList<Int> = undoOperation.highlighterIndex
            len = highlighterIndex.size
            for (i in 0 until len) {
                highlighterPaths[undoPageNumber][highlighterIndex[i]].valid = true
            }
        }
        return operationPageNumber
    }

    fun redo(): Int {
        if (redoStack.empty()) return pageNumber
        val marking : Markings = redoStack.peek()
        redoStack.pop()
        undoStack.push(marking)
        var page = 0
        if (marking.isDraw()) {
            val redoOp: Pencil = marking as Pencil
            page = redoOp.pageNumber
            if (redoOp.type) {
                pencilPaths[redoOp.pageNumber][redoOp.index].valid = true
            } else {
                highlighterPaths[redoOp.pageNumber][redoOp.index].valid = true
            }
        } else {
            val undoOp: Eraser = marking as Eraser
            val pencilIndex: ArrayList<Int> = undoOp.penIndex
            var len = pencilIndex.size
            val undoPageNumber: Int = undoOp.pageNumber
            page = undoOp.pageNumber
            for (i in 0 until len) {
                pencilPaths[undoPageNumber][pencilIndex[i]].valid = false
            }
            val highlighterIndex: ArrayList<Int> = undoOp.highlighterIndex
            len = highlighterIndex.size
            for (i in 0 until len) {
                highlighterPaths[undoPageNumber][highlighterIndex[i]].valid = false
            }
        }
        return page
    }

    override fun onDraw(canvas: Canvas) {
        val maxXOffset = 1200
        val maxYOffset = 1200
        canvas.save()

        if (translateX > maxXOffset) translateX = maxXOffset.toFloat()
        else if (translateX < -maxXOffset) translateX = -maxXOffset.toFloat()

        if (translateY > maxYOffset) translateY = maxYOffset.toFloat()
        else if (translateY < -maxYOffset) translateY = -maxYOffset.toFloat()

        currentMatrix.postTranslate(translateX, translateY)
        canvas.setMatrix(currentMatrix)

        // draw background
        if (bitmap != null) {
            this.setImageBitmap(bitmap)
        }
        currentMatrix.postTranslate(-translateX, -translateY)

        for (path in highlighterPaths[pageNumber]) {
            if (path.valid) canvas.drawPath(path.path!!, highlighterColor)
        }

        if (isHighlighter && path != null) canvas.drawPath(path!!, highlighterColor)
        else if (isPencil && path != null) canvas.drawPath(path!!, pencilColor)

        for (path in pencilPaths[pageNumber]) {
            if (path.valid) canvas.drawPath(path.path!!, pencilColor)
        }
        super.onDraw(canvas)
        canvas.restore()
    }

    private fun getPoints(p: Path): ArrayList<ArrayList<Float>> {
        val pointArray = ArrayList<ArrayList<Float>>()
        val pm = PathMeasure(p, false)
        val length = pm.length
        var distance = 0f
        val speed = length / 20
        var counter = 0
        val coordinates = FloatArray(2)
        while ((distance < length) && (counter < 20)) {
            pm.getPosTan(distance, coordinates, null)
            pointArray.add(ArrayList())
            pointArray[counter].add(coordinates[0])
            pointArray[counter].add(coordinates[1])
            counter++
            distance += speed
        }
        return pointArray
    }

    private fun intersect(p1: Path, p2: Path): Boolean {
        val p1Path: ArrayList<ArrayList<Float>> = getPoints(p1)
        val p2Path: ArrayList<ArrayList<Float>> = getPoints(p2)
        val pathlen1 = p1Path.size
        val pathlen2 = p2Path.size
        for (i in 0 until pathlen1) {
            for (j in 0 until pathlen2) {
                if ((abs(p2Path[i][0] - p1Path[j][0]) <= 15) && (abs(p2Path[i][1] - p1Path[j][1]) <= 15)) {
                    return true
                }
            }
        }
        return false
    }

    private fun erase(erase_path: Path, paths: ArrayList<Drawing>, e: Eraser, isPencil: Boolean) {
        val len = paths.size
        for (i in 0 until len) {
            if (intersect(erase_path, paths[i].path!!)) {
                paths[i].valid = false
                if (isPencil) e.penIndex.add(i)
                else e.highlighterIndex.add(i)
            }
        }
    }

}