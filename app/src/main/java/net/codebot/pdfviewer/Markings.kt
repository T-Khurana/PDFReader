package net.codebot.pdfviewer

interface Markings {

    fun getMarkingsString() : String
    //To differentiate between eraser and other ops
    fun isDraw() : Boolean
}