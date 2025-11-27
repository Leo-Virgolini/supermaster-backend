# Revisión del Módulo de Cálculo de Precios

## 📋 Archivos Revisados

1. `CalculoPrecioController.java` ✅
2. `CalculoPrecioService.java` ✅
3. `CalculoPrecioServiceImpl.java` ❌ **PROBLEMAS ENCONTRADOS**
4. `PrecioCalculadoDTO.java` ✅

---

## ❌ PROBLEMAS CRÍTICOS ENCONTRADOS

### 1. **NO SE CONSIDERA EL CAMPO `aplicaSobre` DE LOS CONCEPTOS DE GASTO**

**Problema:**
El método `calcularGastosPorcentaje()` solo suma todos los porcentajes sin considerar sobre qué base se aplica cada concepto de gasto.

**Código actual (líneas 139-144):**
```java
private BigDecimal calcularGastosPorcentaje(List<CanalConcepto> conceptos) {
    return conceptos.stream()
            .map(cc -> cc.getConcepto().getPorcentaje())
            .filter(p -> p != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

**Problema:**
- Los conceptos de gasto tienen un campo `aplicaSobre` que puede ser:
  - `COSTO`: se aplica sobre el costo base
  - `PVP`: se aplica sobre el precio de venta (PVP)
  - `COSTO_IVA`: se aplica sobre el costo + IVA
  - `COSTO_MARGEN`: se aplica sobre el costo + margen

- Actualmente, todos los porcentajes se suman y se aplican sobre el costo, lo cual es **INCORRECTO**.

**Impacto:** ⚠️ **CRÍTICO** - Los cálculos de precios están incorrectos cuando hay conceptos de gasto con diferentes valores de `aplicaSobre`.

---

### 2. **NO SE CONSIDERA EL IVA DEL PRODUCTO**

**Problema:**
El producto tiene un campo `iva` (porcentaje de IVA) que no se está utilizando en el cálculo.

**Código actual:**
- Solo se usa `producto.getCosto()` (línea 88)
- No se calcula `costoConIva = costo * (1 + iva/100)`

**Impacto:** ⚠️ **IMPORTANTE** - El IVA debería considerarse en el cálculo del costo total, especialmente para conceptos que se aplican sobre `COSTO_IVA`.

---

### 3. **NO SE CONSIDERA EL MARGEN FIJO**

**Problema:**
`ProductoCanal` tiene los siguientes campos que no se están usando:
- `margenFijo` (BigDecimal)
- `margenPromocion` (BigDecimal)
- `margenOferta` (BigDecimal)

**Código actual:**
- Solo se usa `margenPorcentaje` (línea 94)
- Los otros márgenes se ignoran completamente

**Impacto:** ⚠️ **IMPORTANTE** - Si hay márgenes fijos, promociones u ofertas, no se están aplicando.

---

### 4. **LÓGICA DE CÁLCULO INCOMPLETA**

**Problema:**
La fórmula actual asume que:
1. Todos los gastos se aplican sobre el costo
2. El margen se aplica sobre el costo total (con gastos)

Pero según el campo `aplicaSobre`, algunos gastos pueden aplicarse sobre:
- El PVP (que aún no está calculado)
- El COSTO_IVA (que requiere calcular el IVA primero)
- El COSTO_MARGEN (que requiere calcular el margen primero)

**Esto crea una dependencia circular o requiere un cálculo iterativo.**

---

## 🔧 CORRECCIONES NECESARIAS

### Corrección 1: Implementar cálculo considerando `aplicaSobre`

La lógica debería ser:

1. **Calcular costo base**
   ```java
   BigDecimal costo = producto.getCosto();
   ```

2. **Calcular costo con IVA** (si es necesario)
   ```java
   BigDecimal costoConIva = costo.multiply(BigDecimal.ONE.add(ivaFrac));
   ```

3. **Calcular gastos que se aplican sobre COSTO**
   ```java
   BigDecimal gastosSobreCosto = calcularGastosPorConcepto(conceptos, AplicaSobre.COSTO, costo);
   ```

4. **Calcular costo total inicial** (costo + gastos sobre costo)
   ```java
   BigDecimal costoTotalInicial = costo.add(gastosSobreCosto);
   ```

5. **Calcular PVP inicial** (para gastos que se aplican sobre PVP)
   ```java
   BigDecimal pvpInicial = calcularPVPConMargen(costoTotalInicial, margenPorcentaje);
   ```

6. **Calcular gastos que se aplican sobre PVP**
   ```java
   BigDecimal gastosSobrePVP = calcularGastosPorConcepto(conceptos, AplicaSobre.PVP, pvpInicial);
   ```

7. **Recalcular PVP final** (ajustando por gastos sobre PVP)
   ```java
   BigDecimal pvpFinal = pvpInicial.add(gastosSobrePVP);
   ```

8. **Aplicar margen fijo, promoción u oferta** (si existen)

### Corrección 2: Considerar IVA del producto

```java
BigDecimal iva = producto.getIva();
if (iva == null) {
    iva = BigDecimal.ZERO;
}
BigDecimal ivaFrac = iva.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
BigDecimal costoConIva = costo.multiply(BigDecimal.ONE.add(ivaFrac));
```

### Corrección 3: Considerar márgenes adicionales

```java
// Aplicar margen fijo
if (productoCanal.getMargenFijo() != null && productoCanal.getMargenFijo().compareTo(BigDecimal.ZERO) > 0) {
    pvp = pvp.add(productoCanal.getMargenFijo());
}

// Aplicar margen promoción (si existe)
if (productoCanal.getMargenPromocion() != null && productoCanal.getMargenPromocion().compareTo(BigDecimal.ZERO) > 0) {
    // Lógica para aplicar promoción
}

// Aplicar margen oferta (si existe)
if (productoCanal.getMargenOferta() != null && productoCanal.getMargenOferta().compareTo(BigDecimal.ZERO) > 0) {
    // Lógica para aplicar oferta
}
```

---

## 📝 NOTAS ADICIONALES

### Preguntas que requieren aclaración:

1. **¿Cómo se deben aplicar los márgenes adicionales?**
   - ¿El `margenFijo` se suma al PVP?
   - ¿El `margenPromocion` y `margenOferta` reemplazan al `margenPorcentaje` o se suman?

2. **¿Cuál es la prioridad cuando hay múltiples márgenes?**
   - ¿Se aplican todos o solo uno?

3. **¿Cómo se manejan los gastos que se aplican sobre PVP?**
   - ¿Requieren un cálculo iterativo?
   - ¿O se calcula un PVP inicial y luego se ajusta?

4. **¿El IVA se aplica siempre o solo en ciertos casos?**
   - ¿Se aplica antes o después de los gastos?

---

## ✅ ASPECTOS CORRECTOS

1. ✅ Validación de costo nulo
2. ✅ Validación de margen >= 100%
3. ✅ Uso de `RoundingMode.HALF_UP` para redondeo
4. ✅ Escala de 2 decimales para montos
5. ✅ Escala de 6 decimales para cálculos intermedios
6. ✅ Estructura del DTO es correcta
7. ✅ Controller está bien implementado

---

## 🎯 RECOMENDACIÓN

**Se requiere una revisión completa de la lógica de cálculo** para implementar correctamente:

1. El cálculo considerando `aplicaSobre`
2. La consideración del IVA
3. La aplicación de márgenes adicionales

**Sugerencia:** Consultar con el equipo de negocio o revisar el Excel original para entender la lógica exacta de cálculo.


