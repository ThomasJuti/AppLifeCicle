#!/usr/bin/env bash
# Capa 2: revisión con IA (Google Gemini) — analiza el diff del PR.
set -euo pipefail

BASE_SHA="${BASE_SHA:?BASE_SHA required}"
HEAD_SHA="${HEAD_SHA:?HEAD_SHA required}"
REPORT_FILE="${REPORT_FILE:?REPORT_FILE required}"
GEMINI_API_KEY="${GEMINI_API_KEY:-}"
GEMINI_MODEL="${GEMINI_MODEL:-gemini-2.0-flash}"
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

# Limitar diff para no exceder contexto (~80k chars)
DIFF=$(git diff "$BASE_SHA" "$HEAD_SHA" | head -c 80000)
if [[ $(git diff "$BASE_SHA" "$HEAD_SHA" | wc -c) -gt 80000 ]]; then
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

API_URL="https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent?key=${GEMINI_API_KEY}"

HTTP=$(curl -sS -w "\n%{http_code}" "$API_URL" \
  -H "Content-Type: application/json" \
  -d "$REQUEST")

BODY=$(echo "$HTTP" | sed '$d')
CODE=$(echo "$HTTP" | tail -n 1)

if [[ "$CODE" != "200" ]]; then
  {
    echo "### Revisión con IA"
    echo ""
    echo "_Error al llamar a Gemini (HTTP ${CODE}). Verifica \`GEMINI_API_KEY\` y cuota._"
    echo ""
    echo "<details><summary>Detalle</summary>"
    echo ""
    echo '```json'
    echo "$BODY" | head -c 2000
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
  echo "_Modelo: \`${GEMINI_MODEL}\`_"
} > "$REPORT_FILE"
