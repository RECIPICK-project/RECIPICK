#!/bin/bash

# RECIPICK 배포 스크립트
# 이 스크립트는 EC2에서 실행됩니다

set -e  # 에러 발생시 스크립트 중단

echo "🚀 RECIPICK 배포 시작..."

# 변수 설정
SERVICE_NAME="recipick"
PROJECT_DIR="/home/ubuntu/RECIPICK"
BACKEND_DIR="$PROJECT_DIR/BACKEND/RECIPICK-PROJECT"

echo "📁 프로젝트 디렉토리로 이동: $PROJECT_DIR"
cd $PROJECT_DIR

# 기존 서비스 중지
echo "⏹️  기존 서비스 중지 중..."
sudo systemctl stop $SERVICE_NAME || echo "서비스가 실행 중이 아닙니다"

# 최신 코드 pull
echo "📥 최신 코드 가져오기..."
git stash || true  # 로컬 변경사항 임시 저장
git pull origin main

# 백엔드 디렉토리로 이동하여 빌드
echo "🔨 프로젝트 빌드 중..."
cd $BACKEND_DIR
./gradlew clean copyFrontend build -x test

# 서비스 시작
echo "🔄 서비스 시작 중..."
sudo systemctl start $SERVICE_NAME

# 서비스 상태 확인
sleep 5
if sudo systemctl is-active --quiet $SERVICE_NAME; then
    echo "✅ 배포 성공! 서비스가 정상적으로 실행 중입니다."
    sudo systemctl status $SERVICE_NAME --no-pager -l
else
    echo "❌ 배포 실패! 서비스 시작에 실패했습니다."
    echo "로그 확인:"
    sudo journalctl -u $SERVICE_NAME --no-pager -l -n 20
    exit 1
fi

# 헬스 체크 (선택적)
echo "🏥 헬스 체크 중..."
for i in {1..30}; do
    if curl -f -s http://localhost:8080/actuator/health > /dev/null 2>&1 || curl -f -s http://localhost:8080/ > /dev/null 2>&1; then
        echo "✅ 애플리케이션이 정상적으로 응답합니다!"
        break
    elif [ $i -eq 30 ]; then
        echo "⚠️  헬스 체크 타임아웃 (애플리케이션이 응답하지 않습니다)"
        echo "하지만 서비스는 실행 중입니다. 수동으로 확인해주세요."
        break
    else
        echo "헬스 체크 시도 $i/30..."
        sleep 2
    fi
done

echo "🎉 배포 완료!"