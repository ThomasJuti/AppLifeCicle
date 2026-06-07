#!/usr/bin/env bash
# Capa 2: revisión con IA (Google Gemini) — analiza el diff del PR.
set -euo pipefail

BASE_SHA="${BASE_SHA:?BASE_SHA required}"
HEAD_SHA="${HEAD_SHA:?HEAD_SHA required}"
REPORT_FILE="${REPORT_FILE:?REPORT_FILE required}"
GEMINI_API_KEY="${GEMINI_API_KEY:-}"
GEMINI_MODEL="${GEMINI_MODEL:-gemini-2.5-flash}"
GEMINI_MODEL_FALLBACKS="${GEMINI_MODEL_FALLBACKS:-gemini-2.5-flash,gemini-2.0-flash-lite,gemini-1.5-flash}"
PR_TITLE="${PR_TITLE:-}"
PR_BODY="${PR_BODY:-}"

if [[ -z "$GEMINI_API_KEY" ]]; then
  {
    echo "### Revisión con IA"
    echo ""
    echo "_Omitida: configura el secret \`GEMINI_API_KEY\` en el repo para activar esta capa._"
  } > "$REPORT_FILE"
  exit 0
fi

mapfile -t CHANGED < <(git diff --name-only --diff-filter=ACMR "$BASE_SHA" "$HEAD_SHA")
FILE_LIST=$(printf '%s\n' "${CHANGED[@]}")

# Limitar diff (~30k chars) para cuota free tier
DIFF=$(git diff "$BASE_SHA" "$HEAD_SHA" | head -c 30000)
if [[ $(git diff "$BASE_SHA" "$HEAD_SHA" | wc -c) -gt 30000 ]]; then
  DIFF="${DIFF}

... [diff truncado por tamaño]"
fi

SYSTEM_PROMPT='Eres un revisor senior de pull requests para LifeCicleApp (kata Fullstack/Cloud).

Stack del proyecto:
- Backend: Java 21, Spring Boot 3, JWT, JPA, Flyway, PostgreSQL/H2, arquitectura hexagonal
- Frontend: React 18, Vite, Axios, React Router
- Infra: Terraform, AWS (CloudFront, S3, ALB, ECS, RDS, ECR, Secrets Manager)
- CI/CD: GitHub Actions (ci.yml, deploy.yml)

Responde SIEMPRE en español y en Markdown con esta estructura exacta:

### Revisión con IA

**Resumen:** (1-2 oraciones)

**Riesgos** (bullets, o "Ninguno relevante")
**Calidad / tests** (bullets)
**Seguridad** (bullets — secretos, CORS, JWT, .env, tfvars)
**Infra / deploy** (bullets si aplica, si no "N/A")
**Sugerencias** (bullets concretas y accionables)

Sé conciso. No repitas reglas obvias. Prioriza ciclo de vida dev/prod, seguridad y despliegue AWS.'

USER_CONTENT="## Pull Request
**Título:** ${PR_TITLE}

**Descripción:**
${PR_BODY:-_(sin descripción)_}

## Archivos modificados (${#CHANGED[@]})
\`\`\`
${FILE_LIST}
\`\`\`

## Diff
\`\`\`diff
${DIFF}
\`\`\`"

REQUEST=$(jq -n \
  --arg system "$SYSTEM_PROMPT" \
  --arg user "$USER_CONTENT" \
  '{
    systemInstruction: {parts: [{text: $system}]},
    contents: [{role: "user", parts: [{text: $user}]}],
    generationConfig: {temperature: 0.2, maxOutputTokens: 1200}
  }')

call_gemini() {
  local model="$1"
  local api_url="https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${GEMINI_API_KEY}"
  curl -sS -w "\n%{http_code}" "$api_url" \
    -H "Content-Type: application/json" \
    -d "$REQUEST"
}

# Modelos a probar: el configurado + fallbacks (sin duplicados)
MODELS=("$GEMINI_MODEL")
IFS=',' read -ra FALLBACKS <<< "$GEMINI_MODEL_FALLBACKS"
for m in "${FALLBACKS[@]}"; do
  [[ "$m" == "$GEMINI_MODEL" ]] && continue
  MODELS+=("$m")
done

BODY=""
CODE=""
USED_MODEL=""
LAST_ERROR=""

for model in "${MODELS[@]}"; do
  for attempt in 1 2; do
    HTTP=$(call_gemini "$model")
    BODY=$(echo "$HTTP" | sed '$d')
    CODE=$(echo "$HTTP" | tail -n 1)

    if [[ "$CODE" == "200" ]]; then
      USED_MODEL="$model"
      break 2
    fi

    LAST_ERROR="$BODY"

    if [[ "$CODE" == "429" ]] && [[ "$attempt" -lt 2 ]]; then
      sleep 15
      continue
    fi

    # 404/400: probar siguiente modelo; 429 agotado: también siguiente
    break
  done
done

if [[ "$CODE" != "200" ]]; then
  {
    echo "### Revisión con IA"
    echo ""
    echo "_Error al llamar a Gemini (HTTP ${CODE}). Cuota agotada o plan sin acceso al modelo._"
    echo ""
    echo "**Qué hacer:**"
    echo "1. En [Google AI Studio](https://aistudio.google.com/) verifica cuota y que la API esté habilitada."
    echo "2. Si el error dice \`limit: 0\`, puede hacer falta **vincular facturación** en Google Cloud (el tier gratuito sigue siendo gratis hasta el límite)."
    echo "3. Opcional: variable de repo \`GEMINI_MODEL\` (p. ej. \`gemini-2.5-flash\` o \`gemini-1.5-flash\`)."
    echo ""
    echo "<details><summary>Detalle</summary>"
    echo ""
    echo '```json'
    echo "$LAST_ERROR" | head -c 2000
    echo '```'
    echo "</details>"
  } > "$REPORT_FILE"
  exit 0
fi

REVIEW=$(echo "$BODY" | jq -r '.candidates[0].content.parts[0].text // empty')

if [[ -z "$REVIEW" ]]; then
  echo "### Revisión con IA" > "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
  echo "_No se recibió respuesta del modelo._" >> "$REPORT_FILE"
  exit 0
fi

{
  echo "$REVIEW"
  echo ""
  echo "_Modelo: \`${USED_MODEL}\`_"
} > "$REPORT_FILE"
