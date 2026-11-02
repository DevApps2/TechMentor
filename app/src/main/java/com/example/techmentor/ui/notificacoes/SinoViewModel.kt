package com.example.techmentor.ui.notificacoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.Sessao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/** Só conta as notificações não lidas, para a bolinha do sino. */
class SinoViewModel : ViewModel() {

    private val _naoLidas = MutableStateFlow(0)
    val naoLidas: StateFlow<Int> = _naoLidas.asStateFlow()

    fun atualizar() {
        val usuarioId = Sessao.usuarioId ?: return

        viewModelScope.launch {
            try {
                _naoLidas.value = ApiClient.api.listarNotificacoes(usuarioId, false).size
            } catch (e: HttpException) {
                // O sino é um extra: um erro aqui não deve atrapalhar a tela
            } catch (e: IOException) {
                // idem
            }
        }
    }
}