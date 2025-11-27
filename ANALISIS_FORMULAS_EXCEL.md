# Análisis de Fórmulas del Excel SUPER MASTER

## 📊 Hojas del Excel

1. **MASTER** - Hoja principal con todos los productos y cálculos
2. **TitulosWeb** - Títulos para web
3. **Impuestos** - Configuración de impuestos y gastos por canal
4. **COSTOS** - Costos de productos
5. **MLA-ENVIOS** - Configuración de envíos para MercadoLibre
6. **Validaciones** - Validaciones de clasificaciones

---

## 🔍 FÓRMULAS CLAVE PARA CÁLCULO DE PRECIOS

### 1. **PVP NUBE (Columna AD - Fila 3)**

```excel
=IFERROR(IF(MASTER[[#This Row],[TAG]]="MAQUINA",0,
  (((MASTER[[#This Row],[COSTO]]+MASTER[[#This Row],[COSTO]]*
    (IF(MASTER[[#This Row],[GAN.MIN.ML]]=0,"MARGEN",
      IF(MASTER[[#This Row],[TAG]]="MAQUINA",MASTER[[#This Row],[GAN.MIN.ML]]+0.2,
        MASTER[[#This Row],[GAN.MIN.ML]]+0.25))
  ))*MASTER[[#This Row],[IMP.]])/(1-GT3C))/(1-CUPON)
,"MARGEN")
```

**Lógica:**
1. Si TAG = "MAQUINA" → PVP = 0
2. Si GAN.MIN.ML = 0 → usar "MARGEN" (probablemente margenPorcentaje)
3. Si TAG = "MAQUINA" → GAN.MIN.ML + 0.2
4. Si no → GAN.MIN.ML + 0.25
5. **Cálculo:** `((COSTO + COSTO * ganancia) * IMP) / (1 - GT3C) / (1 - CUPON)`

**Donde:**
- `COSTO` = costo del producto
- `GAN.MIN.ML` = ganancia mínima (probablemente margenPorcentaje)
- `IMP` = factor de impuestos (probablemente 1 + IVA/100)
- `GT3C` = gastos totales para 3 cuotas (probablemente suma de conceptos de gasto)
- `CUPON` = descuento por cupón

---

### 2. **Cálculo de Márgenes (Columnas AG, AH, AI, AJ)**

#### AG - Margen 3 Cuotas:
```excel
=IFERROR((((MASTER[[#This Row],[PVP NUBE]]*(1-CUPON))*(1-GT3C))/(MASTER[[#This Row],[IMP.]])-MASTER[[#This Row],[COSTO]])/MASTER[[#This Row],[COSTO]],"MARGEN")
```

**Lógica inversa:**
- `((PVP * (1-CUPON) * (1-GT3C)) / IMP - COSTO) / COSTO`
- Esto calcula el margen porcentual a partir del PVP

#### AH - Margen 9 Cuotas:
```excel
=IFERROR((((MASTER[[#This Row],[PVP NUBE]]*(1-CUPON))*(1-GT9C))/(MASTER[[#This Row],[IMP.]])-MASTER[[#This Row],[COSTO]])/MASTER[[#This Row],[COSTO]],"MARGEN")
```

#### AI - Margen 12 Cuotas:
```excel
=IFERROR((((MASTER[[#This Row],[PVP NUBE]]*(1-CUPON))*(1-GT12C))/(MASTER[[#This Row],[IMP.]])-MASTER[[#This Row],[COSTO]])/MASTER[[#This Row],[COSTO]],"MARGEN")
```

#### AJ - Margen con descuento 15%:
```excel
=IFERROR((((MASTER[[#This Row],[PVP NUBE]]*(1-CUPON)*(1-0.15))*(1-(EMBALAJE+MARKETING+GASTONUBE)))/(MASTER[[#This Row],[IMP.]])-MASTER[[#This Row],[COSTO]])/MASTER[[#This Row],[COSTO]],"MARGEN")
```

---

### 3. **PVP ML (Columna X)**

```excel
=IFERROR(IF(MASTER[[#This Row],[GAN.MIN.ML]]>0,
  (((MASTER[[#This Row],[ML-COSTO+GAN]]+MASTER[[#This Row],[ENVIO]])*MASTER[[#This Row],[IMP.]])/(MASTER[[#This Row],[%CUOTAS]]))/0.9
,"MARGEN"),"")
```

**Lógica:**
- Si GAN.MIN.ML > 0:
  - `((ML-COSTO+GAN + ENVIO) * IMP) / %CUOTAS / 0.9`
- Si no → "MARGEN"

---

### 4. **PVP GASTRO (Columnas AL, AM, AN)**

```excel
=IFERROR(IF(MASTER[[#This Row],[TAG]]="MAQUINA",
  (((MASTER[[#This Row],[PVP GASTRO]])*(1-(MARKETING+GASTONUBE)))/(MASTER[[#This Row],[IMP.]])-MASTER[[#This Row],[COSTO]])/MASTER[[#This Row],[COSTO]],
  (((MASTER[[#This Row],[PVP GASTRO]])*(1-(EMBALAJE+MARKETING+GASTONUBE)))/(MASTER[[#This Row],[IMP.]])-MASTER[[#This Row],[COSTO]])/MASTER[[#This Row],[COSTO]])
,"MARGEN")
```

**Lógica:**
- Si TAG = "MAQUINA": no se aplica EMBALAJE
- Si no: se aplica EMBALAJE + MARKETING + GASTONUBE

---

## 📋 CONCEPTOS DE GASTO (Hoja Impuestos)

### Canales identificados:
- **NUBE**: M.P. (0.018), NUBE (0.01), CUPON, 3/6/9/12 CUOTAS
- **ML**: COMISION ML (0.14), CUOTA Promocionada (0.04), 3/6/9/12 CUOTAS
- **TODOS**: MARKETING (0.05), EMBALAJE (0.015), IIBB (0.05)
- **GASTRO**: Relacion con NUBE (0.1)

### Gastos Totales por Cuotas (NUBE):
- **3 CUOTAS**: `=$C$4+$C$17+C7` (M.P. + MARKETING + 3 CUOTAS)
- **6 CUOTAS**: `=$C$4+$C$17+C8` (M.P. + MARKETING + 6 CUOTAS)
- **9 CUOTAS**: `=$C$4+$C$17+C9` (M.P. + MARKETING + 9 CUOTAS)
- **12 CUOTAS**: `=$C$4+$C$17+C10` (M.P. + MARKETING + 12 CUOTAS)

### Gastos Totales NUBE MENAJE:
- **3 CUOTAS**: `=$C$18+$C$17+$C$4+$C$5+C7` (EMBALAJE + MARKETING + M.P. + NUBE + 3 CUOTAS)
- Similar para 6, 9, 12 cuotas

---

## 🔑 VARIABLES Y CONSTANTES IDENTIFICADAS

### Variables del Producto:
- `COSTO` - Costo del producto
- `IVA` - Porcentaje de IVA
- `IMP` - Factor de impuestos (probablemente 1 + IVA/100)
- `GAN.MIN.ML` - Ganancia mínima (margenPorcentaje)
- `TAG` - Etiqueta del producto (ej: "MAQUINA")
- `ENVIO` - Costo de envío
- `%CUOTAS` - Porcentaje según cuotas

### Variables de Canal:
- `GT3C`, `GT9C`, `GT12C` - Gastos totales para 3, 9, 12 cuotas
- `CUPON` - Descuento por cupón
- `EMBALAJE` - 0.015
- `MARKETING` - 0.05
- `GASTONUBE` - Variable según canal
- `KTG_NUBE`, `KTG_VOL`, `KTG_VOL2` - Descuentos adicionales

---

## 🎯 LÓGICA DE CÁLCULO CORREGIDA

### Fórmula General para PVP:

```
1. Calcular costo con ganancia:
   costoConGanancia = COSTO * (1 + margenPorcentaje/100)

2. Aplicar impuestos:
   costoConImpuestos = costoConGanancia * (1 + IVA/100)

3. Aplicar gastos (que se aplican sobre diferentes bases):
   - Gastos sobre COSTO: se suman al costo
   - Gastos sobre PVP: se calculan después
   - Gastos sobre COSTO_IVA: se aplican sobre costoConImpuestos
   - Gastos sobre COSTO_MARGEN: se aplican sobre costoConGanancia

4. Calcular PVP base:
   PVP_base = costoConImpuestos / (1 - gastosTotalesSobrePVP)

5. Aplicar descuentos:
   PVP_final = PVP_base * (1 - CUPON) * (1 - otrosDescuentos)
```

---

## ⚠️ PROBLEMAS IDENTIFICADOS EN EL CÓDIGO ACTUAL

1. **No se considera el campo `aplicaSobre`** - Los gastos se suman todos igual
2. **No se aplica el IVA correctamente** - El IVA debería multiplicar, no solo sumarse
3. **No se consideran los márgenes adicionales** (fijo, promoción, oferta)
4. **La fórmula de PVP está invertida** - Debería ser: `PVP = (costo + gastos) / (1 - margen%)` pero considerando que algunos gastos se aplican sobre el PVP

---

## 📝 PRÓXIMOS PASOS

1. Implementar cálculo considerando `aplicaSobre`
2. Aplicar IVA correctamente
3. Considerar márgenes adicionales
4. Implementar cálculo iterativo para gastos que se aplican sobre PVP


