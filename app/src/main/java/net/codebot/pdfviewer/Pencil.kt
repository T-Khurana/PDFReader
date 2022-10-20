package net.codebot.pdfviewer
//type is true if it is a pencil, false for highlighter
class Pencil(var type: Boolean, var pageNumber: Int, var index: Int) : Markings {

    override fun isDraw(): Boolean {return true}

    override fun getMarkingsString(): String {
        return "$type Page: $pageNumber index: $index"
    }

}