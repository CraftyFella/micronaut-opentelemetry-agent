package com.example

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.runtime.Micronaut.run

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