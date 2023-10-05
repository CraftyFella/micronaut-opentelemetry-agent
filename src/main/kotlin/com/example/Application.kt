package com.example

import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import com.mongodb.reactivestreams.client.MongoCollection
import com.mongodb.reactivestreams.client.MongoDatabase
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Value
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Filter
import io.micronaut.http.annotation.Get
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.runtime.Micronaut.run
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.inject.Singleton
import org.bson.Document
import org.reactivestreams.Publisher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow

fun main(args: Array<String>) {
	run(*args)
}

@Controller("/hello")
class HelloController(private val annotatedThing: AnnotatedThing, private val manuallyWiredUpThing: ManuallyWiredUpThing, private val mongoDbThing: MonoDbThing) {

	@Get("/")
	suspend fun index(): String {
		annotatedThing.invoke()
		manuallyWiredUpThing.invoke()
		val count = mongoDbThing.invoke().count()
		return "Hello World $count"
	}
}

@Singleton
class AnnotatedThing {

	private val log: Logger = LoggerFactory.getLogger(AnnotatedThing::class.java)

	@WithSpan
	fun invoke() {
		Span.current().setAttribute("testing", true)
		log.info("Doing a thing")
	}
}

@Singleton
class MonoDbThing(private val mongoCollection: MongoCollection<Document>) {

	private val log: Logger = LoggerFactory.getLogger(MonoDbThing::class.java)

	@WithSpan
	suspend fun invoke(): List<Document> {
		return mongoCollection.find().asFlow().toList()
	}
}

@Singleton
class ManuallyWiredUpThing(private val tracer: Tracer) {

	private val log: Logger = LoggerFactory.getLogger(AnnotatedThing::class.java)

	fun invoke() {
		tracer.spanBuilder("manualSpan").startSpan().setAttribute("testing", true).end()
		log.info("Doing a thing")
	}
}

@Filter("/**")
@Introspected
class TraceIdFilter : HttpServerFilter {
	@Value("\${otel.custom.trace-header-name:x-traceid}")
	var traceIdHeaderName: String? = null
	override fun doFilter(
		request: HttpRequest<*>?, chain: ServerFilterChain
	): Publisher<MutableHttpResponse<*>> {
		val proceed = chain.proceed(request)
		return Publishers.map(
			proceed
		) { response: MutableHttpResponse<*> ->
			response.header(
				traceIdHeaderName,
				Span.current().spanContext.traceId
			)
			response
		}
	}
}

@Factory
class TracerFactory {
	@Bean
	fun tracer(openTelemetry: OpenTelemetry): Tracer {
		return openTelemetry.getTracer("app")
	}

	@Bean
	fun openTelemetry(): OpenTelemetry {
		return GlobalOpenTelemetry.get()
	}

}

@Factory
class MongoFactory {
	@Bean
	fun mongoClient(): MongoClient = MongoClients.create("mongodb://localhost:27017")

	@Bean
	fun mongoDatabase(mongoClient: MongoClient): MongoDatabase =
		mongoClient.getDatabase("test")

	@Bean
	fun mongoCollection(mongoDatabase: com.mongodb.reactivestreams.client.MongoDatabase): MongoCollection<Document> =
		mongoDatabase.getCollection("test")
}