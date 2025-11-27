# Implementación de Cálculo de Precios Corregido

## ✅ Cambios Implementados

### 1. **Consideración del campo `aplicaSobre`**

Los conceptos de gasto ahora se separan y aplican según su base:

- **COSTO**: Se aplican sobre el costo base (se suman al costo)
- **COSTO_MARGEN**: Se aplican sobre el costo con ganancia
- **COSTO_IVA**: Se aplican sobre el costo con impuestos (IVA)
- **PVP**: Se aplican sobre el precio de venta (se usan en el denominador)

### 2. **Aplicación correcta del IVA**

El IVA ahora se aplica como factor multiplicador:
```
IMP = 1 + IVA/100
costoConImpuestos = costoConGanancia * IMP
```

### 3. **Consideración de márgenes adicionales**

Se consideran los siguientes márgenes adicionales:
- `margenFijo`: Se suma directamente al PVP base
- `margenPromocion`: Se aplica como porcentaje sobre el PVP base
- `margenOferta`: Se aplica como porcentaje sobre el PVP base

### 4. **Lógica de cálculo corregida**

La nueva lógica sigue este orden:

1. **Costo base** = `costo`
2. **Aplicar gastos sobre COSTO** → `costoConGastos`
3. **Aplicar margen porcentual** → `costoConGanancia`
4. **Aplicar gastos sobre COSTO_MARGEN** → `costoConGanancia` (actualizado)
5. **Aplicar IVA** → `costoConImpuestos = costoConGanancia * (1 + IVA/100)`
6. **Aplicar gastos sobre COSTO_IVA** → `costoConImpuestos` (actualizado)
7. **Calcular PVP base** → `PVP = costoConImpuestos / (1 - gastosSobrePVP)`
8. **Aplicar márgenes adicionales** (fijo, promoción, oferta)

## 📋 Fórmula Final

```
PVP = ((((costo * (1 + gastosSobreCosto%) * (1 + margen%) * (1 + gastosSobreCostoMargen%) * (1 + IVA/100) * (1 + gastosSobreCostoIva%)) / (1 - gastosSobrePVP%)) + margenFijo) * (1 + margenPromocion%) * (1 + margenOferta%)
```

## ⚠️ Notas Importantes

### 1. **Filtrado por Cuotas**

Actualmente, **NO se filtra por cuotas**. En el Excel, los conceptos de gasto tienen un campo `cuotas` (3, 6, 9, 12) que indica para qué cuotas aplica. 

**Para implementar esto en el futuro:**
- Agregar un parámetro `cuotas` al método `calcularPrecioCanal()`
- Filtrar los conceptos según el campo `cuotas` antes de calcular

### 2. **Lógica Especial para TAG=MAQUINA**

En el Excel, hay lógica especial para productos con `TAG="MAQUINA"`:
- No se aplica EMBALAJE
- La ganancia se ajusta: `GAN.MIN.ML + 0.2` o `+ 0.25`

**Para implementar esto:**
- Determinar cómo se identifica un producto como "MAQUINA" en la BD (probablemente por `tipo` o `clasifGral`)
- Agregar lógica condicional para excluir conceptos de EMBALAJE cuando sea MAQUINA
- Ajustar el margen según la regla

### 3. **Descuentos (CUPON)**

En el Excel, hay descuentos por cupón que se aplican al final:
```
PVP_final = PVP_base / (1 - CUPON)
```

**Para implementar esto:**
- Agregar un campo de descuento/cupón al `ProductoCanal` o como parámetro
- Aplicar el descuento al final del cálculo

### 4. **Cálculo Iterativo para Gastos sobre PVP**

Actualmente, los gastos sobre PVP se calculan de forma directa usando la fórmula:
```
PVP = costoConImpuestos / (1 - gastosSobrePVP%)
```

Esto es correcto cuando los gastos sobre PVP no dependen de otros gastos sobre PVP. Si hay dependencias circulares, sería necesario un cálculo iterativo.

## 🔄 Comparación con Excel

### Fórmula del Excel (PVP NUBE):
```
PVP = ((COSTO + COSTO * ganancia) * IMP) / (1 - GT3C) / (1 - CUPON)
```

### Fórmula Implementada:
```
PVP = (((costo * (1 + gastosSobreCosto%) * (1 + margen%) * (1 + gastosSobreCostoMargen%) * (1 + IVA/100) * (1 + gastosSobreCostoIva%)) / (1 - gastosSobrePVP%)) + margenFijo) * (1 + margenPromocion%) * (1 + margenOferta%)
```

**Diferencias:**
1. ✅ La implementación es más flexible y considera todos los tipos de `aplicaSobre`
2. ⚠️ Falta implementar el filtrado por cuotas (GT3C, GT9C, etc.)
3. ⚠️ Falta implementar el descuento CUPON
4. ⚠️ Falta implementar la lógica especial para TAG=MAQUINA

## 📝 Próximos Pasos Sugeridos

1. **Agregar parámetro de cuotas** al método de cálculo
2. **Implementar lógica para TAG=MAQUINA** (determinar cómo identificar productos MAQUINA)
3. **Agregar campo de descuento/cupón** al cálculo
4. **Agregar tests unitarios** para validar los cálculos
5. **Documentar ejemplos** de cálculo con diferentes escenarios

