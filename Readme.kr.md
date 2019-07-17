# DOSBox.j
## 소개
dosbox를 java로 구현하는 프로젝트입니다.  
 핵심 기능들 위주로 작업했으며 나머지 미구현 부분은 필요에 따라 추가할 예정입니다.

## 실행 방법
1. jre 설치 확인(최소 jre8 이상 필요)
2. CLI 환경에서 
    ```
    $java dosboxj.jar
    ```
## Porting
* 기준 dosbox 버전 : [0.74](https://sourceforge.net/projects/dosbox/)
* 개발 환경 : jdk 8 이상
* 가상 메모리 범위 : 최대 가상 메모리는 2^31 - 1 bytes(약 2GB)까지 지원
* cpu는 simple, normal, full 중 normal 기준으로 작업
* 클래스와 메소드는 최대한 java naming convention에 따라 작성했으나 일부 field, 변수는 대상에서 제외함
* 미구현 기능
    * mixer, midi, debug, sblaster, gus, speaker,
    * mouse, joystick,
    * serial port, ipx,
    * mscdex, cdrom image

## 현재 상황
* 명령 창 표시
* 기본 명령 실행

## TODO
### 1. 소스 정리
* 의미없는 타입 변환 정리 : 편집기의 replace를 과도하게 사용하면서 추가된 의미없는 타입 변환 구문들 정리
* 이름 정리 : 일반적인 java naming convention을 따르지 않은 부분 존재 (field, 변수 등)
### 2. 미구현 기능
### 3. 최적화
* 메모리, 성능 관련 최적화 작업
### 4. 테스트
* CGA, EGA, VGA 등 환경에서 테스트 필요

## License
GNU GPL v2

## 참조
[dosbox homepage](https://www.dosbox.com/)