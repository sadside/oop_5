#!/bin/bash

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Скрипт очистки временных файлов ===${NC}"

# Остановка сервера, если он запущен
echo -e "${YELLOW}Проверка и останов серверов...${NC}"
SERVER_PORT=8080
PID=$(lsof -ti :$SERVER_PORT -sTCP:LISTEN)
if [ -n "$PID" ]; then
    echo -e "Остановка сервера с PID: ${RED}$PID${NC}..."
    kill $PID
    sleep 1
    if kill -0 $PID > /dev/null 2>&1; then
        echo -e "${RED}Не удалось остановить сервер автоматически.${NC}"
    else
        echo -e "${GREEN}Сервер успешно остановлен.${NC}"
    fi
else
    echo -e "${GREEN}Серверы не обнаружены.${NC}"
fi

# Очистка логов
echo -e "${YELLOW}Очистка логов...${NC}"
rm -rf ../logs/server/java/*
rm -rf ../logs/server/xml/*
rm -rf ../logs/client/java/*
rm -rf ../logs/client/xml/*
echo -e "${GREEN}Логи очищены.${NC}"

# Удаление других временных файлов
echo -e "${YELLOW}Очистка временных файлов...${NC}"
find .. -name "*.log" -delete
find .. -name "nohup.out" -delete
echo -e "${GREEN}Временные файлы удалены.${NC}"

echo -e "${GREEN}=== Очистка завершена ===${NC}" 