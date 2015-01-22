/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.models;

import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec

class ViewObjectsTestSpec extends PlaySpec {

  "The Paginator class" must {

    "be created with argument for middle page of 10 pages, page count of 1" in {

      val p = Paginator(5L, 1, 10L)
      
      p.prevPageID mustEqual 4L
      p.nextPageID mustEqual 6L
      p.hasPrev mustEqual true
      p.hasNext mustEqual true
      
      p.firstPageID mustEqual 1L
      p.lastPageID mustEqual 10L
      p.firstClass mustEqual ""
      p.lastClass mustEqual ""

      p.pages mustEqual List(
        PaginationDetail(0, "", "...", false),
        PaginationDetail(3L, "", "3"),
        PaginationDetail(4L, "", "4"),
        PaginationDetail(5L, "active", "5"),
        PaginationDetail(6L, "", "6"),
        PaginationDetail(7L, "", "7"),
        PaginationDetail(0, "", "...", false))
    }

    "be created with argument for page 5 of 10 pages, page count of 2" in {

      // the pages are 1, 3, 5, 7, 9
      val p = Paginator(5L, 2, 10L)
      p.prevPageID mustEqual 3L
      p.nextPageID mustEqual 7L
      p.hasPrev mustEqual true
      p.hasNext mustEqual true
      
      p.firstPageID mustEqual 1L
      p.lastPageID mustEqual 9L
      p.firstClass mustEqual ""
      p.lastClass mustEqual ""

      p.pages mustEqual List(
        PaginationDetail(1L, "", "1"),
        PaginationDetail(3L, "", "2"),
        PaginationDetail(5L, "active", "3"),
        PaginationDetail(7L, "", "4"),
        PaginationDetail(9L, "", "5"))
    }

    "be created with argument for page 7 of 10 pages, page count of 3" in {

      // the 10 pages are on: 1, 4, 7, 10
      val p = Paginator(7L, 3, 10L)
      p.prevPageID mustEqual 4L
      p.nextPageID mustEqual 10L
      p.hasPrev mustEqual true
      p.hasNext mustEqual true
      
      p.firstPageID mustEqual 1L
      p.lastPageID mustEqual 10L
      p.firstClass mustEqual ""
      p.lastClass mustEqual ""

      p.pages mustEqual List(
        PaginationDetail(1L, "", "1"),
        PaginationDetail(4L, "", "2"),
        PaginationDetail(7L, "active", "3"),
        PaginationDetail(10L, "", "4"))
    }

    "be created with argument for first page of 10 pages, count 1" in {

      val p = Paginator(1L, 1, 10L)
      p.prevPageID mustEqual 1
      p.nextPageID mustEqual 2L
      p.hasPrev mustEqual false
      p.hasNext mustEqual true
      
      p.firstPageID mustEqual 1L
      p.lastPageID mustEqual 10L
      p.firstClass mustEqual "disabled"
      p.lastClass mustEqual ""

      p.pages mustEqual List(
        PaginationDetail(1L, "active", "1"),
        PaginationDetail(2L, "", "2"),
        PaginationDetail(3L, "", "3"),
        PaginationDetail(4L, "", "4"),
        PaginationDetail(5L, "", "5"),
        PaginationDetail(0, "", "...", false))
    }

    "be created with argument for page 10 of 10 pages, count 1" in {

      val p = Paginator(10L, 1, 10L)
      p.prevPageID mustEqual 9L
      p.nextPageID mustEqual 10L
      p.hasPrev mustEqual true
      p.hasNext mustEqual false
      
      p.firstPageID mustEqual 1L
      p.lastPageID mustEqual 10L
      p.firstClass mustEqual ""
      p.lastClass mustEqual "disabled"

      p.pages mustEqual List(
        PaginationDetail(0L, "", "...", false),
        PaginationDetail(6L, "", "6"),
        PaginationDetail(7L, "", "7"),
        PaginationDetail(8L, "", "8"),
        PaginationDetail(9L, "", "9"),
        PaginationDetail(10L, "active", "10"))
    }

    "be created with argument for page single page, count 1" in {

      val p = Paginator(1L, 1, 1L)
      p.prevPageID mustEqual 1L
      p.nextPageID mustEqual 1L
      p.hasPrev mustEqual false
      p.hasNext mustEqual false
      
      p.firstPageID mustEqual 1L
      p.lastPageID mustEqual 1L
      p.firstClass mustEqual "disabled"
      p.lastClass mustEqual "disabled"

      p.pages mustEqual List(
        PaginationDetail(1L, "active", "1"))
    }

    "be created with argument for page 1 of 2 pages, count 2" in {

      val p = Paginator(1L, 2, 2L)
      p.prevPageID mustEqual 1L
      p.nextPageID mustEqual 1L
      p.hasPrev mustEqual false
      p.hasNext mustEqual false
      
      p.firstPageID mustEqual 1L
      p.lastPageID mustEqual 1L
      p.firstClass mustEqual "disabled"
      p.lastClass mustEqual "disabled"

      p.pages mustEqual List(
        PaginationDetail(1L, "active", "1"))
    }

    "be created with argument for page 97  of 100 pages, page count of 3" in {

      // the pages are: 29, 30, 31, 32, 33
      val p = Paginator(97L, 3, 100L)
      p.prevPageID mustEqual 94L
      p.nextPageID mustEqual 100L
      p.hasPrev mustEqual true
      p.hasNext mustEqual true
      
      p.firstPageID mustEqual 1L
      p.lastPageID mustEqual 100L
      p.firstClass mustEqual ""
      p.lastClass mustEqual ""

      p.pages mustEqual List(
        PaginationDetail(0L, "", "...", false),
        PaginationDetail(88L, "", "30"),
        PaginationDetail(91L, "", "31"),
        PaginationDetail(94L, "", "32"),
        PaginationDetail(97L, "active", "33"),
        PaginationDetail(100L, "", "34"))
    }

    "be created with argument for page 4  of 100 pages, page count of 3" in {

      // the pages are: 1, 4, 7, 10, 13
      val p = Paginator(4L, 3, 100L)
      p.prevPageID mustEqual 1L
      p.nextPageID mustEqual 7L
      p.hasPrev mustEqual true
      p.hasNext mustEqual true
      
      p.firstPageID mustEqual 1L
      p.lastPageID mustEqual 100L
      p.firstClass mustEqual ""
      p.lastClass mustEqual ""

      p.pages mustEqual List(
        PaginationDetail(1L, "", "1"),
        PaginationDetail(4L, "active", "2"),
        PaginationDetail(7L, "", "3"),
        PaginationDetail(10L, "", "4"),
        PaginationDetail(13L, "", "5"),
        PaginationDetail(0L, "", "...", false))
    }

    "be created with argument for page 4  of 7 pages, page count of 1" in {

      val p = Paginator(4L, 1, 7L)
      p.prevPageID mustEqual 3L
      p.nextPageID mustEqual 5L
      p.hasPrev mustEqual true
      p.hasNext mustEqual true
      
      p.firstPageID mustEqual 1L
      p.lastPageID mustEqual 7L
      p.firstClass mustEqual ""
      p.lastClass mustEqual ""

      p.pages mustEqual List(
        PaginationDetail(0L, "", "...", false),
        PaginationDetail(2L, "", "2"),
        PaginationDetail(3L, "", "3"),
        PaginationDetail(4L, "active", "4"),
        PaginationDetail(5L, "", "5"),
        PaginationDetail(6L, "", "6"),
        PaginationDetail(0L, "", "...", false))
    }

    "be created with argument for page 4  of 5 pages, page count of 1" in {

      val p = Paginator(4L, 1, 5L)
      p.prevPageID mustEqual 3L
      p.nextPageID mustEqual 5L
      p.hasPrev mustEqual true
      p.hasNext mustEqual true
      
      p.firstPageID mustEqual 1L
      p.lastPageID mustEqual 5L
      p.firstClass mustEqual ""
      p.lastClass mustEqual ""

      p.pages mustEqual List(
        PaginationDetail(1L, "", "1"),
        PaginationDetail(2L, "", "2"),
        PaginationDetail(3L, "", "3"),
        PaginationDetail(4L, "active", "4"),
        PaginationDetail(5L, "", "5"))
    }
  }
}