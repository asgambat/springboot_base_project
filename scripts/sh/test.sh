#!/bin/bash
echo "========================================"
echo "  Esecuzione test ms-base-prj"
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

./mvnw test
echo ""
echo "Test completati."
