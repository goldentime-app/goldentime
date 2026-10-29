# 골든타임 (Golden Time)

**이동 중 건강·응급 AI 코파일럿** — 현대자동차그룹 Pleos Connect 차량용 앱 개발 공모전 출품작

차량 데이터로 아이 멀미와 운전자 피로를 미리 막고, 응급 상황에서는 119 요약 전송부터 구급차와 만날 최적 지점(랑데부) 안내까지 이어 줍니다.

## 주요 기능

| 모드 | 화면 | 내용 |
|---|---|---|
| 일상 케어 | H0 · H1 · H2 | 핸들 각도·속도로 멀미 노출량(MSDV, ISO 2631-1 방식) 실시간 계산, 경로별 멀미 예보, 아동 장거리 휴식 권장 |
| 주의 | H3 | 발열 등 주의 상황에서 약국·야간 진료 경유 |
| 응급 | E1–E7 | 응급 대화 → 상황 요약·119 문자 전송(3초 취소) → 대기/직접/접선 비교 → 접선 이동 → 구급대 인계, 심정지 즉시 안내, 에어백 전개 자동 감지 |
| 설정 | S1 | 가족 프로필, 개인정보 원칙(차량에만 저장) |

## 안전 원칙

진단하지 않습니다 · 119 안내가 우선입니다 · 생명 관련 안내는 고정 대본만 씁니다 · 자동 전송은 사전 동의 + 3초 취소 · 과속을 유도하지 않습니다.

## Pleos Connect SDK 사용

- **Vehicle SDK 2.3.2** — SteeringWheel(핸들 각도), Brake, Seat(좌석별 탑승), Safety(에어백 전개)
- **NaviHelper SDK 3.0.5** — requestTrip(도착 예정 시각), addWaypoint, changeRouteOption(COMFORTABLE), onTBTInfo
- **FusedLocation SDK 1.1.3** — 위치 기반 속도 추정, 접선 지점 반경 진입 감지(addProximityAlert)
- **TextToSpeech / SpeechToText SDK 3.3.1** — HYBRID 모드 한국어 음성 안내·응급 접수
- **실제 지도** — MapLibre Android 12.3.1 + OpenFreeMap 타일(키 불필요), 경로는 OSRM으로 받은 영동고속도로 구간(`app/src/main/assets/demo_route.json`). 지도 데이터 © OpenStreetMap 기여자
- **국립중앙의료원 응급의료기관 정보 API** — 가까운 응급실과 실시간 가용 병상 (`local.properties`에 `datago.key`가 있을 때)
- SDK 호출은 인터페이스(`data/`) 뒤에 있어, 초기화에 실패하면 시연용 Fake 구현으로 자동 전환됩니다.

## 실행

1. Pleos Connect 에뮬레이터 v2.1.0 (API 34, RAM 4GB, Cold boot)
2. 저장소 `apk/` 폴더의 APK 설치: `adb install apk/세로에이아이_골든타임_release.apk`
3. 상단 "골든타임" 제목을 길게 누르면 시연 바가 나타나요. 시나리오 A~E 선택
   - 또는 `adb shell am start -n ai.seroai.goldentime/.MainActivity -f 0x20000000 --es gt_demo A --ei gt_step 7`
   - A 고속도로 열성경련(응급 전체 흐름) · B 사고 · C 심정지 · D 멀미 예보·대응 · E 발열 주의
4. Virtual Console에서 핸들 각도를 바꾸면 홈 화면 멀미 게이지가 실시간으로 반응합니다.

직접 빌드: JDK 17+, `local.properties`에 `sdk.dir` 지정 후 `gradlew assembleDebug`
(선택) `pleos.client.id`·`pleos.client.secret`(서버 음성), `datago.key`(응급실 실시간 병상)를 넣으면 실서비스 연동, 없으면 차량 내 엔진·시연 데이터로 동작

## 구조

```
app/src/main/java/ai/seroai/goldentime/
  core/       멀미 모델(MotionModel, LiveMotionEngine), 경로 예보, 랑데부 계산, 119 요약, 휴식 계획 — 순수 로직
  emergency/  응급 흐름, 고정 대본, 119 전송 제어, 사고 감지
  data/       Vehicle·Navi·Location 소스 (Pleos 구현 + Fake)
  voice/      음성 (현재 Android TTS/STT)
  demo/       시연 시나리오 A~E
  ui/         Jetpack Compose 화면
prototype/    웹 프로토타입 (계산 기준 구현)
```

테스트: `gradlew testDebugUnitTest` (단위 테스트 49개)
