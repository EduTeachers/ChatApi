package cz.educanet

import cz.educanet.plugins.GlorifiedString
import cz.educanet.plugins.configureRouting
import cz.educanet.plugins.configureSerialization
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "localhost") {
        install(CORS) {
            method(HttpMethod.Post)
            method(HttpMethod.Options)
            method(HttpMethod.Delete)
            allowNonSimpleContentTypes = true
            anyHost()
        }
        install(StatusPages) {
            exception<Throwable> {
                call.respond(
                    HttpStatusCode.BadRequest,
                    GlorifiedString("Invalid data")
                )
            }
        }
        configureRouting()
        configureSerialization()
    }.start(wait = true)
}
