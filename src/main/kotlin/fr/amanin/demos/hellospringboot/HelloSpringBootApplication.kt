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
import org.springframework.http.MediaType
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
class HelloSpringBootApplication

fun main(args: Array<String>) {
	runApplication<HelloSpringBootApplication>(*args)
}

@RestController
@Profile("server")
class HelloService(val props: AppProperties, @Value("\${countdown.delay}") val countdownDelay : Duration) {

	@GetMapping("hello")
	fun hello(@RequestHeader(HttpHeaders.REFERER, defaultValue = "anonymous") referer : String) : Mono<String> {
		return props.delayAction {
			Mono.just("Hello from ${props.name} to $referer !")
		}
			.timed()
			.doOnNext { println("delayed ${it.elapsed()} ms") }
			.map { it.get() }
	}

	@MessageMapping("countdown")
	fun countdownToHello(@Payload request: String) : Flux<String> {
		return Flux.interval(countdownDelay)
			.scan(request.toInt()) { count, time -> count - 1 }
			.takeWhile { it > 0 }
			.map { it.toString() }
			.concatWith(Mono.just("Hello !"))
	}
}

@Component
@Profile("client")
class HelloClient(@Value("\${server.port}") val port: Int, val props: AppProperties) : CommandLineRunner {
	val cli = WebClient.create()

	override fun run(vararg args: String?) {
		val msg = props.delayAction(::hello).timed().block()
			?: throw java.lang.RuntimeException("RESPONSE IS NULL !")
		println("Message received in ${msg.elapsed().toMillis()} ms: ${msg.get()}")
	}

	private fun hello() : Mono<String> {
		return cli.get().uri("http://localhost:$port/hello")
			.header(HttpHeaders.REFERER, props.name)
			.retrieve()
			.bodyToMono()
	}
}

@Component
@Profile("rsocket-client")
class CountdownClient(@Value("\${rsocket.server.port}") val port: Int) : CommandLineRunner {
	val cli = RSocketRequester.builder()
		.dataMimeType(MediaType.TEXT_PLAIN)
		.tcp("localhost", port)
		.route("countdown")

	override fun run(vararg  args: String?) {
		cli.data(5.toString())
			.retrieveFlux(String::class.java)
			.doOnNext { println(it) }
			.blockLast()
	}
}

@ConfigurationProperties("app")
@ConstructorBinding
data class AppProperties(val name: String, val delay: Duration?)

private fun <T> AppProperties.delayAction(action: () -> Mono<T>) : Mono<T> {
	return Mono.justOrEmpty(delay)
		.flatMap { Mono.delay(it) }
		.then(Mono.defer(action))
}
