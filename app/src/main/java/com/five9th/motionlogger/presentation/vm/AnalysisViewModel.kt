package com.five9th.motionlogger.presentation.vm

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.five9th.motionlogger.R
import com.five9th.motionlogger.domain.entities.ActivityClass
import com.five9th.motionlogger.domain.entities.CollectingSession
import com.five9th.motionlogger.domain.usecases.AnalyzeSessionUseCase
import com.five9th.motionlogger.domain.usecases.GetSessionInfoUseCase
import com.five9th.motionlogger.domain.usecases.GetSessionUseCase
import com.five9th.motionlogger.presentation.uimodel.SessionItem
import com.five9th.motionlogger.presentation.uimodel.UiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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

    private val _analysisResultSF = MutableStateFlow("")
    val analysisResultSF = _analysisResultSF.asStateFlow()
    // ----------


    init {
        Log.d(tag, "Session id: $sessionId")

        if (sessionId == ID_UNDEFINED) showError(ErrorType.ID_UNDEFINED)
        else {
            loadSessionInfo()
            loadSessionAndAnalyse()
        }
    }

    private fun showError(error: ErrorType) {
        val msg = when (error) {
            ErrorType.ID_UNDEFINED -> "Error: Session ID was not specified."
            ErrorType.SESSION_TOO_SHORT -> "Not enough samples."
        }
        _messageSF.value = msg
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
                tryRunAnalysis(it)
            }
        }
    }

    private suspend fun tryRunAnalysis(session: CollectingSession) {
        try {
            runAnalysis(session)
        }
        catch (e: CancellationException) {
            throw e // always rethrow
        }
        catch (e: Exception) {
            val errText = "Error: ${e.message}"
            _messageSF.value = errText
        }
    }

    private suspend fun runAnalysis(session: CollectingSession) {

        val result = analyzeSessionUseCase(session)

        if (result.windowResults.isEmpty()) {
            showError(ErrorType.SESSION_TOO_SHORT)
            return
        }

        val percentages = result.getPercentages()

        var text = ""  // <-- not so great but will do for now

        for ((act, percent) in percentages) {
            text += "${getActivityName(act)}: ${(percent * 100).roundToInt()}%\n"
        }

        // display result
        _analysisResultSF.value = text
    }

    private fun getActivityName(act: ActivityClass): String {
        val resId = when (act) {
            ActivityClass.DOWN_STAIRS -> R.string.activity_dws
            ActivityClass.UP_STAIRS -> R.string.activity_ups
            ActivityClass.WALKING -> R.string.activity_wlk
            ActivityClass.JOGGING -> R.string.activity_jog
            ActivityClass.STANDING -> R.string.activity_std
            ActivityClass.SITTING -> R.string.activity_sit
        }

        return application.getString(resId)
    }


    private enum class ErrorType {ID_UNDEFINED, SESSION_TOO_SHORT}

    companion object {
        const val EXTRA_ID = "extra_id"
        const val ID_UNDEFINED = -1
    }
}