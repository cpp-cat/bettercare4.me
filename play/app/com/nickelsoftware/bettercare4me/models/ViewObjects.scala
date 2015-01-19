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

case class PaginationDetail(pageID: Long, pageClass: String, pageLabel: String, isPage: Boolean=true) 

/**
 * View class to manage the pagination
 * 
 * @param currentPageID current view page
 * @param pageCnt nbr of pages to show per view (a pageCnt of 1 will display 20 rows)
 * @param totalPageCnt total number of pages available
 */
case class Paginator(currentPageID: Long, pageCnt: Int, totalPageCnt: Long) {
    
  def prevPageID = currentPageID - pageCnt
  def nextPageID = currentPageID + pageCnt
  def nbrRows = pageCnt * Paginator.pageSize1
  
  def hasPrev = prevPageID > 0
  def hasNext = nextPageID <= totalPageCnt
  
  def prevClass = if(hasPrev) ""; else "disabled"
  def nextClass = if(hasNext) ""; else "disabled"
   
  val pages: List[PaginationDetail] = {
    // convert the pageID to page link nbr
    def plID(i: Long): Int = (i/pageCnt + (if(i%pageCnt > 0) 1; else 0)).toInt
    def plLabel(i: Long) = plID(i).toString 
    def plClass(i: Long) = if(i == currentPageID) "active"; else ""
    
    def loop(plID: Int, l: List[PaginationDetail]): List[PaginationDetail] = {
      if(plID < 2) l
      else {
        val pID = ((plID-1)*pageCnt + 1).toLong
        loop(plID-1, PaginationDetail(pID, plClass(pID), plLabel(pID)) :: l)
      }
    }
      
    var l = List.empty[PaginationDetail]
    
    // Build the list of page links, from right to left
    // put the last page indicator
    l = PaginationDetail(totalPageCnt, plClass(totalPageCnt), plLabel(totalPageCnt)) :: l
    
    // put a page spacer (label w/ elipsis) if have more than 3 pages to the right of current page
    // nr: nbr of pages to the right of current page 
    val last = plID(totalPageCnt)
    val current = plID(currentPageID)
    if(last - current > 2)  {
      
      l = PaginationDetail(0, "", "...", false) :: l
      
      // paginate starting 2 pages to the right of current page
      l = loop(current + 2, l)

    } else {
      
      // paginate from last page minus one
      l = loop(last-1, l)
    }
    
    // put a page spacer (label w/ ellipsis) if have more than 3 pages to the left of current page
    if(current > 4) l = PaginationDetail(0, "", "...", false) :: l
    
    // put the link of the first page and 
    PaginationDetail(1, plClass(1), plLabel(1)) :: l
  }
}

