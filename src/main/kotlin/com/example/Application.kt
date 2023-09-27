package com.example

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
import io.opentelemetry.api.trace.Span
import org.reactivestreams.Publisher

fun main(args: Array<String>) {
	run(*args)
}

@Controller("/hello")
class HelloController {

	@Get("/")
	fun index(): String {
		return "Hello World"
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