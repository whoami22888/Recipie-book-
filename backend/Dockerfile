FROM python:3.13-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

COPY backend/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# The 424-recipe catalogue is restored from the committed transport chunks
# because the generated JSON is intentionally not tracked in git.
COPY data/catalogue_payload ./data/catalogue_payload
COPY scripts/restore_catalogue.py ./scripts/restore_catalogue.py
RUN python scripts/restore_catalogue.py

RUN useradd --create-home --uid 10001 appuser

USER appuser

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/health', timeout=3).read()"

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
