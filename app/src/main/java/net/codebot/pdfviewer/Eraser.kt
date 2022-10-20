package net.codebot.pdfviewer

class Eraser(var pageNumber: Int) : Markings {
    var penIndex = ArrayList<Int>()
    var highlighterIndex = ArrayList<Int>()

    override fun getMarkingsString() : String{
        var str = "$pageNumber\nPen: "
        for(i in penIndex.iterator()){
            str += "$penIndex "
        }
        str += "\nHighlighter: "
        for(i in highlighterIndex.iterator()){
            str += "$highlighterIndex "
        }
        str += "\n"
        return str
    }

    override fun isDraw() : Boolean{ return false }

}