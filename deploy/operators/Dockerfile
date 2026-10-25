ARG BASE_IMAGE=python:3.12-slim-bookworm
FROM ${BASE_IMAGE}
USER root
WORKDIR /opt/shopagent
COPY scripts/requirements.txt /tmp/operator-requirements.txt
RUN python -m pip install --no-cache-dir -r /tmp/operator-requirements.txt
COPY scripts/import_demo_catalog.py scripts/import_demo_catalog.py
COPY ai-service/knowledge/catalog.json ai-service/knowledge/catalog.json
COPY backend/src/main/resources/static/shopagent/catalog/ backend/src/main/resources/static/shopagent/catalog/
COPY deploy/scripts/import-catalog-container.py deploy/scripts/import-catalog-container.py
ENTRYPOINT ["python", "deploy/scripts/import-catalog-container.py"]
