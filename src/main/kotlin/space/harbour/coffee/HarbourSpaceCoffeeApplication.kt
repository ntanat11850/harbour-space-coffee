package space.harbour.kotlin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HarbourSpaceCoffeeApplication

fun main(args: Array<String>) {
    runApplication<HarbourSpaceCoffeeApplication>(*args)
}
