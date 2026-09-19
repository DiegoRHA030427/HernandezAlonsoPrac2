package ovh.gabrielhuav.flasklogin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TareasViewModel : ViewModel() {

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token

    private val _usuario = MutableStateFlow<String?>(null)
    val usuario: StateFlow<String?> = _usuario

    private val _tareas = MutableStateFlow<List<Tarea>>(emptyList())
    val tareas: StateFlow<List<Tarea>> = _tareas

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje

    fun limpiarMensaje() { _mensaje.value = null }

    private fun bearer(): String = "Bearer ${_token.value}"

    fun registrar(user: String, pass: String, onOk: () -> Unit) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val r = RetrofitClient.api.registrar(AuthRequest(user, pass))
                if (r.isSuccessful) {
                    _mensaje.value = "Usuario registrado, ya puedes iniciar sesion"
                    onOk()
                } else {
                    _mensaje.value = "Error al registrar (codigo ${r.code()})"
                }
            } catch (e: Exception) {
                _mensaje.value = "Error de conexion: ${e.message}"
            }
            _cargando.value = false
        }
    }

    fun login(user: String, pass: String, onOk: () -> Unit) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val r = RetrofitClient.api.login(AuthRequest(user, pass))
                if (r.isSuccessful && r.body()?.token != null) {
                    _token.value = r.body()!!.token
                    _usuario.value = r.body()!!.username
                    cargarTareas()
                    onOk()
                } else {
                    _mensaje.value = "Credenciales invalidas"
                }
            } catch (e: Exception) {
                _mensaje.value = "Error de conexion: ${e.message}"
            }
            _cargando.value = false
        }
    }

    fun cerrarSesion() {
        _token.value = null
        _usuario.value = null
        _tareas.value = emptyList()
    }

    fun cargarTareas() {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val r = RetrofitClient.api.listarTareas(bearer())
                if (r.isSuccessful) {
                    _tareas.value = r.body() ?: emptyList()
                } else {
                    _mensaje.value = "No se pudieron cargar las tareas (${r.code()})"
                }
            } catch (e: Exception) {
                _mensaje.value = "Error de conexion: ${e.message}"
            }
            _cargando.value = false
        }
    }

    fun crearTarea(titulo: String, descripcion: String, prioridad: String) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.crearTarea(
                    bearer(),
                    TareaRequest(titulo, descripcion, prioridad)
                )
                if (r.isSuccessful) {
                    _mensaje.value = "Tarea creada"
                    cargarTareas()
                } else {
                    _mensaje.value = "Error al crear (${r.code()})"
                }
            } catch (e: Exception) {
                _mensaje.value = "Error de conexion: ${e.message}"
            }
        }
    }

    fun actualizarTarea(t: Tarea, completada: Boolean) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.actualizarTarea(
                    bearer(),
                    t.id,
                    TareaRequest(t.titulo, t.descripcion, t.prioridad, completada)
                )
                if (r.isSuccessful) cargarTareas()
                else _mensaje.value = "Error al actualizar (${r.code()})"
            } catch (e: Exception) {
                _mensaje.value = "Error de conexion: ${e.message}"
            }
        }
    }

    fun borrarTarea(t: Tarea) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.borrarTarea(bearer(), t.id)
                if (r.isSuccessful) {
                    _mensaje.value = "Tarea eliminada"
                    cargarTareas()
                } else {
                    _mensaje.value = "Error al borrar (${r.code()})"
                }
            } catch (e: Exception) {
                _mensaje.value = "Error de conexion: ${e.message}"
            }
        }
    }
}

