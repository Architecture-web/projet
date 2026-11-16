@echo off
REM Demarre l'infrastructure locale de la demo : PostgreSQL, RabbitMQ et le faux course-service.
REM Ensuite, lancer GamificationServiceApplication dans IntelliJ SANS le profil "dev".
set PG=C:\Users\MSI\Desktop\postgres15
set RB=C:\Users\MSI\Desktop\rabbitmq
set ERLANG_HOME=%RB%\erlang
set RABBITMQ_BASE=%RB%\data

echo [1/3] PostgreSQL (port 5432)
start "postgres" /min "%PG%\extracted\pgsql\bin\postgres.exe" -D "%PG%\data"

echo [2/3] RabbitMQ (ports 5672 et 15672, interface : http://localhost:15672 guest/guest)
start "rabbitmq" /min "%RB%\server\rabbitmq_server-4.4.0\sbin\rabbitmq-server.bat"

echo [3/3] Faux course-service (port 8082)
start "course-service" /min node "%~dp0fake-course-service.js"

echo Termine. Attendre ~20 secondes que RabbitMQ soit pret.
