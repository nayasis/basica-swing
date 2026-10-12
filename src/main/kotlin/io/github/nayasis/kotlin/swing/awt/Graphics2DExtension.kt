@file:Suppress("unused")

package io.github.nayasis.kotlin.swing.awt

import java.awt.Graphics2D
import java.awt.Point

fun Graphics2D.drawLine(start: Point, end: Point) =
    drawLine(start.x,start.y,end.x,end.y)

fun Graphics2D.drawRect(start: Point, width: Int, height: Int) =
    drawRect(start.x,start.y,width,height)