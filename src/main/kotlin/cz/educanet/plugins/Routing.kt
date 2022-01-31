package cz.educanet.plugins

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun deserialize(decoder: Decoder): LocalDateTime {
        TODO("Not yet implemented")
    }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        return encoder.encodeString(value.format(formatter))
    }
}

@Serializable
data class Message(
    val id: Int,
    val from: String,
    val text: String,
    @Serializable(LocalDateTimeSerializer::class)
    val date: LocalDateTime = LocalDateTime.now()
)

@Serializable
data class MessageValues(
    val from: String,
    val text: String
)

@Serializable
data class PaginationSettings(val start: UInt, val perPage: UInt)

var id = 1
val messages = sortedMapOf(compareBy { it }, id to Message(id, "Máca", "Ahoj"))

@Serializable
data class GlorifiedInt(val id: Int)

@Serializable
data class GlorifiedString(val message: String)

fun Application.configureRouting() {
    routing {
        route("/message") {
            post {
                val message: MessageValues = call.receive()
                if(message.text.isBlank() || message.from.isBlank())
                    return@post call.respond(HttpStatusCode.BadRequest, GlorifiedString("Data must not be empty"))
                synchronized(this@routing) {
                    messages[++id] = Message(id, message.from, message.text)
                }
                call.respond(GlorifiedString("message sent"))
            }

            get {
                val settings = call.request.queryParameters.let { qp ->
                    val start = qp["start"]?.toUIntOrNull()
                    val perPage = qp["perPage"]?.toUIntOrNull()
                    when {
                        start != null && perPage != null -> PaginationSettings(start, perPage)
                        else -> null
                    }
                }
                val paginated = synchronized(this@routing) {
                    settings?.let {
                        val start = ((settings.start - 1u) * settings.perPage).toInt()
                        messages.asSequence().drop(start).take(settings.perPage.toInt()).map { it.value }.toList()
                    } ?: messages.values.toList()
                }
                call.respond(paginated)
            }
            delete {
                val id = call.receive<GlorifiedInt>().id
                when (messages.remove(id) != null) {
                    true -> call.respond(GlorifiedString("Good job"))
                    else -> call.respond(HttpStatusCode.NotFound, GlorifiedString("Message does not exist"))
                }
            }
        }
    }
}
