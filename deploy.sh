#!/bin/bash

# RECIPICK 배포 스크립트
# 이 스크립트는 EC2에서 실행됩니다

set -e  # 에러 발생시 스크립트 중단

echo "🚀 RECIPICK 배포 시작..."

# 변수 설정
APP_NAME="recipick"
JAR_FILE=$(ls *.jar | head -1)
SERVICE_NAME="recipick"
DEPLOY_DIR="/opt/recipick"

echo "📦 발견된 JAR 파일: $JAR_FILE"

# 기존 서비스 중지
echo "⏹️  기존 서비스 중지 중..."
sudo systemctl stop $SERVICE_NAME || echo "서비스가 실행 중이 아닙니다"

# 기존 JAR 파일 백업
if [ -f "$DEPLOY_DIR/current.jar" ]; then
    echo "💾 기존 JAR 파일 백업..."
    sudo mv "$DEPLOY_DIR/current.jar" "$DEPLOY_DIR/backup-$(date +%Y%m%d_%H%M%S).jar"
fi

# 새 JAR 파일 복사
echo "📁 새 JAR 파일 설치..."
sudo cp "$JAR_FILE" "$DEPLOY_DIR/current.jar"

# .env 파일은 서버에서 직접 관리 (보안상 이유)
echo "ℹ️  .env 파일은 서버에서 직접 관리합니다"

# 권한 설정
sudo chown ubuntu:ubuntu "$DEPLOY_DIR/current.jar"
sudo chmod +x "$DEPLOY_DIR/current.jar"

# .env 파일이 존재할 경우 권한만 확인
if [ -f "$DEPLOY_DIR/.env" ]; then
    sudo chown ubuntu:ubuntu "$DEPLOY_DIR/.env"
fi

# systemd 서비스 파일이 없으면 생성
if [ ! -f "/etc/systemd/system/$SERVICE_NAME.service" ]; then
    echo "📝 systemd 서비스 파일 생성..."
    sudo tee "/etc/systemd/system/$SERVICE_NAME.service" > /dev/null <<EOF
[Unit]
Description=RECIPICK Spring Boot Application
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=$DEPLOY_DIR
Environment=SPRING_PROFILES_ACTIVE=prod
ExecStart=/usr/bin/java -jar $DEPLOY_DIR/current.jar
SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

    sudo systemctl daemon-reload
    sudo systemctl enable $SERVICE_NAME
fi

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

echo "🎉 배포 완료!"