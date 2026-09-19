package ovh.gabrielhuav.flasklogin
// Peticiones
data class AuthRequest(val username: String, val password: String)

data class TareaRequest(
    val titulo: String,
    val descripcion: String? = null,
    val prioridad: String = "media",
    val completada: Boolean? = null
)
// Respuestas
data class MensajeResponse(val message: String?)

data class LoginResponse(
    val status: String?,
    val message: String?,
    val token: String?,
    val user_id: Int?,
    val username: String?
)
data class Tarea(
    val id: Int,
    val titulo: String,
    val descripcion: String?,
    val prioridad: String,
    val completada: Boolean,
    val user_id: Int
)