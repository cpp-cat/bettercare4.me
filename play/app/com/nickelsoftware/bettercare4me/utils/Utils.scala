package com.nickelsoftware.bettercare4me.utils

object Utils {

  def add2Map[C](s: String, c: C, map: Map[String, List[C]]): Map[String, List[C]] = {

    val l = map.getOrElse(s, List())
    map + (s -> (c :: l))
  }

  def add2Map[C](s: String, l: List[C], map: Map[String, List[C]]): Map[String, List[C]] = {
    
    if (l.isEmpty) map
    else {

      val l2 = map.getOrElse(s, List.empty)
      if (l2.isEmpty) map + (s -> l)
      else map + (s -> List.concat(l, l2))
    }
  }
}