#!/usr/bin/env bash
# =====================================================================
# Prueba de humo de extremo a extremo (API) · Comanda v0.3
# Recorre las funcionalidades principales con el sistema en marcha:
#   docker compose up -d --build && ./scripts/prueba-humo.sh
# Requiere: curl y jq. Use una base de datos recién creada (docker compose down -v).
# =====================================================================
set -euo pipefail

API="${API:-http://localhost:8080/api}"
SALUD="${SALUD:-${API%/api}/actuator/health}"
RESP="$(mktemp)"
trap 'rm -f "$RESP"' EXIT

ok()    { echo "  ✔ $1"; }
falla() { echo "  ✘ $1"; echo "    respuesta ($CODE): $(cat "$RESP")"; exit 1; }
titulo(){ echo; echo "▶ $1"; }

# req MÉTODO RUTA TOKEN [CUERPO] → deja el código HTTP en $CODE y el cuerpo en $RESP
req() {
  local args=(-s -o "$RESP" -w '%{http_code}' -X "$1" "$API$2" -H "Content-Type: application/json")
  [[ -n "$3" ]] && args+=(-H "Authorization: Bearer $3")
  [[ $# -ge 4 ]] && args+=(-d "$4")
  CODE=$(curl "${args[@]}")
}
esperar() { [[ "$CODE" == "$1" ]] || falla "se esperaba HTTP $1"; }
campo()   { jq -r "$1" "$RESP"; }

titulo "Esperando al backend ($SALUD)"
for _ in $(seq 1 90); do
  if curl -sf "$SALUD" | grep -q UP; then ok "backend en línea"; break; fi
  sleep 2
done
curl -sf "$SALUD" | grep -q UP || { echo "  ✘ el backend no respondió"; exit 1; }

login() { req POST /auth/login "" "{\"username\":\"$1\",\"password\":\"$2\"}"; esperar 200; campo .token; }
MESERO=$(login mesero1 mesero123); COCINA=$(login cocina cocina123)
CAJA=$(login caja caja123); ADMIN=$(login admin admin123)
ok "inicio de sesión de los cuatro roles (RNF-04)"

titulo "F-01 · Estado de mesas"
req GET /mesas "$MESERO"; esperar 200
[[ $(campo 'length') == 12 ]] && ok "12 mesas" || falla "número de mesas"
req GET /caja/cuentas "$MESERO"; esperar 403; ok "el mesero no accede a caja (403)"

titulo "F-02 · Menú con disponibilidad por receta"
req GET /menu "$MESERO"; esperar 200
AJIACO=$(campo '.[] | select(.nombre=="Ajiaco santafereño") | .id')
LIMONADA=$(campo '.[] | select(.nombre=="Limonada natural") | .id')
BANDEJA=$(campo '.[] | select(.nombre=="Bandeja paisa") | .id')
[[ $(campo '.[] | select(.nombre=="Mojarra frita") | .disponible') == false ]] && ok "Mojarra frita sin stock" || falla "Mojarra"
[[ -z $(campo '.[] | select(.nombre=="Cazuela de mariscos") | .id') ]] && ok "plato suspendido oculto" || falla "Cazuela"

titulo "F-03 · F-06 · F-07 · Registrar pedido (mesa 3)"
CLAVE="prueba-humo-$(date +%s)"
CUERPO="{\"numeroMesa\":3,\"items\":[{\"productoId\":$AJIACO,\"cantidad\":1},{\"productoId\":$LIMONADA,\"cantidad\":2}],\"claveIdempotencia\":\"$CLAVE\"}"
req POST /pedidos "$MESERO" "$CUERPO"; esperar 201
PEDIDO=$(campo .id); [[ $(campo .total) == 32000* ]] && ok "pedido #$PEDIDO por \$32.000" || falla "total"

titulo "F-08 · Reintento idempotente desde la tablet"
req POST /pedidos "$MESERO" "$CUERPO"; esperar 201
[[ $(campo .id) == "$PEDIDO" ]] && ok "el reintento devuelve el mismo pedido (sin duplicar)" || falla "duplicado"
req GET /pedidos/cocina/estado "$MESERO"; esperar 200
[[ $(campo .comandasEnCola) -ge 1 ]] && ok "comanda en cola hasta que cocina confirme" || falla "cola"

titulo "F-05 · Validación de stock al confirmar"
req POST /pedidos "$MESERO" "{\"numeroMesa\":4,\"items\":[{\"productoId\":$BANDEJA,\"cantidad\":2}]}"; esperar 409
[[ $(campo .codigo) == INGREDIENTES_INSUFICIENTES ]] && ok "rechaza 2 bandejas paisa (solo hay chicharrón para 1)" || falla "stock"
req POST /pedidos "$MESERO" "{\"numeroMesa\":3,\"items\":[{\"productoId\":$LIMONADA,\"cantidad\":1}]}"; esperar 409
ok "no abre otro pedido en una mesa ocupada (RD-01)"

titulo "F-08 · F-09 · F-10 · Cocina"
req GET /pedidos/cocina "$COCINA"; esperar 200
[[ $(campo "map(select(.id==$PEDIDO)) | length") == 1 ]] && ok "la comanda aparece en cocina" || falla "cocina"
req POST /pedidos/cocina/ack "$COCINA" "{\"pedidoIds\":[$PEDIDO]}"; esperar 200; ok "ACK de recepción"
req PATCH "/pedidos/$PEDIDO/estado" "$COCINA" '{"nuevoEstado":"LISTO_PARA_SERVIR"}'; esperar 409
ok "rechaza el salto Registrado → Listo (RD-03)"
req PATCH "/pedidos/$PEDIDO/estado" "$COCINA" '{"nuevoEstado":"EN_PREPARACION"}'; esperar 200
req PATCH "/pedidos/$PEDIDO/estado" "$COCINA" '{"nuevoEstado":"LISTO_PARA_SERVIR"}'; esperar 200
ok "En preparación → Listo para servir"

titulo "F-11 · F-12 · F-14 · Mesero"
req GET /pedidos/notificaciones "$MESERO"; esperar 200
[[ $(campo "map(select(.pedidoId==$PEDIDO)) | length") == 1 ]] && ok "notificación de plato listo" || falla "notificación"
req POST "/pedidos/$PEDIDO/servido" "$MESERO" '{}'; esperar 200; ok "marcado como servido (Entregado)"
req POST "/pedidos/$PEDIDO/solicitar-cuenta" "$MESERO" '{}'; esperar 200; ok "cuenta solicitada a caja"

titulo "CU-02 · Cobro y factura electrónica"
req GET "/caja/cuentas/$PEDIDO" "$CAJA"; esperar 200
[[ $(campo .cuenta.cobrable) == true && $(campo .desglose.total) == 32000* ]] && ok "desglose: total \$32.000 con INC incluido" || falla "desglose"
req POST "/caja/cuentas/$PEDIDO/cobro" "$CAJA" '{"pagos":[{"metodo":"TARJETA","monto":32000,"ultimosDigitos":"0000"}]}'; esperar 402
ok "tarjeta rechazada por el banco (curso alterno 5a)"
req POST "/caja/cuentas/$PEDIDO/cobro" "$CAJA" '{"pagos":[{"metodo":"EFECTIVO","monto":12000,"montoRecibido":20000},{"metodo":"TARJETA","monto":20000,"ultimosDigitos":"4242"}]}'
esperar 201
[[ $(campo '.cufe | length') == 96 && $(campo .estadoDian) == ACEPTADA ]] && ok "factura $(campo .numero) validada por la DIAN (pago dividido)" || falla "factura"
req GET /mesas "$MESERO"; esperar 200
[[ $(campo '.[] | select(.numero==3) | .estado') == DISPONIBLE ]] && ok "la mesa 3 quedó libre" || falla "mesa"

titulo "F-20 · Cierre de caja"
req GET /caja/resumen-dia "$ADMIN"; esperar 200
[[ $(campo .resumen.numeroFacturas) -ge 1 ]] && ok "ventas del día: \$$(campo .resumen.totalVentas)" || falla "resumen"
req POST /caja/cierre "$CAJA" '{"confirmarInconsistencias":true}'; esperar 403; ok "solo el administrador cierra caja (RF-10)"
req POST /caja/cierre "$ADMIN" '{"confirmarInconsistencias":true,"observacion":"Prueba de humo"}'; esperar 201
ok "caja cerrada · comprobante DIAN $(campo .comprobanteDian)"
req POST /pedidos "$MESERO" "{\"numeroMesa\":5,\"items\":[{\"productoId\":$LIMONADA,\"cantidad\":1}]}"; esperar 409
[[ $(campo .codigo) == CAJA_CERRADA ]] && ok "con la caja cerrada no se aceptan pedidos (M-02)" || falla "caja cerrada"

echo; echo "✅ Prueba de humo superada"
