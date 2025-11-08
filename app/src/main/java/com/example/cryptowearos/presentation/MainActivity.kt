package com.example.cryptowearos.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

class PokemonViewModel : ViewModel() {

    var uiState by mutableStateOf<PokemonState>(PokemonState.Loading)
        private set

    init {
        fetchPokemonOfTheDay()
    }

    fun fetchPokemonOfTheDay() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pokemon = fetchPokemon()
                uiState = PokemonState.Success(pokemon)
            } catch (e: Exception) {
                uiState = PokemonState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }
    private fun fetchPokemon(): Pokemon {
        val today = LocalDate.now(ZoneId.of("America/Sao_Paulo"))
        val dayOfYear = today.dayOfYear

        val url = "https://pokeapi.co/api/v2/pokemon/$dayOfYear"

        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .build()

        val response = client.newCall(request).execute()

        val body = response.body?.string() ?: throw Exception("Resposta vazia")

        val json = JSONObject(body)
        val name = json.getString("name")
        val sprite =
            json.getJSONObject("sprites")
                .getString("front_default")

        return Pokemon(
            name = name.replaceFirstChar { it.uppercase() },
            imageUrl = sprite
        )
    }
}

data class Pokemon(
    val name: String,
    val imageUrl: String
)

sealed class PokemonState {
    object Loading : PokemonState()
    data class Success(val pokemon: Pokemon) : PokemonState()
    data class Error(val message: String) : PokemonState()
}

class MainActivity : ComponentActivity() {

    private val viewModel: PokemonViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PokemonScreen(viewModel)
        }
    }
}

@Composable
fun PokemonScreen(viewModel: PokemonViewModel) {
    val state = viewModel.uiState

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is PokemonState.Loading -> {
                CircularProgressIndicator()
            }

            is PokemonState.Error -> {
                Text(
                    text = "Erro: ${state.message}",
                    textAlign = TextAlign.Center
                )
            }

            is PokemonState.Success -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(state.pokemon.imageUrl),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Pokémon do dia:",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = state.pokemon.name,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
