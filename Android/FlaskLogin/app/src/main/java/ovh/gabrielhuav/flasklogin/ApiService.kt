package ovh.gabrielhuav.flasklogin

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("register")
    suspend fun registrar(@Body body: AuthRequest): Response<MensajeResponse>

    @POST("login")
    suspend fun login(@Body body: AuthRequest): Response<LoginResponse>

    @GET("tareas")
    suspend fun listarTareas(@Header("Authorization") token: String): Response<List<Tarea>>

    @POST("tareas")
    suspend fun crearTarea(
        @Header("Authorization") token: String,
        @Body body: TareaRequest
    ): Response<Tarea>

    @PUT("tareas/{id}")
    suspend fun actualizarTarea(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body body: TareaRequest
    ): Response<Tarea>

    @DELETE("tareas/{id}")
    suspend fun borrarTarea(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<MensajeResponse>
}