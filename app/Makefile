# Makefile
run-dist: # запустить приложение
	./build/install/app/bin/app
make build:
	cd app && ./gradlew build --rerun-tasks
report:
	@echo "Генерация отчета о покрытии тестами..."
	./gradlew jacocoTestReport
	
.PHONY: build