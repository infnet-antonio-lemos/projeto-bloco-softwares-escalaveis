# Implantação em Kubernetes

Um único conjunto de manifests, em **escala de desenvolvimento** (1 réplica por serviço),
aplicado com Kustomize.

```
k8s/
├── kustomization.yaml       # namespace, rótulos comuns, tags das imagens
├── namespace.yaml
├── config.yaml              # ConfigMap: endereços e perfis (nada sensível)
├── secrets.yaml             # Secret: credenciais (demo — ver aviso no arquivo)
├── postgres/                # StatefulSet + Service headless + PVC
├── rabbitmq/                # StatefulSet + Service (NodePort na UI) + PVC
├── <serviço>/               # Deployment + Service, um por serviço
├── ingress.yaml
└── deploy-local.sh          # constrói, carrega no cluster e aplica
```

## Subir localmente

Requer um cluster **minikube**:

```bash
minikube start --cpus=4 --memory=6g
./k8s/deploy-local.sh
```

Acesso, no minikube:

| Serviço | URL |
| --- | --- |
| Frontend | `http://$(minikube ip):30000` |
| RabbitMQ Management | `http://$(minikube ip):30672` (`petclinic` / `petclinic`) |

> **No WSL2**:
>
> ```bash
> kubectl -n petclinic port-forward svc/frontend 3000:80
> kubectl -n petclinic port-forward svc/api-gateway 8080:8080
> kubectl -n petclinic port-forward svc/rabbitmq 15672:15672
> ```

Em outros clusters sem NodePort exposto, o `port-forward` acima é igualmente o caminho.