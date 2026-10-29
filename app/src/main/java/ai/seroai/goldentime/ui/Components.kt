package ai.seroai.goldentime.ui

import ai.seroai.goldentime.ui.theme.GtType
import ai.seroai.goldentime.ui.theme.Palette
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.em
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 상단 바 — 가짜 시계 제거(시스템 바가 실시간 표시), 주행 상태 배지만. trailing에 보조 내비 배치 가능 */
@Composable
fun TopBar(
    mode: String,
    chipColor: Color,
    driving: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(Palette.Ground.copy(alpha = 0.72f))
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("골든타임", color = Palette.Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Box(
            Modifier.background(chipColor, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                mode,
                color = if (chipColor == Palette.Alarm) Color.White else Palette.Ground,
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            )
        }
        Box(Modifier.weight(1f))
        trailing?.invoke()
        Text(
            if (driving) "주행 중" else "정차",
            color = Palette.Ink2, fontSize = 16.sp,
        )
    }
}

@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    emergency: Boolean = false,
    borderColor: Color? = null,
    background: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // 선으로 가두지 않고 면의 밝기로 구분 (HIG deference) — 강조 카드는 강조색을 옅게 깐다
    val base = background ?: if (emergency) Palette.AlarmPanel else Palette.Panel
    val fill = borderColor?.let { it.copy(alpha = 0.16f).compositeOver(base) } ?: base
    Column(
        modifier.background(fill, RoundedCornerShape(CARD_RADIUS)).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content,
    )
}

/** 카드 모서리 — 큰 라운드로 물체처럼 */
val CARD_RADIUS = 28.dp

/** 카드 라벨 (아이브로) */
@Composable
fun CardTitle(text: String, color: Color = Palette.Ink2) {
    Text(text, color = color, fontSize = GtType.Caption, fontWeight = FontWeight.SemiBold)
}

/**
 * 누르면 살짝 눌려 들어가는 물리감 (scale 0.96, 스프링 복귀). 리플 대신 쓰는 터치 피드백.
 */
fun Modifier.pressable(onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "press",
    )
    this.graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

/** 3/4 원호 게이지 — 숫자가 주인공 (지름의 40%) */
@Composable
fun Gauge(percent: Int, color: Color, label: String, modifier: Modifier = Modifier.size(200.dp)) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = size.minDimension * 0.07f, cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(
                Color.White.copy(alpha = 0.1f), startAngle = 135f, sweepAngle = 270f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = stroke,
            )
            drawArc(
                color, startAngle = 135f, sweepAngle = 270f * (percent.coerceIn(0, 100) / 100f),
                useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = stroke,
            )
        }
        Text("$percent", color = Palette.Ink, fontSize = GtType.Display, fontWeight = FontWeight.Bold, letterSpacing = (-0.03).em)
        Text(
            label, color = Palette.Ink2, fontSize = 16.sp, lineHeight = 22.sp, textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
        )
    }
}

/** 주 행동 버튼 — 차량 터치 타깃 고정 높이 64dp */
@Composable
fun BigButton(
    text: String,
    color: Color = Palette.Alarm,
    textColor: Color = Color.White,
    fontSize: Int = 22,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(64.dp)
            .pressable(onClick)
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, fontSize = fontSize.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }
}

@Composable
fun GhostButton(text: String, fontSize: Int = 17, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(56.dp)
            .pressable(onClick)
            .background(Palette.Panel2, RoundedCornerShape(50))
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Palette.Ink, fontSize = fontSize.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }
}

/** 체크 행 — "✓ " 문자열 대신 민트 원형 체크 아이콘 + 텍스트 */
@Composable
fun CheckRow(text: String, fontSize: Int = 20, color: Color = Palette.Ink) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(26.dp).background(Palette.Calm, CircleShape), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(13.dp)) {
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.05f, size.height * 0.55f)
                    lineTo(size.width * 0.38f, size.height * 0.88f)
                    lineTo(size.width * 0.95f, size.height * 0.15f)
                }
                drawPath(p, Palette.Ground, style = Stroke(width = size.width * 0.22f, cap = StrokeCap.Round))
            }
        }
        Text(text, color = color, fontSize = fontSize.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 듣는 중 마이크 — 이모지 금지. 벡터 마이크 + 110% 펄스 동심원 2겹 */
@Composable
fun ListeningMic(modifier: Modifier = Modifier.size(220.dp)) {
    val t = rememberInfiniteTransition(label = "mic")
    val pulse by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            // 동심원 펄스 2겹
            val r1 = size.minDimension * (0.32f + 0.16f * pulse)
            val r2 = size.minDimension * (0.32f + 0.16f * ((pulse + 0.5f) % 1f))
            drawCircle(Palette.Alarm.copy(alpha = 0.25f * (1f - pulse)), radius = r1, center = c)
            drawCircle(Palette.Alarm.copy(alpha = 0.25f * (1f - (pulse + 0.5f) % 1f)), radius = r2, center = c)
            // 본체 원
            val body = size.minDimension * 0.32f
            drawCircle(Palette.Alarm, radius = body, center = c)
            // 마이크 벡터: 캡슐 + 받침 아크 + 스탠드
            val mw = body * 0.42f
            val mh = body * 0.72f
            val top = c.y - body * 0.52f
            drawRoundRect(
                Color.White,
                topLeft = Offset(c.x - mw / 2, top),
                size = Size(mw, mh),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(mw / 2),
            )
            val arcR = mw * 1.05f
            drawArc(
                Color.White, startAngle = 20f, sweepAngle = 140f, useCenter = false,
                topLeft = Offset(c.x - arcR, top + mh - arcR * 0.9f),
                size = Size(arcR * 2, arcR * 2),
                style = Stroke(width = body * 0.09f, cap = StrokeCap.Round),
            )
            drawLine(
                Color.White,
                Offset(c.x, top + mh + arcR * 0.12f + arcR),
                Offset(c.x, top + mh + arcR * 0.42f + arcR * 0.4f),
                strokeWidth = body * 0.09f, cap = StrokeCap.Round,
            )
        }
    }
}

/** 압박 박자 점 4개 — 분당 110회(545ms) 순차 점멸. TTS 박자와 동기 */
@Composable
fun CprBeatDots(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "beat")
    val phase by t.animateFloat(
        0f, 4f,
        infiniteRepeatable(tween(545 * 4, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(4) { i ->
            val on = phase.toInt() % 4 == i
            Box(
                Modifier.size(24.dp).background(
                    if (on) Palette.Alarm else Palette.Alarm.copy(alpha = 0.25f),
                    CircleShape,
                ),
            )
        }
    }
}

/**
 * 랑데부 타임라인 바 — 동일 스케일로 세 옵션의 만남/병원 시점 비교.
 * 만남 시점까지 진하게, 병원 도착까지 흐리게.
 */
@Composable
fun RendezvousTimeline(contact: Int, hospital: Int, maxMinutes: Int, accent: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(10.dp)) {
        val w = size.width
        drawRoundRect(Color.White.copy(alpha = 0.1f), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
        val hospW = w * (hospital.toFloat() / maxMinutes).coerceIn(0f, 1f)
        drawRoundRect(
            accent.copy(alpha = 0.35f),
            size = Size(hospW, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
        )
        val contactW = w * (contact.toFloat() / maxMinutes).coerceIn(0f, 1f)
        drawRoundRect(
            accent,
            size = Size(contactW, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
        )
        // 의료진 만남 시점 마커
        // 손잡이 마커는 슬라이더로 오인돼 제거 (만남 지점은 진한 구간 끝으로 읽힘)
    }
}

/**
 * 접선 지도 — 내 차와 구급차가 접선 지점(졸음쉼터 핀)으로 다가오는 애니메이션.
 * 실지도 대신 도로선 + 마커 연출 (시연용).
 */
@Composable
fun MapPlaceholder(meetPoint: Boolean, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "map")
    val progress by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "progress",
    )
    val measurer = rememberTextMeasurer()
    Canvas(modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))) {
        val w = size.width
        val h = size.height
        fun p(x: Float, y: Float) = Offset(x * w, y * h)
        val u = minOf(w, h) / 100f // 지도 단위

        // 바탕: 야간 지도 — 지형 블록, 하천, 일반 도로
        drawRect(Color(0xFF0F151C))
        val block = Color(0xFF141C25)
        listOf(
            Triple(0.04f, 0.06f, 0.22f), Triple(0.30f, 0.04f, 0.16f), Triple(0.62f, 0.52f, 0.20f),
            Triple(0.08f, 0.40f, 0.14f), Triple(0.40f, 0.74f, 0.18f), Triple(0.78f, 0.70f, 0.16f),
        ).forEach { (x, y, s) ->
            drawRoundRect(block, topLeft = p(x, y), size = Size(s * w, s * h * 1.3f), cornerRadius = CornerRadius(3 * u))
        }
        val river = Path().apply {
            moveTo(0f, 0.30f * h)
            cubicTo(0.25f * w, 0.20f * h, 0.45f * w, 0.42f * h, 0.62f * w, 0.30f * h)
            cubicTo(0.78f * w, 0.18f * h, 0.90f * w, 0.26f * h, w, 0.20f * h)
        }
        drawPath(river, Color(0xFF13283A), style = Stroke(width = 4.5f * u, cap = StrokeCap.Round))
        val minor = Color(0xFF1E2833)
        listOf(
            listOf(0.0f to 0.55f, 0.35f to 0.50f, 1.0f to 0.58f),
            listOf(0.20f to 0.0f, 0.24f to 0.45f, 0.18f to 1.0f),
            listOf(0.55f to 0.0f, 0.60f to 0.40f, 0.70f to 1.0f),
            listOf(0.82f to 0.0f, 0.86f to 0.55f, 0.95f to 1.0f),
            listOf(0.0f to 0.88f, 0.50f to 0.92f, 1.0f to 0.82f),
        ).forEach { line ->
            val path = Path().apply {
                moveTo(line[0].first * w, line[0].second * h)
                quadraticTo(line[1].first * w, line[1].second * h, line[2].first * w, line[2].second * h)
            }
            drawPath(path, minor, style = Stroke(width = 1.2f * u, cap = StrokeCap.Round))
        }

        // 고속도로 (외곽선 + 본선) 와 경로
        val route = Path().apply {
            moveTo(0.05f * w, 0.92f * h)
            cubicTo(0.22f * w, 0.80f * h, 0.30f * w, 0.66f * h, 0.46f * w, 0.62f * h)
            cubicTo(0.62f * w, 0.58f * h, 0.70f * w, 0.36f * h, 0.80f * w, 0.24f * h)
            cubicTo(0.86f * w, 0.17f * h, 0.92f * w, 0.12f * h, 0.97f * w, 0.08f * h)
        }
        drawPath(route, Color(0xFF2A3644), style = Stroke(width = 6.5f * u, cap = StrokeCap.Round))
        drawPath(route, Palette.Route.copy(alpha = 0.22f), style = Stroke(width = 5.5f * u, cap = StrokeCap.Round))
        drawPath(route, Palette.Route, style = Stroke(width = 2.2f * u, cap = StrokeCap.Round))

        val pm = PathMeasure().apply { setPath(route, false) }
        val len = pm.length
        fun at(f: Float) = pm.getPosition(len * f.coerceIn(0f, 1f))
        val meetF = 0.47f

        fun label(text: String, anchor: Offset, color: Color, dy: Float = -6f * u) {
            val style = TextStyle(color = color, fontSize = (3.6f * u).toSp(), fontWeight = FontWeight.Bold)
            val r = measurer.measure(text, style)
            val pad = 1.6f * u
            val tl = Offset(anchor.x - r.size.width / 2f - pad, anchor.y + dy - r.size.height - pad)
            drawRoundRect(
                Color(0xE60B0E12), topLeft = tl,
                size = Size(r.size.width + pad * 2, r.size.height + pad * 2), cornerRadius = CornerRadius(2f * u),
            )
            drawText(r, topLeft = tl + Offset(pad, pad))
        }
        fun ambulance(c: Offset) {
            val s = 3.4f * u
            drawRoundRect(Palette.Alarm, topLeft = c - Offset(s, s), size = Size(2 * s, 2 * s), cornerRadius = CornerRadius(1.2f * u))
            drawLine(Color.White, c - Offset(s * 0.55f, 0f), c + Offset(s * 0.55f, 0f), strokeWidth = 0.9f * u)
            drawLine(Color.White, c - Offset(0f, s * 0.55f), c + Offset(0f, s * 0.55f), strokeWidth = 0.9f * u)
        }
        fun myCar(c: Offset) {
            drawCircle(Palette.Route.copy(alpha = 0.25f), radius = 5.5f * u, center = c)
            drawCircle(Color.White, radius = 3f * u, center = c)
            drawCircle(Palette.Route, radius = 2f * u, center = c)
        }

        if (meetPoint) {
            val meet = at(meetF)
            // 지나온/남은 구간 강조: 내 차 → 접선 지점은 틸
            val seg = Path()
            pm.getSegment(0f, len * meetF, seg, true)
            drawPath(seg, Palette.Calm, style = Stroke(width = 2.6f * u, cap = StrokeCap.Round))
            drawCircle(Palette.Amber.copy(alpha = 0.18f + 0.1f * (1 - progress)), radius = (7f + 5f * progress) * u, center = meet)
            drawCircle(Palette.Amber, radius = 3.6f * u, center = meet)
            drawCircle(Palette.Ground, radius = 1.4f * u, center = meet)
            label("접선 · 졸음쉼터", meet, Palette.Amber, dy = -6.5f * u)
            val car = at(meetF * progress * 0.94f)
            val amb = at(1f - (1f - meetF) * progress * 0.9f)
            ambulance(amb)
            label("구급차", amb, Palette.AlarmText)
            myCar(car)
            label("내 차", car, Palette.Ink)
        } else {
            val now = at(0.12f + 0.5f * progress)
            // 다음 회전 지점 (예고 대상)
            val turn = at(0.62f)
            drawCircle(Palette.Calm.copy(alpha = 0.25f), radius = 6f * u, center = turn)
            drawCircle(Palette.Calm, radius = 2.4f * u, center = turn)
            label("급커브 · 회전 예고", turn, Palette.Calm)
            val rest = at(0.86f)
            drawCircle(Palette.Amber, radius = 2.6f * u, center = rest)
            label("휴게소 6km", rest, Palette.Amber)
            myCar(now)
        }
    }
}

/** 좌석 표시 — 활성(모니터링 대상)만 틸 */
@Composable
fun SeatIndicator(label: String, on: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Palette.Panel2, RoundedCornerShape(16.dp)).padding(vertical = 16.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(18.dp).background(if (on) Palette.Calm else Color(0xFF56616C), CircleShape))
        Text(label, color = if (on) Palette.Ink else Palette.Ink2, fontSize = 15.sp, textAlign = TextAlign.Center)
    }
}

/**
 * 하단 행동 영역 — 화면별 다음 행동은 왼쪽, 응급 버튼은 항상 오른쪽 아래 같은 자리 (절대 원칙 7).
 * [onEmergency]가 null이면(이미 응급 흐름) 응급 버튼 자리를 비우지 않고 왼쪽 행동만 둔다.
 */
@Composable
fun ActionBar(
    onEmergency: (() -> Unit)?,
    hint: String? = null,
    left: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().height(96.dp).padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            left()
        }
        if (onEmergency != null) {
            hint?.let { Text(it, color = Palette.Ink2, fontSize = GtType.Caption) }
            Box(
                Modifier.size(width = 216.dp, height = 64.dp)
                    .pressable(onEmergency)
                    .background(Palette.Alarm, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Text("응급 모드", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * 좌석 배치도 — 위에서 본 차량 실루엣에 앞 2 / 뒤 1 좌석. 탑승(착좌 신호)은 Calm 채움, 빈 좌석은 윤곽만.
 * 문자 점 대신 공간 배치로 "어느 좌석인지"를 바로 읽게 한다.
 */
@Composable
fun SeatMap(driver: Boolean, passenger: Boolean, rear: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val body = Size(w * 0.86f, h * 0.92f)
        val origin = Offset((w - body.width) / 2, (h - body.height) / 2)
        val r = CornerRadius(body.width * 0.28f)
        drawRoundRect(Palette.Panel2, topLeft = origin, size = body, cornerRadius = r)
        // 앞 유리 (진행 방향 = 위)
        drawLine(
            Palette.Rule, Offset(origin.x + body.width * 0.2f, origin.y + body.height * 0.2f),
            Offset(origin.x + body.width * 0.8f, origin.y + body.height * 0.2f), strokeWidth = 2.dp.toPx(),
        )
        val seat = Size(body.width * 0.3f, body.height * 0.2f)
        fun seatAt(cx: Float, cy: Float, on: Boolean) {
            val tl = Offset(origin.x + body.width * cx - seat.width / 2, origin.y + body.height * cy - seat.height / 2)
            if (on) drawRoundRect(Palette.Ink2, topLeft = tl, size = seat, cornerRadius = CornerRadius(6.dp.toPx()))
            else drawRoundRect(
                Palette.Ink2.copy(alpha = 0.6f), topLeft = tl, size = seat, cornerRadius = CornerRadius(6.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        seatAt(0.32f, 0.38f, driver)    // 운전석 (좌핸들)
        seatAt(0.68f, 0.38f, passenger) // 조수석
        // 뒷좌석: 아이 카시트 위치 1개를 크게
        val rearSeat = Size(body.width * 0.5f, body.height * 0.2f)
        val rtl = Offset(origin.x + body.width * 0.5f - rearSeat.width / 2, origin.y + body.height * 0.7f - rearSeat.height / 2)
        if (rear) drawRoundRect(Palette.Calm, topLeft = rtl, size = rearSeat, cornerRadius = CornerRadius(6.dp.toPx()))
        else drawRoundRect(
            Palette.Ink2.copy(alpha = 0.6f), topLeft = rtl, size = rearSeat, cornerRadius = CornerRadius(6.dp.toPx()),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

/** 꺾인 화살표 — 커브 예고 (◀ 문자 대신) */
@Composable
fun TurnArrow(left: Boolean, color: Color, modifier: Modifier = Modifier.size(40.dp)) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = s * 0.12f
        val dir = if (left) -1f else 1f
        val cx = size.width / 2
        val p = Path().apply {
            moveTo(cx - dir * s * 0.15f, s * 0.92f)
            lineTo(cx - dir * s * 0.15f, s * 0.45f)
            quadraticTo(cx - dir * s * 0.15f, s * 0.3f, cx + dir * s * 0.02f, s * 0.3f)
            lineTo(cx + dir * s * 0.32f, s * 0.3f)
        }
        drawPath(p, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
        val tip = Offset(cx + dir * s * 0.42f, s * 0.3f)
        val head = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(tip.x - dir * s * 0.2f, tip.y - s * 0.17f)
            lineTo(tip.x - dir * s * 0.2f, tip.y + s * 0.17f)
            close()
        }
        drawPath(head, color)
    }
}
