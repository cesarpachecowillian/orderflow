#!/bin/sh
# Popula o OrderFlow com volume considerável de dados mock realistas.
# Roda via: docker run --rm --network orderflow-network --entrypoint sh \
#            -v "$PWD/scripts:/scripts" curlimages/curl:latest /scripts/seed-data.sh

set -e

BASE="${BASE:-http://api-gateway:8080}"

extract() { grep -o "\"$1\":[^,}]*" | head -1 | sed -E "s/\"$1\"://; s/\"//g"; }

login()    { curl -fs -X POST "$BASE/auth/login"    -H "Content-Type: application/json" \
              -d "{\"email\":\"$1\",\"password\":\"$2\"}" | extract token; }
register() { curl -fs -X POST "$BASE/auth/register" -H "Content-Type: application/json" \
              -d "{\"name\":\"$1\",\"email\":\"$2\",\"password\":\"$3\"}" >/dev/null 2>&1 && return 0 || return 1; }
new_order(){ TOKEN=$(login "$1" senha123); \
             curl -fs -X POST "$BASE/orders" \
              -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
              -d "$2" | extract id; }
confirm()  { curl -fs -X PATCH "$BASE/orders/$2/confirm" -H "Authorization: Bearer $1" >/dev/null; }
cancel_user()  { TOKEN=$(login "$1" senha123); \
             curl -fs -X PATCH "$BASE/orders/$2/cancel" \
                  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
                  -d "{\"reason\":\"$3\"}" >/dev/null; }
cancel_admin() { curl -fs -X PATCH "$BASE/orders/$2/cancel" \
                  -H "Authorization: Bearer $1" -H "Content-Type: application/json" \
                  -d "{\"reason\":\"$3\"}" >/dev/null; }

echo "Aguardando API Gateway..."
i=0; until curl -fs -o /dev/null "$BASE/swagger-ui.html"; do
  i=$((i+1)); [ "$i" -gt 60 ] && { echo "Timeout"; exit 1; }
  sleep 2
done
echo "Gateway pronto."

# ---------- 30 usuários com nomes brasileiros realistas ----------
NAMES="Maria Silva|maria.silva@email.com
Bob Souza|bob.souza@email.com
Carlos Lima|carlos.lima@email.com
Ana Costa|ana.costa@email.com
João Pereira|joao.pereira@email.com
Fernanda Oliveira|fernanda.oliveira@email.com
Ricardo Almeida|ricardo.almeida@email.com
Juliana Santos|juliana.santos@email.com
Paulo Mendes|paulo.mendes@email.com
Beatriz Rocha|beatriz.rocha@email.com
Lucas Ferreira|lucas.ferreira@email.com
Camila Ribeiro|camila.ribeiro@email.com
Rafael Carvalho|rafael.carvalho@email.com
Mariana Gomes|mariana.gomes@email.com
Felipe Barbosa|felipe.barbosa@email.com
Larissa Cardoso|larissa.cardoso@email.com
Thiago Martins|thiago.martins@email.com
Patrícia Nunes|patricia.nunes@email.com
Gustavo Lopes|gustavo.lopes@email.com
Renata Pinto|renata.pinto@email.com
Eduardo Cavalcanti|eduardo.cavalcanti@email.com
Gabriela Moreira|gabriela.moreira@email.com
Bruno Teixeira|bruno.teixeira@email.com
Aline Correia|aline.correia@email.com
Vinícius Araújo|vinicius.araujo@email.com
Letícia Sousa|leticia.sousa@email.com
Diego Castro|diego.castro@email.com
Tatiane Rezende|tatiane.rezende@email.com
Henrique Fonseca|henrique.fonseca@email.com
Bianca Vieira|bianca.vieira@email.com"

echo
echo "== Cadastrando usuários =="
created_users=""
echo "$NAMES" | while IFS='|' read -r NAME EMAIL; do
  if register "$NAME" "$EMAIL" senha123; then
    echo "  + $EMAIL"
  else
    echo "  - $EMAIL (já existia)"
  fi
done

# ---------- catálogo de produtos para sortear ----------
# product|price|category
PRODUCTS="Notebook Dell XPS|7800.00
Notebook Lenovo ThinkPad|6500.00
Macbook Air M2|9200.00
Mouse Logitech MX Master|420.00
Mouse Razer DeathAdder|290.00
Teclado Mecânico Keychron|650.00
Teclado Logitech MX Keys|780.00
Webcam Full HD Logitech|350.00
Webcam 4K Razer|890.00
Monitor 27pol 4K|2400.00
Monitor Ultrawide 34pol|3200.00
Cadeira Gamer DT3|1850.00
Cadeira ergonômica Flexform|2400.00
Mousepad XL|120.00
Headset HyperX Cloud|680.00
Headset Sony WH-1000XM5|2900.00
SSD NVMe 1TB|650.00
SSD NVMe 2TB|1290.00
Memória DDR4 32GB|890.00
Memória DDR5 64GB|1750.00
Placa de Vídeo RTX 4070|4200.00
Placa de Vídeo RTX 4080|7800.00
Microfone Condensador HyperX|1100.00
Interface de Áudio Focusrite|890.00
Mesa Digitalizadora Wacom|1450.00
Smartphone Galaxy S24|4900.00
Tablet iPad Air|5400.00
Smartwatch Apple Watch|3200.00
Câmera Canon EOS R50|6900.00
Drone DJI Mini 4|4500.00
Roteador Wi-Fi 6 Asus|890.00
Switch Gigabit 8 portas|320.00
Cabo HDMI 2.1 (3m)|85.00
Hub USB-C 7-em-1|450.00
Suporte Articulado 2 monitores|550.00
Iluminação Ring Light|320.00"

# ---------- gera pedidos: cada usuário ganha 1-4 pedidos com 1-3 itens ----------
echo
echo "== Criando pedidos (1-4 por usuário, 1-3 itens cada) =="
ALL_IDS=""
USER_OF_ID=""
i=0
echo "$NAMES" | while IFS='|' read -r NAME EMAIL; do
  i=$((i+1))
  # 1..4 pedidos por usuário
  N=$(( (i * 7 + 3) % 4 + 1 ))
  pedido=0
  while [ $pedido -lt $N ]; do
    pedido=$((pedido+1))
    # 1..3 itens
    M=$(( (i * 3 + pedido) % 3 + 1 ))
    items_json=""
    item=0
    while [ $item -lt $M ]; do
      item=$((item+1))
      idx=$(( (i * 11 + pedido * 5 + item * 3) % 36 + 1 ))
      P=$(echo "$PRODUCTS" | sed -n "${idx}p")
      PRODUCT=$(echo "$P" | cut -d'|' -f1)
      PRICE=$(echo "$P" | cut -d'|' -f2)
      QTY=$(( (item + pedido) % 3 + 1 ))
      [ -n "$items_json" ] && items_json="$items_json,"
      items_json="$items_json{\"productName\":\"$PRODUCT\",\"quantity\":$QTY,\"unitPrice\":$PRICE}"
    done
    payload="{\"items\":[$items_json]}"
    OID=$(new_order "$EMAIL" "$payload")
    if [ -n "$OID" ]; then
      printf "."
      echo "$OID|$EMAIL" >> /tmp/orderflow_orders.txt
    fi
  done
done
echo

# ---------- admin confirma ~70%, cancela ~10%, deixa ~20% pending ----------
ADMIN=$(login admin@orderflow.com admin123)
[ -z "$ADMIN" ] && { echo "ERRO: admin login falhou"; exit 1; }

REASONS_USER="Mudei de ideia|Encontrei mais barato em outro lugar|Endereço errado, vou refazer|Não preciso mais"
REASONS_ADMIN="Estoque indisponível para o produto|Falha na confirmação de pagamento|Cliente solicitou alteração via SAC|Endereço fora da área de entrega|Suspeita de fraude"

echo
echo "== Processando pedidos (confirmar/cancelar/deixar pending) =="
n=0; confirmed=0; cancelled_user=0; cancelled_admin=0; pending=0
while IFS='|' read -r OID EMAIL; do
  n=$((n+1))
  # roda determinística: distribuição 70/10/10/10
  bucket=$(( n % 10 ))
  if [ $bucket -lt 7 ]; then
    confirm "$ADMIN" "$OID"; confirmed=$((confirmed+1))
  elif [ $bucket -eq 7 ]; then
    R=$(echo "$REASONS_USER" | tr '|' '\n' | sed -n "$(( (n % 4) + 1 ))p")
    cancel_user "$EMAIL" "$OID" "$R"; cancelled_user=$((cancelled_user+1))
  elif [ $bucket -eq 8 ]; then
    R=$(echo "$REASONS_ADMIN" | tr '|' '\n' | sed -n "$(( (n % 5) + 1 ))p")
    cancel_admin "$ADMIN" "$OID" "$R"; cancelled_admin=$((cancelled_admin+1))
  else
    pending=$((pending+1))
  fi
done < /tmp/orderflow_orders.txt
rm -f /tmp/orderflow_orders.txt

echo "  Confirmados: $confirmed | Cancelados (cliente): $cancelled_user | Cancelados (admin): $cancelled_admin | Pendentes: $pending"

echo
echo "== Resumo global =="
curl -fs -H "Authorization: Bearer $ADMIN" "$BASE/orders/summary"
echo
echo "Seed concluído."
