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
 * Pagination is:
 *   [<< First] [< Previous] [...] [1] [2] [3] [4] [5] [...] [Next >] [Last >>] 
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

  def firstPageID = 1L
  def lastPageID = ((plID(totalPageCnt)-1) * pageCnt + 1).toLong
    
  def firstClass = if(currentPageID == 1) "disabled"; else ""
  def lastClass = if(currentPageID + pageCnt > totalPageCnt) "disabled"; else ""
  
  private def plID(i: Long): Int = (i/pageCnt + (if(i%pageCnt > 0) 1; else 0)).toInt
    
  val pages: List[PaginationDetail] = {
    // convert the pageID to page link nbr
    def plLabel(i: Long) = plID(i).toString 
    def plClass(i: Long) = if(i == currentPageID) "active"; else ""
    
    def loop(plID: Int, stopID: Int, l: List[PaginationDetail]): List[PaginationDetail] = {
      if(plID < 1 || plID < stopID) l
      else {
        val pID = ((plID-1)*pageCnt + 1).toLong
        loop(plID-1, stopID, PaginationDetail(pID, plClass(pID), plLabel(pID)) :: l)
      }
    }
      
    var l = List.empty[PaginationDetail]
    
    // Build the list of page links, from right to left
    // -------------------------------------
    // put a page spacer (label w/ elipsis) if:
    //  - if current > 2 and last - current > 3 
    //  - if current == 2 and last > 6 \ 
    //  - if current == 1 and last > 5 /  same as last - current > 4
    val last = plID(totalPageCnt)
    val current = plID(currentPageID)
    if(current < 4 && last > 5) {
      
      l = PaginationDetail(0, "", "...", false) :: l
      
      // paginate to the first 5 pages     
      l = loop(5, 1, l)
      
    } else if(current > 2 && last - current > 3)  {
      
      l = PaginationDetail(0, "", "...", false) :: l
      
      // paginate starting 2 pages to the right of current page
      l = loop(current + 2, current - 2, l)

    } else {
      
      // paginate from last page
      l = loop(last, last-4, l)
    }
    
    // put a page spacer (label w/ ellipsis) if have more than 2 pages to the left of current page
    if(current > 3) l = PaginationDetail(0, "", "...", false) :: l
    l
  }
}

