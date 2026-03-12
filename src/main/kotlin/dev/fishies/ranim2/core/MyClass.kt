package dev.fishies.ranim2.core

class MyClass {
    val x = System.nanoTime()
}

fun aaa(b: MyClass) {
    val x = b::x
    val y = b::x
    val z = MyClass::x
    println(z(b))
    println(x == y)
}
