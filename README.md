[일반의약품 검색을 위한 OCR 기반 모바일 어플리케이션]

1. 개발기간 : 2020.1월~2022.11월
2. 담당 : 정희수 : 기획, 프론트엔드(안드로이드) 허정민 : 백엔드(서버)
6. 사용 언어 : Node.js(Javascript), Python, Java
7. 개발 환경 : AWS EC2 ubuntu, Android Studio
API : Google Cloud Vision API, KoNLPy mecab, 공공데이터포털 ‘의약품 제품 허가정보 서비스’

3. 기능 :

- 의약품 텍스트를 인식하여 검색 기능
 카메라 촬영) 사진 속 텍스트를 OCR API로 인식후 KoNLPy mecab으로 의약품명 추출 후 의약품 검색 데이터베이스에서 필요한 정보 제공
 사용자 입력 방식) 텍스트 인식 안되는 의약품은 사용자가 직접 검색 후 서버로 전송

- 검색 기록 저장 기능 : 검색 기록을 저장,거 저장 내용 확인
  

흐름도
<img width="445" alt="스크린샷 2024-09-06 오후 1 48 58" src="https://github.com/user-attachments/assets/5627cbdf-0d39-4008-8f83-26f4580b084c">


[MediMedi팀 졸업포스터.pdf](https://github.com/user-attachments/files/16902901/MediMedi.pdf)
