package fr.amanin.demos.hellospringboot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@SpringBootApplication
class HelloSpringBootApplication

fun main(args: Array<String>) {
	runApplication<HelloSpringBootApplication>(*args)
}

@RestController
class HelloService {

	@GetMapping("hello")
	fun hello() = Mono.just("Hello !")
}