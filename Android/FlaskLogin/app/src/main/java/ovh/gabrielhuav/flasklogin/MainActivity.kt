package ovh.gabrielhuav.flasklogin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ovh.gabrielhuav.flasklogin.ui.theme.FlaskLoginTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlaskLoginTheme {
                AppTareas()
            }
        }
    }
}

@Composable
fun AppTareas() {
    val nav = rememberNavController()
    val vm: TareasViewModel = viewModel()

    NavHost(navController = nav, startDestination = "login") {
        composable("login") { PantallaLogin(vm, nav) }
        composable("registro") { PantallaRegistro(vm, nav) }
        composable("tareas") { PantallaTareas(vm, nav) }
    }
}

@Composable
fun PantallaLogin(vm: TareasViewModel, nav: androidx.navigation.NavController) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    val cargando by vm.cargando.collectAsState()
    val mensaje by vm.mensaje.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Mis Tareas", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("Inicia sesion para continuar", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            label = { Text("Usuario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Contrasenia") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { vm.login(user, pass) { nav.navigate("tareas") } },
            enabled = !cargando && user.isNotBlank() && pass.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Iniciar sesion")
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { nav.navigate("registro") }) {
            Text("No tengo cuenta, registrarme")
        }

        mensaje?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun PantallaRegistro(vm: TareasViewModel, nav: androidx.navigation.NavController) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    val cargando by vm.cargando.collectAsState()
    val mensaje by vm.mensaje.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Crear cuenta", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            label = { Text("Usuario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Contrasenia") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { vm.registrar(user, pass) { nav.popBackStack() } },
            enabled = !cargando && user.isNotBlank() && pass.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Registrarme")
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { nav.popBackStack() }) {
            Text("Ya tengo cuenta")
        }

        mensaje?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaTareas(vm: TareasViewModel, nav: androidx.navigation.NavController) {
    val tareas by vm.tareas.collectAsState()
    val usuario by vm.usuario.collectAsState()
    val cargando by vm.cargando.collectAsState()
    val mensaje by vm.mensaje.collectAsState()

    var mostrarDialogo by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbar.showSnackbar(it)
            vm.limpiarMensaje()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Tareas de ${usuario ?: ""}") },
                actions = {
                    IconButton(onClick = { vm.cargarTareas() }) {
                        Icon(Icons.Filled.Refresh, "Recargar")
                    }
                    IconButton(onClick = {
                        vm.cerrarSesion()
                        nav.navigate("login") { popUpTo("login") { inclusive = true } }
                    }) {
                        Icon(Icons.Filled.ExitToApp, "Cerrar sesion")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogo = true }) {
                Icon(Icons.Filled.Add, "Nueva tarea")
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (cargando) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (tareas.isEmpty() && !cargando) {
                Text(
                    "No tienes tareas. Toca + para crear una.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(tareas) { t -> TarjetaTarea(t, vm) }
                }
            }
        }
    }

    if (mostrarDialogo) {
        DialogoNuevaTarea(
            onCerrar = { mostrarDialogo = false },
            onGuardar = { titulo, desc, prio ->
                vm.crearTarea(titulo, desc, prio)
                mostrarDialogo = false
            }
        )
    }
}

@Composable
fun TarjetaTarea(t: Tarea, vm: TareasViewModel) {
    val colorPrioridad = when (t.prioridad.lowercase()) {
        "alta" -> Color(0xFFD32F2F)
        "media" -> Color(0xFFF9A825)
        else -> Color(0xFF388E3C)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = t.completada,
                onCheckedChange = { vm.actualizarTarea(t, it) }
            )
            Column(Modifier.weight(1f)) {
                Text(
                    t.titulo,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (t.completada) TextDecoration.LineThrough else null
                )
                if (!t.descripcion.isNullOrBlank()) {
                    Text(t.descripcion, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(4.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(t.prioridad) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = colorPrioridad)
                )
            }
            IconButton(onClick = { vm.borrarTarea(t) }) {
                Icon(Icons.Filled.Delete, "Borrar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun DialogoNuevaTarea(onCerrar: () -> Unit, onGuardar: (String, String, String) -> Unit) {
    var titulo by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var prioridad by remember { mutableStateOf("media") }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Nueva tarea") },
        text = {
            Column {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Titulo") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descripcion") }
                )
                Spacer(Modifier.height(12.dp))
                Text("Prioridad:")
                Row {
                    listOf("alta", "media", "baja").forEach { p ->
                        FilterChip(
                            selected = prioridad == p,
                            onClick = { prioridad = p },
                            label = { Text(p) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGuardar(titulo, desc, prioridad) },
                enabled = titulo.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        }
    )
}