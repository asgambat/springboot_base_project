#!/bin/bash
echo "========================================"
echo "  Smoke Test ms-base-prj"
echo "========================================"
echo ""
echo "GET http://localhost:8080/api/hello?name=Spring"
echo ""
curl -s -w "\n\nHTTP Status: %{http_code}\n" "http://localhost:8080/api/hello?name=Spring"
