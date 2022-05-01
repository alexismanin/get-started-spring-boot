package fr.amanin.demos.hellospringboot

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@SpringBootApplication
class HelloSpringBootApplication

fun main(args: Array<String>) {
	runApplication<HelloSpringBootApplication>(*args)
}

@RestController
@Profile("server")
class HelloService {

	@GetMapping("hello")
	fun hello() = Mono.just("Hello !")
}

@Component
@Profile("client")
class HelloClient(@Value("\${server.port}") val port: Int) : CommandLineRunner {
	val cli = WebClient.create()

	override fun run(vararg args: String?) {
		val msg = cli.get().uri("http://localhost:$port/hello")
			.retrieve()
			.bodyToMono<String>()
			.block()

		println(msg)
	}
}