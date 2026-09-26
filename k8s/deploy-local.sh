#!/usr/bin/env bash
# Sobe a stack completa num cluster minikube local.
#
# Uso: ./k8s/deploy-local.sh
#
# O passo que costuma faltar é o de carregar as imagens no cluster: o nó do minikube
# tem seu próprio daemon Docker e não enxerga as imagens construídas na máquina. Sem o
# `minikube image load`, os pods ficam em ImagePullBackOff tentando baixar
# "petclinic/backend:dev" de um registry público onde ela não existe.
set -euo pipefail

SERVICES=(api-gateway backend appointment-service frontend)

cd "$(dirname "$0")/.."

# Falha cedo e com mensagem clara: sem esta checagem, um kubeconfig apontando para
# outro cluster faria o script construir as imagens, carregá-las no minikube (que pode
# nem estar rodando) e aplicar os manifests num lugar inesperado.
CONTEXT=$(kubectl config current-context 2>/dev/null || echo "")
if [ "$CONTEXT" != "minikube" ]; then
  echo "Erro: o contexto atual do kubectl é '${CONTEXT:-nenhum}', não 'minikube'." >&2
  echo "      Rode 'minikube start' e/ou 'kubectl config use-context minikube'." >&2
  exit 1
fi

echo "==> Construindo imagens (tag :dev)"
for svc in "${SERVICES[@]}"; do
  echo "    - $svc"
  docker build -q -t "petclinic/$svc:dev" "./$svc" >/dev/null
done

echo "==> Carregando imagens no cluster"
for svc in "${SERVICES[@]}"; do
  minikube image load "petclinic/$svc:dev"
done

echo "==> Aplicando os manifests"
# Sempre com -k: os manifests não declaram namespace no metadata, quem injeta é o
# kustomization.yaml. Um `apply -f` criaria tudo em `default` sem reclamar.
kubectl apply -k k8s

echo "==> Aguardando os pods ficarem prontos"
# Infra primeiro: as aplicações reiniciam em laço até o banco e o broker aceitarem
# conexões. Esperar aqui torna a saída legível, em vez de uma parede de CrashLoopBackOff.
kubectl -n petclinic rollout status statefulset/postgres --timeout=180s
kubectl -n petclinic rollout status statefulset/rabbitmq --timeout=180s
for svc in "${SERVICES[@]}"; do
  kubectl -n petclinic rollout status "deployment/$svc" --timeout=300s
done

echo
kubectl -n petclinic get pods
echo
echo "Acesso — port-forward (funciona em qualquer ambiente, inclusive WSL2):"
echo "  kubectl -n petclinic port-forward svc/frontend    3000:80    # http://localhost:3000"
echo "  kubectl -n petclinic port-forward svc/api-gateway 8080:8080"
echo "  kubectl -n petclinic port-forward svc/rabbitmq    15672:15672  (petclinic/petclinic)"
echo
echo "Use localhost, não 127.0.0.1: a lista de origens do CORS do gateway é literal."
echo
echo "Acesso — NodePort no IP do nó (não roteia no WSL2):"
echo "  Frontend:    http://$(minikube ip):30000"
echo "  RabbitMQ UI: http://$(minikube ip):30672"
