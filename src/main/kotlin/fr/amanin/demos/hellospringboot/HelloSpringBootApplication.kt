package fr.amanin.demos.hellospringboot

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
class HelloSpringBootApplication

fun main(args: Array<String>) {
	runApplication<HelloSpringBootApplication>(*args)
}

@RestController
@Profile("server")
class HelloService(val props: AppProperties) {

	@GetMapping("hello")
	fun hello(@RequestHeader(HttpHeaders.REFERER, defaultValue = "anonymous") referer : String) : Mono<String> {
		return Mono.just("Hello from ${props.name} to $referer !")
	}
}

@Component
@Profile("client")
class HelloClient(@Value("\${server.port}") val port: Int, val props: AppProperties) : CommandLineRunner {
	val cli = WebClient.create()

	override fun run(vararg args: String?) {
		val msg = cli.get().uri("http://localhost:$port/hello")
			.header(HttpHeaders.REFERER, props.name)
			.retrieve()
			.bodyToMono<String>()
			.block()

		println(msg)
	}
}

@ConfigurationProperties("app")
@ConstructorBinding
data class AppProperties(val name: String)