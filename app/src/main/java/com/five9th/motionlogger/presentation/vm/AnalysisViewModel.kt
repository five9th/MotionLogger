package com.five9th.motionlogger.presentation.vm

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.five9th.motionlogger.domain.entities.CollectingSession
import com.five9th.motionlogger.domain.usecases.AnalyzeSessionUseCase
import com.five9th.motionlogger.domain.usecases.GetSessionInfoUseCase
import com.five9th.motionlogger.domain.usecases.GetSessionUseCase
import com.five9th.motionlogger.presentation.uimodel.SessionItem
import com.five9th.motionlogger.presentation.uimodel.UiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class AnalysisViewModel @Inject constructor (
    private val getSessionInfoUseCase: GetSessionInfoUseCase,
    private val getSessionUseCase: GetSessionUseCase,
    private val analyzeSessionUseCase: AnalyzeSessionUseCase,
    savedStateHandle: SavedStateHandle,
    application: Application
) : AndroidViewModel(application) {

    private val tag = "AnalysisViewModel"

    // id must be put as extra to the activity's intent
    private val sessionId: Int = savedStateHandle[EXTRA_ID] ?: ID_UNDEFINED

    private var session: CollectingSession? = null

    private val mapper = UiMapper(application)


    // ---- UI State ----
    private val _sessionInfoSF = MutableStateFlow(
        SessionItem("", "", "", "", "")
    )
    val sessionInfoSF = _sessionInfoSF.asStateFlow()

    private val _sampleCountSF = MutableStateFlow("")
    val sampleCountSF = _sampleCountSF.asStateFlow()

    private val _messageSF = MutableStateFlow("")
    val messageSF = _messageSF.asStateFlow()
    // ----------


    init {
        Log.d(tag, "Session id: $sessionId")

        if (sessionId == ID_UNDEFINED) showError()
        else {
            loadSessionInfo()
            loadSessionAndAnalyse()
        }
    }

    private fun showError() {
        _messageSF.value = "Error: Session ID was not specified."
    }

    private fun loadSessionInfo() {
        val sessionInfo = getSessionInfoUseCase(sessionId) ?: return
        val uiModel = mapper.mapDomainToUiModel(sessionInfo)

        _sessionInfoSF.value = uiModel
    }

    private fun loadSessionAndAnalyse() {
        viewModelScope.launch {
            session = getSessionUseCase(sessionId)

            session?.let {
                // display samples count
                _sampleCountSF.value = String.format(
                    Locale.getDefault(), "%d", it.samples.size)

                // run analysis
                runAnalysis(it)
            }
        }
    }

    // this whole thing is temporary for testing (todo)
    private suspend fun runAnalysis(session: CollectingSession) {

        val activityLabels = arrayOf("dws", "ups", "wlk", "jog", "std", "sit") // <-- this needs to be centralised

        val result = analyzeSessionUseCase(session)
        val percentages = result.getPercentages()

        var text = ""

        for ((act, percent) in percentages) {
            text += "${activityLabels[act.value]}: ${(percent * 100).roundToInt()}%\n"
        }

        // display result
        _messageSF.value = text
    }


    companion object {
        const val EXTRA_ID = "extra_id"
        const val ID_UNDEFINED = -1
    }
}