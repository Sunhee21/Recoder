package com.example.camera1demo

/**
 * @intro
 * @author sunhee
 * @date 2020/3/30
 */
fun main() {
    System.out.println("${ADDR}")
    testVar = false
    System.out.println("${ADDR}")

}


val ADDR
get() = run { if (testVar) "init"  else "change"}

var testVar = true