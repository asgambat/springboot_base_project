#!/bin/bash
echo "========================================"
echo "  Avvio ms-base-prj (Spring Boot)"
echo "========================================"
cd "$(dirname "$0")"

if [ ! -f .env ]; then
    echo "ERRORE: file .env non trovato. Crea un file .env con JDK_PATH=percorso_jdk"
    exit 1
fi
source .env
if [ -z "$JDK_PATH" ]; then
    echo "ERRORE: JDK_PATH non definito in .env"
    exit 1
fi
export JAVA_HOME="$JDK_PATH"
export PATH="$JAVA_HOME/bin:$PATH"

echo "Compilazione in corso..."
./mvnw -q clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "ERRORE: build fallita."
    exit 1
fi
echo "Avvio server..."
"$JAVA_HOME/bin/java" -jar target/ms-base-prj-0.0.1-SNAPSHOT.jar
