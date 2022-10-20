package net.codebot.pdfviewer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


// PDF sample code from
// https://medium.com/@chahat.jain0/rendering-a-pdf-document-in-android-activity-fragment-using-pdfrenderer-442462cb8f9a
// Issues about cache etc. are not at all obvious from documentation, so we should expect people to need this.
// We may wish to provide this code.
class MainActivity : AppCompatActivity() {
    val LOGNAME = "pdf_viewer"
    val FILENAME = "shannon1948.pdf"
    val FILERESID = R.raw.shannon1948

    // manage the pages of the PDF, see below
    lateinit var pdfRenderer: PdfRenderer
    lateinit var parcelFileDescriptor: ParcelFileDescriptor
    var currentPage: PdfRenderer.Page? = null
    var pausedPageIndex : Int = -1

    var created : Boolean = false

    // custom ImageView class that captures strokes and draws them over the image
    lateinit var pageImage: PDFimage

    lateinit var pdf : LinearLayout

    lateinit var prevButton : Button
    lateinit var nextButton : Button
    lateinit var undoButton : Button
    lateinit var redoButton : Button
    lateinit var pencilButton : Button
    lateinit var highlighterButton : Button
    lateinit var eraserButton : Button
    lateinit var selectorButton : Button

    lateinit var pageNumberText : TextView
    lateinit var docNameText : TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(LOGNAME,"in Main onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prevButton = findViewById(R.id.previous_button)
        nextButton = findViewById(R.id.next_button)
        undoButton = findViewById(R.id.undo_button)
        redoButton = findViewById(R.id.redo_button)
        pencilButton = findViewById(R.id.draw_button)
        selectorButton = findViewById(R.id.select_button)
        eraserButton = findViewById(R.id.eraser_button)
        highlighterButton = findViewById(R.id.highlighter_button)
        docNameText = findViewById(R.id.doc_name)

        selectorButton.isSelected = true

        selectorButton.setOnClickListener {
            pencilButton.isSelected = false
            highlighterButton.isSelected = false
            eraserButton.isSelected = false
            selectorButton.isSelected = true

            pageImage.isMouse = true
            pageImage.isEraser = false
            pageImage.isHighlighter = false
            pageImage.isPencil = false

        }
        pencilButton.setOnClickListener {
            pencilButton.isSelected = true
            highlighterButton.isSelected = false
            eraserButton.isSelected = false
            selectorButton.isSelected = false

            pageImage.isMouse = false
            pageImage.isEraser = false
            pageImage.isHighlighter = false
            pageImage.isPencil = true
        }
        highlighterButton.setOnClickListener {
            pencilButton.isSelected = false
            highlighterButton.isSelected = true
            eraserButton.isSelected = false
            selectorButton.isSelected = false

            pageImage.isMouse = false
            pageImage.isEraser = false
            pageImage.isPencil = false
            pageImage.isHighlighter = true
        }
        eraserButton.setOnClickListener {
            pencilButton.isSelected = false
            highlighterButton.isSelected = false
            eraserButton.isSelected = true
            selectorButton.isSelected = false

            pageImage.isMouse = false
            pageImage.isHighlighter = false
            pageImage.isPencil = false
            pageImage.isEraser = true
        }
        undoButton.setOnClickListener {
            val undoOpPageNum = pageImage.undo()
            showPage(undoOpPageNum)
        }
        redoButton.setOnClickListener {
            val redoOpPageNum = pageImage.redo()
            showPage(redoOpPageNum)
        }

        // open page 0 of the PDF
        // it will be displayed as an image in the pageImage (above)
        try {
            openRenderer(this)
            //showPage(0) Removed to show pages according to the pdf page selected
            //closeRenderer()
        } catch (exception: IOException) {
            Log.d(LOGNAME, "Error opening PDF")
        }

        pdf = findViewById(R.id.pdf)
        pageImage = PDFimage(this, pdfRenderer.pageCount)
        pageImage.minimumWidth = 1000
        pageImage.minimumHeight = 2000
        pdf.addView(pageImage)

        showPage(0)
        val buttonsListener = View.OnClickListener { v: View ->
            var currPage = currentPage?.index
            if (currPage != null) {
                if (v == prevButton && currPage > 0) {
                    showPage(currPage - 1)
                }
                else if (v == nextButton && currPage < pdfRenderer.pageCount - 1) {
                    showPage(currPage + 1)

                }
            }
        }
        docNameText.text = FILENAME
        prevButton.setOnClickListener(buttonsListener)
        nextButton.setOnClickListener(buttonsListener)
        created = true

    }

    override fun onResume() {
    Log.d(LOGNAME,"in Main onResume")
        super.onResume()
        if (created) {
            created = false
            return
        }
        try {
            openRenderer(this)
        }
        catch(ex : IOException){
            Log.d(LOGNAME, "Unable to open PDF renderer")
        }
        showPage(pausedPageIndex)
    }


    override fun onStop() {
        Log.d(LOGNAME,"in Main onStop")
        super.onStop()
        try {
            closeRenderer()
        } catch (ex: IOException) {
            Log.d(LOGNAME, "Unable to close PDF renderer")
        }
    }


    @Throws(IOException::class)
    private fun openRenderer(context: Context) {
        Log.d(LOGNAME,"in Main openRenderer")
        // In this sample, we read a PDF from the assets directory.
        val file = File(context.cacheDir, FILENAME)
        if (!file.exists()) {
            // pdfRenderer cannot handle the resource directly,
            // so extract it into the local cache directory.
            val asset = this.resources.openRawResource(FILERESID)
            val output = FileOutputStream(file)
            val buffer = ByteArray(1024)
            var size: Int
            while (asset.read(buffer).also { size = it } != -1) {
                output.write(buffer, 0, size)
            }
            asset.close()
            output.close()
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

        // capture PDF data
        // all this just to get a handle to the actual PDF representation
        pdfRenderer = PdfRenderer(parcelFileDescriptor)
    }

    // do this before you quit!
    @Throws(IOException::class)
    private fun closeRenderer() {
        Log.d(LOGNAME,"in Main closeRenderer")
        if (currentPage != null){
            pausedPageIndex = currentPage!!.index
            currentPage?.close()
            currentPage = null
        }
        pdfRenderer.close()
        parcelFileDescriptor.close()
    }


    @SuppressLint("SetTextI18n")
    private fun showPage(index: Int) {
        Log.d(LOGNAME,"in Main SHowPage")
        if (pdfRenderer.pageCount <= index) {
            return
        }

        // Close the current page before opening another one.
        if (currentPage != null){
            currentPage?.close()
        }

        pageNumberText = findViewById(R.id.page_number)
        pageNumberText.text = "Page : ${index + 1}/${pdfRenderer.pageCount}"
        // Use `openPage` to open a specific page in PDF.
        currentPage = pdfRenderer.openPage(index)

        if (currentPage != null) {
            // Important: the destination bitmap must be ARGB (not RGB).
            val bitmap = Bitmap.createBitmap(currentPage!!.getWidth(), currentPage!!.getHeight(), Bitmap.Config.ARGB_8888)

            // Here, we render the page onto the Bitmap.
            // To render a portion of the page, use the second and third parameter. Pass nulls to get the default result.
            // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
            currentPage!!.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            if (currentPage!!.index > 0){
                prevButton.isEnabled = true
            }
            if(currentPage!!.index + 1 < pdfRenderer.pageCount){
                nextButton.isEnabled = true
            }

            // Display the page
            pageImage.setImage(bitmap, index)
        }
    }
}