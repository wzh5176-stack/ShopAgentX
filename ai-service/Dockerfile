FROM python:3.12-slim-bookworm

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates libgomp1 \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --gid 10001 shopagent \
    && useradd --uid 10001 --gid shopagent --no-create-home shopagent

COPY requirements.txt requirements-embedding.txt ./

RUN python -m pip install --no-cache-dir -r requirements.txt

COPY *.py ./
COPY knowledge/ ./knowledge/

USER shopagent

EXPOSE 8000

CMD ["python", "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]