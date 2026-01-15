# Toss Same Cats Helper (토스 같은 고양이 맞추기 도우미)

이 앱은 토스(Toss) 앱의 미니 게임인 "같은 고양이 맞추기"를 도와주는 안드로이드 도우미 애플리케이션입니다. 화면 오버레이를 통해 게임 화면을 캡처하고, 이미지 분석을 수행한 뒤, 같은 그림의 카드를 찾아 사용자에게 알려줍니다.

## 주요 기능 (Features)

*   **오버레이 컨트롤 (Overlay Controls)**: 다른 앱 위에 떠 있는 플로팅 버튼을 통해 게임 중에도 쉽게 조작할 수 있습니다.
*   **화면 캡처 및 분석 (Screen Capture & Analysis)**: 미디어 프로젝션(Media Projection) 권한을 사용하여 현재 화면을 캡처하고, 이미지를 분석합니다.
*   **자체 이미지 처리 알고리즘**: `ImageAnalyzer` 클래스 내에서 Flood Fill 및 MSE(Mean Squared Error) 알고리즘을 직접 구현하여 고양이 카드를 식별하고 매칭합니다.
*   **다양한 그리드 지원**: 2x2, 3x2, 4x2, 4x3, 4x4, 5x4, 6x4, 6x5 등 다양한 게임 격자 크기를 지원합니다.
*   **자동 스크롤 (Auto-Scroll)**: 한 단계의 분석이 끝나고 중지하면, 자동으로 다음 단계의 그리드 버튼으로 스크롤하여 빠른 진행을 돕습니다.
*   **결과 표시 (Result Visualization)**: 분석된 결과를 화면 위에 오버레이로 그려주어, 사용자가 어떤 카드를 선택해야 할지 직관적으로 보여줍니다.

## 이미지 분석 알고리즘 (Image Analysis Logic)

`ImageAnalyzer`는 외부 라이브러리 없이 Android의 기본 `Bitmap` 처리 기능만을 사용하여 자체적으로 구현되었습니다. 분석 과정은 다음 단계로 이루어집니다.

1.  **전처리 (Preprocessing)**: 성능 최적화를 위해 원본 이미지를 축소(Downscale)하고, `Flood Fill` 알고리즘을 사용하여 배경(Background)과 전경(Foreground)을 분리합니다.
2.  **블롭 감지 (Blob Detection)**: 분리된 전경 픽셀들 중에서 연결된 구성 요소(Connected Components)를 찾아 '블롭(Blob)'으로 식별합니다. 이 블롭들이 잠재적인 카드 후보가 됩니다.
3.  **필터링 (Filtering)**: 식별된 블롭 중 카드의 비율(Aspect Ratio)과 크기(Area) 조건에 맞지 않는 노이즈를 제거하여 유효한 카드 영역만 남깁니다.
4.  **정렬 (Sorting)**: 추출된 카드 영역들을 게임 격자(Grid) 구조에 맞춰 좌상단부터 우하단 순서로 정렬합니다.
5.  **매칭 (Matching)**:
    *   각 카드 영역을 크롭하고 32x32 크기로 정규화합니다.
    *   모든 카드 쌍에 대해 픽셀 간의 색상 차이(MSE, Mean Squared Error)를 계산합니다.
    *   오차 값이 임계값(Threshold) 이하인 경우 같은 그림의 카드로 판단하여 그룹화합니다.

## 설치 및 사용 방법 (Usage)

1.  **앱 실행**: 앱을 실행하고 권한 요청을 허용합니다.
    *   **다른 앱 위에 그리기 권한 (Overlay Permission)**: 오버레이 컨트롤 및 결과 표시를 위해 필요합니다.
    *   **화면 녹화/캡처 권한 (Media Projection)**: 게임 화면을 분석하기 위해 필요합니다.
2.  **게임 시작**: 토스 앱의 "같은 고양이 맞추기" 게임을 실행합니다.
3.  **분석 시작**:
    *   도우미 앱의 오버레이 컨트롤에서 현재 게임의 카드 배치(예: 4x2)에 맞는 버튼을 누릅니다.
    *   화면이 잠시 캡처되고 분석이 수행됩니다.
4.  **결과 확인**:
    *   같은 그림의 카드 위에 색상과 번호가 표시됩니다.
    *   표시된 힌트를 보고 게임을 진행합니다.
5.  **다음 단계**:
    *   화면의 'Stop' 버튼이나 빈 공간을 눌러 결과 화면을 닫습니다.
    *   앱이 자동으로 다음 단계 버튼으로 스크롤합니다.

## 기술 스택 (Tech Stack)

*   **Language**: Kotlin
*   **Platform**: Android SDK (Min SDK 31, Target SDK 36)
*   **Image Processing**: Custom Algorithm (Flood Fill, MSE) using Android Graphics Bitmap
*   **Architecture components**: ViewBinding, Service (Foreground Service), MediaProjectionManager

## 개발 환경 (Development)

*   Android Studio
*   Gradle

## 주의사항

*   이 앱은 개인적인 학습 및 편의를 위해 제작된 토이 프로젝트입니다.
*   게임의 그래픽이나 UI가 변경될 경우 인식률이 떨어질 수 있습니다.
