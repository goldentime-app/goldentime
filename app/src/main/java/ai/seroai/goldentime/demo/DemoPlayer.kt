package ai.seroai.goldentime.demo

import ai.seroai.goldentime.emergency.Screen
import ai.seroai.goldentime.ui.AppState
import ai.seroai.goldentime.ui.ChatLine
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 시연 단계 재생 — prototype runStep()과 동일한 상태 주입 */
class DemoPlayer(private val state: AppState, private val onToast: (String) -> Unit) {
    var scenario by mutableStateOf(DemoScenarios.A)
        private set
    var stepIndex by mutableStateOf(-1)
        private set

    fun select(s: DemoScenario) {
        scenario = s
        reset()
    }

    fun reset() {
        stepIndex = -1
        state.resetEmergency()
        state.go(Screen.H0)
    }

    /** 시나리오 [key]의 [step]단계까지 차례로 재생 (step < 0이면 홈) */
    fun jump(key: String, step: Int) {
        select(DemoScenarios.all.firstOrNull { it.key == key } ?: return)
        for (k in 0..step.coerceAtMost(scenario.steps.size - 1)) run(k)
    }

    fun next() {
        if (stepIndex < scenario.steps.size - 1) run(stepIndex + 1)
    }

    fun prev() {
        if (stepIndex > 0) {
            // 이전 단계까지의 대화를 다시 쌓는다
            val target = stepIndex - 1
            state.chat.clear()
            for (k in 0..target) addChat(scenario.steps[k])
            stepIndex = target
            apply(scenario.steps[target], rebuildChat = false)
        }
    }

    private fun run(i: Int) {
        stepIndex = i
        if (i == 0) state.resetEmergency()
        val step = scenario.steps[i]
        addChat(step)
        apply(step, rebuildChat = false)
    }

    private fun addChat(step: DemoStep) {
        step.userSays?.let { state.chat.add(ChatLine(it, fromApp = false)) }
        if (step.appSays != null && step.screen == Screen.E1) {
            state.chat.add(ChatLine(step.appSays, fromApp = true))
        }
    }

    private fun apply(step: DemoStep, rebuildChat: Boolean) {
        state.forcedRiskPercent = step.riskPercent
        if (step.crash) state.crash = true
        step.toast?.let(onToast)
        state.go(step.screen, scripted = true, immediateSend = step.alreadySent)
        if (step.crash) state.crash = true
        step.appSays?.let { state.speaker.speak(it) } // TTS (ko-KR)
    }
}
