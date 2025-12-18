#!/bin/bash
BASE_PATH="/home/ubuntu"
BASE_SERVER_PATH=$BASE_PATH/kopang-server

CURRENT_TIME=$(date +%c)

# plain jar 제외하고 실행 가능한 jar 파일 하나만 정확히 찾기
BUILD_JAR_FILE=$(ls $BASE_SERVER_PATH/*.jar | grep -v 'plain' | tail -n 1)
JAR_NAME=$(basename "$BUILD_JAR_FILE")
echo "$CURRENT_TIME > build 파일명: $JAR_NAME"

LOG_PATH="$BASE_SERVER_PATH/log"
mkdir -p $LOG_PATH

DEPLOY_LOG="$LOG_PATH/deploy.log"
APP_LOG="$LOG_PATH/nohup.out"

echo "$CURRENT_TIME >  build 파일 복사" >> $DEPLOY_LOG
DEPLOY_PATH=$BASE_SERVER_PATH/deploy-jar/
mkdir -p $DEPLOY_PATH
cp "$BUILD_JAR_FILE" $DEPLOY_PATH

echo "$CURRENT_TIME > kopang-api-server-deploy.jar 교체" >> $DEPLOY_LOG
CP_JAR_PATH=$DEPLOY_PATH$JAR_NAME
APPLICATION_JAR_NAME=kopang-api-server-deploy.jar
APPLICATION_JAR=$DEPLOY_PATH$APPLICATION_JAR_NAME

echo "$CURRENT_TIME > 심볼릭 링크 설정" >> $DEPLOY_LOG

if [ -f "$CP_JAR_PATH" ]; then
    echo "  > 파일 복사 성공: $CP_JAR_PATH" >> $DEPLOY_LOG
else
    echo "  > 파일 복사 실패! 경로 확인 필요: $CP_JAR_PATH" >> $DEPLOY_LOG
    exit 1
fi

ln -Tfs "$CP_JAR_PATH" $APPLICATION_JAR

source $BASE_PATH/.profile

SPRING_PROFILES_ACTIVE="dev"

nohup java -jar \
  -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE \
  -DSTORAGE_DATABASE_DB_URL="$STORAGE_DATABASE_DB_URL" \
  -DSTORAGE_DATABASE_DB_NAME="$STORAGE_DATABASE_DB_NAME" \
  -DSTORAGE_DATABASE_DB_USERNAME="$STORAGE_DATABASE_DB_USERNAME" \
  -DSTORAGE_DATABASE_DB_PASSWORD="$STORAGE_DATABASE_DB_PASSWORD" \
  "$APPLICATION_JAR" > $APP_LOG 2>&1 &

echo "$CURRENT_TIME > build jar 파일 실행" >> $DEPLOY_LOG