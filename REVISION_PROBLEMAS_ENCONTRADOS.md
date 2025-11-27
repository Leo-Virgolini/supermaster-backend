# 🔍 Revisión Completa: Problemas Encontrados

**Fecha:** 2025-11-26  
**Alcance:** Entities, Mappers, DTOs, Controllers

---

## ❌ PROBLEMAS CRÍTICOS (Deben corregirse)

### 1. **Mappers con Clases Sin Importar en Expresiones**

Los siguientes mappers usan clases en expresiones `expression = "java(...)"` sin importarlas. MapStruct necesita los imports explícitos o usar nombres completos.

#### **ProductoMapper.java**
- ❌ Usa: `Marca`, `Origen`, `ClasifGral`, `ClasifGastro`, `Tipo`, `Proveedor`, `Material`
- **Líneas afectadas:** 29-35, 45-51
- **Solución:** Agregar imports o usar nombres completos

#### **ProductoCanalMapper.java**
- ❌ Usa: `Producto`, `Canal`
- **Líneas afectadas:** 16-17
- **Solución:** Agregar imports

#### **ProductoClienteMapper.java**
- ❌ Usa: `Producto`, `Cliente`
- **Líneas afectadas:** 18-19
- **Solución:** Agregar imports

#### **ProductoCatalogoMapper.java**
- ❌ Usa: `Producto`, `Catalogo`
- **Líneas afectadas:** 18-19
- **Solución:** Agregar imports

#### **ProductoCanalPrecioMapper.java**
- ❌ Usa: `Producto`, `Canal`
- **Líneas afectadas:** 16-17
- **Solución:** Agregar imports

#### **ProductoAptoMapper.java**
- ❌ Usa: `Producto`, `Apto`
- **Líneas afectadas:** 18-19
- **Solución:** Agregar imports

#### **CanalConceptoMapper.java**
- ❌ Usa: `Canal`, `ConceptoGasto`
- **Líneas afectadas:** 24-25
- **Solución:** Agregar imports

#### **ReglaDescuentoMapper.java**
- ❌ Usa: `Canal`, `Catalogo`, `ClasifGral`, `ClasifGastro`
- **Líneas afectadas:** 28-31, 41-54
- **Solución:** Agregar imports

#### **MlaMapper.java**
- ❌ Usa: `Producto`
- **Líneas afectadas:** 18
- **Solución:** Agregar imports

---

### 2. **AptoController y AptoService - Uso Incorrecto de DTOs**

**Problema:** `AptoController` y `AptoService` usan `AptoDTO` para crear y actualizar, cuando deberían usar `AptoCreateDTO` y `AptoUpdateDTO` como el resto de los controllers.

**Archivos afectados:**
- `AptoController.java` - líneas 29, 34-38
- `AptoService.java` - líneas 12, 14
- `AptoServiceImpl.java` - líneas 35-49

**Solución:** 
- Crear `AptoCreateDTO` y `AptoUpdateDTO`
- Actualizar `AptoMapper` para incluir métodos de conversión
- Actualizar `AptoService` y `AptoServiceImpl`
- Actualizar `AptoController`

---

### 3. **Inconsistencia en ProductoCreateDTO**

**Problema:** `ProductoCreateDTO` tiene `@NotNull Integer marcaId` (línea 15), pero en la entidad `Producto`, el campo `marca` es nullable (`@ManyToOne` sin `optional = false`).

**Solución:** 
- Opción 1: Remover `@NotNull` de `marcaId` en `ProductoCreateDTO`
- Opción 2: Hacer `marca` obligatorio en la entidad (agregar `optional = false`)

**Recomendación:** Verificar regla de negocio. Si todos los productos deben tener marca, hacer obligatorio en ambos lados.

---

### 4. **ReglaDescuentoUpdateDTO - Falta canalId**

**Problema:** `ReglaDescuentoUpdateDTO` no incluye `canalId`, pero `ReglaDescuentoCreateDTO` sí lo tiene.

**Análisis:** 
- Puede ser intencional (no permitir cambiar el canal de una regla existente)
- O puede ser un error

**Solución:** 
- Si es intencional: OK, pero documentar
- Si no es intencional: Agregar `canalId` a `ReglaDescuentoUpdateDTO` y actualizar el mapper

---

## ⚠️ PROBLEMAS MENORES (Mejoras recomendadas)

### 5. **Mappers con Expresiones Complejas para Relaciones Jerárquicas**

En `TipoMapper`, `MarcaMapper`, `ClasifGralMapper`, `ClasifGastroMapper`, `CanalMapper`:

**Problema:** En `updateEntityFromDTO`, la expresión para `padre` es:
```java
expression = "java(dto.padreId() != null ? new Tipo(dto.padreId()) : entity.getPadre())"
```

**Análisis:** 
- Si `padreId` es `null` en el DTO, mantiene el padre existente (correcto)
- Pero si quieres **eliminar** la relación padre (poner null), no puedes hacerlo enviando null explícitamente

**Solución opcional:** 
- Usar `Optional<Integer>` en DTOs para distinguir entre "no cambiar" y "poner null"
- O aceptar que no se puede eliminar la relación padre vía update

---

### 6. **Falta Validación en DTOs**

Varios DTOs de creación no tienen validaciones `@NotNull` o `@Size`:

- `AptoDTO` (si se crea CreateDTO, agregar validaciones)
- `MaterialCreateDTO` - falta `@NotNull` en `material`
- `OrigenCreateDTO` - falta `@NotNull` en `origen`
- `TipoCreateDTO` - falta `@NotNull` en `nombre`
- `MarcaCreateDTO` - falta `@NotNull` en `nombre`
- `ClasifGralCreateDTO` - falta `@NotNull` en `nombre`
- `ClasifGastroCreateDTO` - falta `@NotNull` en `nombre`
- `CatalogoCreateDTO` - falta `@NotNull` en `catalogo`
- `ClienteCreateDTO` - falta `@NotNull` en `cliente`
- `ProveedorCreateDTO` - falta `@NotNull` en `proveedor` y `apodo`
- `ImpuestoCreateDTO` - falta `@NotNull` en campos
- `ConceptoGastoCreateDTO` - falta `@NotNull` en `concepto` y `porcentaje`

---

### 7. **Falta @Valid en Controllers**

Los controllers no usan `@Valid` en los parámetros `@RequestBody`, por lo que las validaciones de los DTOs no se ejecutan.

**Ejemplo:**
```java
@PostMapping
public ResponseEntity<OrigenDTO> crear(@RequestBody OrigenCreateDTO dto) {
```

**Debería ser:**
```java
@PostMapping
public ResponseEntity<OrigenDTO> crear(@Valid @RequestBody OrigenCreateDTO dto) {
```

---

### 8. **Manejo de Errores Inconsistente**

Algunos servicios usan `RuntimeException` genérico en lugar de excepciones específicas:

- `AptoServiceImpl` - línea 22, 44
- Otros servicios probablemente también

**Solución:** Usar `NotFoundException` del paquete `common.exception`

---

### 9. **ProductoCanalDTO - Falta ID**

`ProductoCanalDTO` no incluye el `id` de la entidad, solo `productoId` y `canalId`. Esto puede ser problemático para actualizaciones.

**Análisis:** 
- La tabla tiene un `id` autoincremental además de la relación producto-canal
- El DTO debería incluir el `id` si se necesita para operaciones

---

## ✅ ASPECTOS CORRECTOS

1. ✅ Uso correcto de `@EmbeddedId` en tablas de relación
2. ✅ Uso correcto de `@MapsId` en relaciones con `@EmbeddedId`
3. ✅ Uso correcto de `FetchType.LAZY` en relaciones
4. ✅ Uso correcto de `@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)` para updates
5. ✅ Estructura de DTOs (Create, Update, DTO base) bien implementada en la mayoría
6. ✅ Uso correcto de MapStruct con `componentModel = "spring"`
7. ✅ Timestamps con `@PrePersist` y `@PreUpdate` en Producto
8. ✅ Relaciones jerárquicas bien implementadas (padre-hijo)

---

## 📋 RESUMEN DE CORRECCIONES NECESARIAS

### **Prioridad ALTA (Crítico - No compila o causa errores):**

1. ✅ Agregar imports en todos los mappers que usan expresiones
2. ✅ Corregir AptoController/AptoService para usar CreateDTO y UpdateDTO
3. ✅ Agregar `@Valid` en todos los controllers

### **Prioridad MEDIA (Funciona pero mejorable):**

4. ✅ Agregar validaciones `@NotNull` en DTOs de creación
5. ✅ Usar `NotFoundException` en lugar de `RuntimeException`
6. ✅ Revisar inconsistencia de `marcaId` en ProductoCreateDTO
7. ✅ Decidir si `ReglaDescuentoUpdateDTO` debe tener `canalId`

### **Prioridad BAJA (Opcional):**

8. ⚠️ Considerar incluir `id` en `ProductoCanalDTO`
9. ⚠️ Mejorar manejo de relaciones padre null en updates

---

¿Quieres que proceda a corregir estos problemas?

