package com.five9th.motionlogger.presentation.vm

import android.app.Application
import android.util.Log
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.five9th.motionlogger.R
import com.five9th.motionlogger.domain.entities.ActivityClass
import com.five9th.motionlogger.domain.entities.CollectingSession
import com.five9th.motionlogger.domain.entities.SampleWindow
import com.five9th.motionlogger.domain.entities.WindowPrediction
import com.five9th.motionlogger.domain.usecases.ml.AnalyzeSessionUseCase
import com.five9th.motionlogger.domain.usecases.GetSessionInfoUseCase
import com.five9th.motionlogger.domain.usecases.GetSessionUseCase
import com.five9th.motionlogger.domain.usecases.WindowSessionUseCase
import com.five9th.motionlogger.domain.usecases.ml.AnalyzeWindowUseCase
import com.five9th.motionlogger.presentation.ui.fragment.WindowInfoDialogFragment
import com.five9th.motionlogger.presentation.uimodel.SessionItem
import com.five9th.motionlogger.presentation.uimodel.UiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class AnalysisViewModel @Inject constructor (
    private val getSessionInfoUseCase: GetSessionInfoUseCase,
    private val getSessionUseCase: GetSessionUseCase,
    private val analyzeSessionUseCase: AnalyzeSessionUseCase,
    private val analyzeWindowUseCase: AnalyzeWindowUseCase,
    savedStateHandle: SavedStateHandle,
    application: Application
) : AndroidViewModel(application) {

    private val tag = "AnalysisViewModel"

    // ID must be put as extra to the activity's intent
    private val sessionId: Int = savedStateHandle[EXTRA_ID] ?: ID_UNDEFINED

    private var session: CollectingSession? = null  // <-- maybe we don't need to keep it
    private var sessionWindows = listOf<SampleWindow>()

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

    private val _analysisResultTextSF = MutableStateFlow("")
    val analysisResultTextSF = _analysisResultTextSF.asStateFlow()

    private val _predictionsSF = MutableSharedFlow<List<WindowPrediction>>()
    val predictionsSF = _predictionsSF.asSharedFlow()
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
            withContext(Dispatchers.IO) {
                session = getSessionUseCase(sessionId)
            }

            session?.let {
                processLoadedSession(it)
            }
        }
    }

    private suspend fun processLoadedSession(session: CollectingSession) {
        // display samples count
        _sampleCountSF.value = String.format(
            Locale.getDefault(), "%d", session.samples.size)

        // windowing
        sessionWindows = WindowSessionUseCase().invoke(session)

        // run analysis
        tryRunAnalysis(sessionWindows)
    }

    private suspend fun tryRunAnalysis(windows: List<SampleWindow>) {
        try {
            runAnalysis(windows)
        }
        catch (e: CancellationException) {
            throw e // always rethrow
        }
        catch (e: Exception) {
            val errText = "Error: ${e.message}"
            _messageSF.value = errText
        }
    }

    private suspend fun runAnalysis(windows: List<SampleWindow>) {

        val result = analyzeSessionUseCase(windows)

        if (result.windowResults.isEmpty()) {
            showError(ErrorType.SESSION_TOO_SHORT)
            return
        }

        _predictionsSF.emit(result.windowResults)

        val percentages = result.getPercentages()

        var text = ""  // <-- not so great but will do for now

        for ((act, percent) in percentages) {
            text += "${getActivityName(act)}: ${(percent * 100).roundToInt()}%\n"
        }

        // display result
        _analysisResultTextSF.value = text
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

    fun onWindowPredictionClick(prediction: WindowPrediction, manager: FragmentManager) {
        Log.d(tag, "Window #${prediction.windowIndex}: ${getActivityName(prediction.predictedClass)}")

        viewModelScope.launch {
            val score = getScore(prediction.windowIndex)

            val fragment = WindowInfoDialogFragment.newInstance(score, prediction.windowIndex)
            fragment.show(manager, "window")
        }
    }

    private suspend fun getScore(windowIdx: Int): List<Float> {
        val w = sessionWindows[windowIdx]
        val result = analyzeWindowUseCase(w)

        return result.scores
    }



    private enum class ErrorType {ID_UNDEFINED, SESSION_TOO_SHORT}

    companion object {
        const val EXTRA_ID = "extra_id"
        const val ID_UNDEFINED = -1
    }
}