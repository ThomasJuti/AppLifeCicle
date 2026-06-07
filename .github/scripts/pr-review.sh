#!/usr/bin/env bash
# Agente de revisión de PR — analiza el diff y genera un informe en Markdown.
set -euo pipefail

BASE_SHA="${BASE_SHA:?BASE_SHA required}"
HEAD_SHA="${HEAD_SHA:?HEAD_SHA required}"
REPORT_FILE="${REPORT_FILE:?REPORT_FILE required}"

mapfile -t CHANGED < <(git diff --name-only --diff-filter=ACMR "$BASE_SHA" "$HEAD_SHA")

findings=()
warnings=()
ok=()

add_finding()  { findings+=("$1"); }
add_warning()  { warnings+=("$1"); }
add_ok()       { ok+=("$1"); }

contains() {
  local needle="$1"
  local f
  for f in "${CHANGED[@]}"; do
    [[ "$f" == *"$needle"* ]] && return 0
  done
  return 1
}

any_match() {
  local pattern="$1"
  local f
  for f in "${CHANGED[@]}"; do
    [[ "$f" =~ $pattern ]] && return 0
  done
  return 1
}

# ── 1. Archivos sensibles ──────────────────────────────────────────────
SENSITIVE_PATTERNS=(
  'terraform\.tfvars$'
  '\.tfstate'
  '^\.env$'
  '\.env\.local$'
  '\.pem$'
  '\.key$'
  'credentials'
  'secrets\.json'
)

for f in "${CHANGED[@]}"; do
  for pat in "${SENSITIVE_PATTERNS[@]}"; do
    if [[ "$f" =~ $pat ]] && [[ "$f" != *".example"* ]]; then
      add_finding "Archivo sensible en el PR: \`$f\` — no debe subirse al repo."
    fi
  done
done

# ── 2. Patrones de secretos en líneas añadidas ─────────────────────────
ADDED=$(git diff "$BASE_SHA" "$HEAD_SHA" --unified=0 || true)

if echo "$ADDED" | grep -qE '^\+.*AKIA[0-9A-Z]{16}'; then
  add_finding "Posible **AWS Access Key** en líneas añadidas."
fi

if echo "$ADDED" | grep -qiE '^\+.*(password|secret|api[_-]?key)\s*[:=]\s*["'"'"'][^"'"'"']{8,}'; then
  if ! echo "$ADDED" | grep -qE '^\+.*\$\{'; then
    add_warning "Posible credencial hardcodeada en el diff — usa variables de entorno o Secrets Manager."
  fi
fi

# ── 3. Backend sin tests ───────────────────────────────────────────────
MAIN_JAVA=false
TEST_JAVA=false
for f in "${CHANGED[@]}"; do
  [[ "$f" == src/main/java/* ]] && MAIN_JAVA=true
  [[ "$f" == src/test/java/* ]] && TEST_JAVA=true
done

if $MAIN_JAVA && ! $TEST_JAVA; then
  add_warning "Cambios en \`src/main/java\` sin tests en \`src/test/java\` — considera añadir o actualizar pruebas."
fi

if $MAIN_JAVA && $TEST_JAVA; then
  add_ok "Backend: código y tests modificados."
fi

# ── 4. Infra / deploy ──────────────────────────────────────────────────
if contains "infra/modules/ecs/"; then
  add_warning "Cambios en ECS — verifica \`healthCheckGracePeriodSeconds\` (Spring tarda ~90s en arrancar)."
fi

if contains ".github/workflows/deploy.yml"; then
  add_warning "Cambios en pipeline de deploy — al mergear a \`main\` se desplegará en AWS."
fi

if contains "infra/"; then
  if ! contains ".terraform.lock.hcl" && any_match '^infra/'; then
    add_warning "Cambios en Terraform — ejecuta \`terraform fmt\` y \`terraform validate\` localmente."
  else
    add_ok "Infra: revisa que CI Terraform pase en verde."
  fi
fi

# ── 5. Frontend ────────────────────────────────────────────────────────
if contains "frontend/src/"; then
  add_ok "Frontend modificado — CI compilará con \`npm run build\`."
  if contains "frontend/.env" && ! contains "frontend/.env.example"; then
    add_finding "No subas \`frontend/.env\` con valores reales — usa \`.env.example\`."
  fi
fi

# ── 6. Tamaño del PR ───────────────────────────────────────────────────
COUNT=${#CHANGED[@]}
if (( COUNT > 30 )); then
  add_warning "PR grande ($COUNT archivos) — considera dividirlo para revisión más fácil."
fi

if (( COUNT == 0 )); then
  add_warning "No se detectaron archivos cambiados en el diff."
fi

# ── Generar informe ────────────────────────────────────────────────────
{
  echo "## Capa 1 — Reglas automáticas"
  echo ""
  echo "Agente **PR Review** — $(date -u '+%Y-%m-%d %H:%M UTC')"
  echo ""
  echo "**Archivos modificados:** $COUNT"
  echo ""

  if ((${#findings[@]})); then
    echo "### Bloqueantes"
    for item in "${findings[@]}"; do
      echo "- $item"
    done
    echo ""
  fi

  if ((${#warnings[@]})); then
    echo "### Advertencias"
    for item in "${warnings[@]}"; do
      echo "- $item"
    done
    echo ""
  fi

  if ((${#ok[@]})); then
    echo "### OK"
    for item in "${ok[@]}"; do
      echo "- $item"
    done
    echo ""
  fi

  if ((! ${#findings[@]} && ! ${#warnings[@]})); then
    echo "### Resultado"
    echo "- Sin problemas detectados. Revisa CI (backend, frontend, terraform) antes de mergear."
    echo ""
  fi

  echo "<details><summary>Archivos en este PR</summary>"
  echo ""
  for f in "${CHANGED[@]}"; do
    echo "- \`$f\`"
  done
  echo ""
  echo "</details>"
  echo ""
  echo "---"
  echo "*Comentario generado por [.github/workflows/pr-review.yml](https://github.com/${GITHUB_REPOSITORY}/blob/main/.github/workflows/pr-review.yml)*"
} > "$REPORT_FILE"

# Exit code: falla CI si hay bloqueantes (opcional — comentamos siempre)
if ((${#findings[@]})); then
  echo "has_blockers=true" >> "${GITHUB_OUTPUT:-/dev/null}"
  exit 1
fi

echo "has_blockers=false" >> "${GITHUB_OUTPUT:-/dev/null}"
