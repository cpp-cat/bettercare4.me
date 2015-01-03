/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.models

// This file contains utility objects and classes for the view pages.

object Paginator {
  val pageSize1 = 20
  val pageSize2 = 40
  val pageSize3 = 60
  val pageSize4 = 80
  val pageSize5 = 100
}

case class Paginator(pageID: Long, pageCnt: Int, totalPageCnt: Long) {
  
  def prevPageID = pageID - pageCnt
  def nextPageID = pageID + pageCnt
  def nbrRows = pageCnt * Paginator.pageSize1
  
  def hasPrev = prevPageID > 0
  def hasNext = nextPageID <= totalPageCnt
  
  def prevClass = if(hasPrev) ""; else "disabled"
  def nextClass = if(hasNext) ""; else "disabled"
  def pageClass(i: Long) = if(i == pageID) "active"; else ""
    
  // methods to limit the number of page links 5
  def nbrPageLinks: Int = {
    if(totalPageCnt < 6) totalPageCnt.toInt
    else if(totalPageCnt < 11) totalPageCnt.toInt/2
    else if(totalPageCnt < 16) totalPageCnt.toInt/3
    else if(totalPageCnt < 21) totalPageCnt.toInt/4
    else 5
  }
  def getPageID(i: Int): Long = {
    if(totalPageCnt < 6) i
    else if(totalPageCnt < 11) i*2
    else if(totalPageCnt < 16) i*3
    else if(totalPageCnt < 21) i*4
    else totalPageCnt.toInt*i/5
  }
}

