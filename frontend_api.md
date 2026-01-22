# API Documentation for Frontend (Next.js)

Este documento describe los endpoints de la API REST para el desarrollo del frontend.

**Base URL:** `http://localhost:8080/api`

---

## Tabla de Contenidos

1. [Visión General del Sistema](#visión-general-del-sistema)
2. [Modelo de Datos](#modelo-de-datos)
3. [Convenciones Generales](#convenciones-generales)
4. [Tipos TypeScript](#tipos-typescript)
5. [Endpoints por Módulo](#endpoints-por-módulo)
   - [Productos](#productos)
   - [Precios](#precios)
   - [Canales](#canales)
   - [Conceptos de Gasto](#conceptos-de-gasto)
   - [Reglas de Descuento](#reglas-de-descuento)
   - [Promociones](#promociones)
   - [Catálogos y Clientes](#catálogos-y-clientes)
   - [Atributos Maestros](#atributos-maestros)
   - [Excel](#excel-importexport)

---

## Visión General del Sistema

Este backend gestiona un sistema de **cálculo de precios multicanal** para productos. La idea central es:

1. **Productos** tienen un costo base e IVA
2. **Canales** representan diferentes puntos de venta (ej: Mercado Libre, Tienda Nube, Mayorista, etc.)
3. Cada canal tiene **conceptos de gasto** asociados que afectan el cálculo del precio
4. Los precios se calculan automáticamente aplicando márgenes, impuestos, comisiones y descuentos
5. Cada canal puede tener diferentes **opciones de cuotas** con recargos o descuentos

### Flujo de Cálculo de Precios

```
┌─────────────┐     ┌─────────────┐     ┌──────────────────┐
│  PRODUCTO   │     │   CANAL     │     │ PRECIO CALCULADO │
│  - costo    │ ──► │  - conceptos│ ──► │  - pvp           │
│  - iva      │     │  - cuotas   │     │  - ganancia      │
│  - margen   │     │  - reglas   │     │  - margen %      │
└─────────────┘     └─────────────┘     └──────────────────┘
```

**Fórmula simplificada:**
```
PVP = (COSTO × (1 + MARGEN/100) × FACTOR_IMP) / (1 - COMISIONES/100)
```

Donde los conceptos de gasto modifican diferentes partes de la fórmula según su `aplicaSobre`.

---

## Modelo de Datos

### Diagrama de Relaciones

```
                                    ┌─────────────┐
                                    │   MARCA     │
                                    └──────┬──────┘
                                           │
┌─────────────┐    ┌─────────────┐    ┌────▼────────┐    ┌─────────────┐
│  PROVEEDOR  │◄───┤  PRODUCTO   ├───►│    TIPO     │    │   ORIGEN    │
│  -porcentaje│    │  -sku       │    └─────────────┘    └──────▲──────┘
└─────────────┘    │  -costo     │                              │
                   │  -iva       │◄─────────────────────────────┘
                   └──────┬──────┘
                          │
          ┌───────────────┼───────────────┐
          │               │               │
          ▼               ▼               ▼
   ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
   │  CATALOGO   │ │   CLIENTE   │ │    APTO     │
   │  (N:M)      │ │   (N:M)     │ │   (N:M)     │
   └─────────────┘ └─────────────┘ └─────────────┘

                   ┌─────────────┐
                   │   PRODUCTO  │
                   │   MARGEN    │
                   │ -minorista  │
                   │ -mayorista  │
                   └──────┬──────┘
                          │
                          ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   CANAL     │◄───┤  PRODUCTO   │    │    MLA      │
│  -nombre    │    │   CANAL     │◄───┤  -mla       │
│  -canalBase │    │   PRECIO    │    │  -precioEnvio│
└──────┬──────┘    │  -pvp       │    └─────────────┘
       │           │  -ganancia  │
       │           └─────────────┘
       │
       ├────────────────┬────────────────┐
       ▼                ▼                ▼
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│   CANAL     │  │   CANAL     │  │   REGLA     │
│  CONCEPTO   │  │  CONCEPTO   │  │  DESCUENTO  │
│  (N:M)      │  │   CUOTA     │  │  -monto     │
└──────┬──────┘  │  -cuotas    │  │  -descuento │
       │         │  -porcentaje│  └─────────────┘
       ▼         └─────────────┘
┌─────────────┐
│  CONCEPTO   │
│   GASTO     │
│ -porcentaje │
│ -aplicaSobre│
└─────────────┘
```

### Tablas Principales

#### 1. `productos` - Catálogo de Productos
| Campo | Descripción |
|-------|-------------|
| `sku` | Código único del producto |
| `descripcion` | Nombre interno |
| `titulo_web` | Nombre para mostrar en web |
| `costo` | Precio de compra/costo base |
| `iva` | Porcentaje de IVA (ej: 21) |
| `es_combo` | Si es un pack/combo |
| `stock` | Cantidad disponible |
| `activo` | Si está activo para venta |

**Relaciones:** marca, tipo, origen, material, proveedor, clasificaciones (gral y gastro), MLA.

#### 2. `producto_margen` - Márgenes por Producto
| Campo | Descripción |
|-------|-------------|
| `margen_minorista` | % ganancia para canales minoristas |
| `margen_mayorista` | % ganancia para canales mayoristas |
| `margen_fijo_*` | Ganancia fija en $ (opcional) |

Cada producto puede tener UN registro de márgenes. El canal determina cuál usar según tenga el concepto `FLAG_USAR_MARGEN_MINORISTA` o `FLAG_USAR_MARGEN_MAYORISTA`.

#### 3. `canales` - Canales de Venta
| Campo | Descripción |
|-------|-------------|
| `canal` | Nombre del canal (ej: "ML", "KT GASTRO") |
| `canal_base_id` | Canal padre para calcular sobre su PVP |

**Ejemplos de canales:**
- ML (Mercado Libre)
- KT HOGAR (Tienda Nube Hogar)
- KT GASTRO (Tienda Nube Gastronómico)
- MAYORISTA

**Canales con canal base (herencia de precios):**

Cuando un canal tiene `canal_base_id` configurado y el concepto `CALCULO_SOBRE_CANAL_BASE` asignado, el precio se calcula en base al PVP del canal padre en lugar de usar el costo del producto.

```
Canal hijo (KT GASTRO)          Canal padre (ML)
      │                              │
      │  ◄─── toma PVP ────────────  │
      │                              │
      ▼                              ▼
PVP_HIJO = PVP_PADRE × (1 + CALCULO_SOBRE_CANAL_BASE% / 100)
```

**Ejemplo:**
- Canal "ML" (padre): PVP = $10,000
- Canal "KT GASTRO" (hijo): tiene `canal_base_id = ML` y concepto `CALCULO_SOBRE_CANAL_BASE` con porcentaje -12%
- Resultado: PVP de KT GASTRO = $10,000 × (1 - 12/100) = $8,800

**Notas importantes:**
- El canal padre debe calcularse primero (el sistema lo hace automáticamente en el orden correcto)
- Si `CALCULO_SOBRE_CANAL_BASE` tiene porcentaje positivo, incrementa el precio; si es negativo, lo decrementa
- Esto es útil para canales que derivan su precio de otro (ej: tienda propia vs marketplace)

#### 4. `conceptos_gastos` - Conceptos de Costo/Gasto
| Campo | Descripción |
|-------|-------------|
| `concepto` | Nombre (ej: "GTML", "IIBB", "IVA") |
| `porcentaje` | Valor del concepto |
| `aplica_sobre` | Cómo se aplica en la fórmula |

**Valores de `aplica_sobre`:**

| Valor | Descripción | Ejemplo |
|-------|-------------|---------|
| `GASTO_SOBRE_COSTO` | Gasto que multiplica costo base | Embalaje +2% |
| `FLAG_FINANCIACION_PROVEEDOR` | Flag: usa % financiación del proveedor | |
| `AJUSTE_MARGEN_PUNTOS` | Suma/resta puntos al margen | +5 puntos al margen |
| `AJUSTE_MARGEN_PROPORCIONAL` | Modifica margen proporcionalmente | -12% del margen |
| `FLAG_USAR_MARGEN_MINORISTA` | Flag: usar margen minorista | |
| `FLAG_USAR_MARGEN_MAYORISTA` | Flag: usar margen mayorista | |
| `GASTO_POST_GANANCIA` | Gasto después de ganancia, antes de IMP | |
| `FLAG_APLICAR_IVA` | Flag: aplicar IVA del producto | |
| `IMPUESTO_ADICIONAL` | Se suma a factor de impuestos | IIBB +3.5% |
| `GASTO_POST_IMPUESTOS` | Gasto después de aplicar impuestos | |
| `FLAG_INCLUIR_ENVIO` | Flag: incluir precio envío del MLA | Envío gratis |
| `COMISION_SOBRE_PVP` | Comisión como divisor sobre PVP | Comisión ML -13% |
| `CALCULO_SOBRE_CANAL_BASE` | Calcula sobre PVP del canal base | |
| `RECARGO_CUPON` | Divisor adicional sobre PVP | Cupones +5% |
| `DESCUENTO_PORCENTUAL` | Descuento final sobre PVP | Promo -10% |
| `INFLACION_DIVISOR` | Divisor de inflación | Inflación +8% |
| `FLAG_APLICAR_PROMOCIONES` | Flag: aplicar promociones | |

**Detalle de cómo se aplican los conceptos:**

```
FÓRMULA GENERAL SIMPLIFICADA:

PVP = (COSTO_AJUSTADO × (1 + MARGEN/100) × FACTOR_IMP) / (1 - COMISIONES_PVP/100)

Donde:
- COSTO_AJUSTADO = costo × (1 + Σ GASTO_SOBRE_COSTO%) × (1 + FLAG_FINANCIACION_PROVEEDOR%)
- MARGEN = margen_base + Σ AJUSTE_MARGEN_PUNTOS ± (margen_base × AJUSTE_MARGEN_PROPORCIONAL%)
- FACTOR_IMP = (1 + FLAG_APLICAR_IVA%) × (1 + Σ IMPUESTO_ADICIONAL%)
- COMISIONES_PVP = Σ conceptos con COMISION_SOBRE_PVP
```

| Tipo | Efecto | Fórmula |
|------|--------|---------|
| `GASTO_SOBRE_COSTO` | Incrementa costo base | `costo × (1 + %/100)` |
| `COMISION_SOBRE_PVP` | Comisión sobre precio final | Divide: `PVP / (1 - %/100)` |
| `IMPUESTO_ADICIONAL` | Agrega al factor de impuestos | `factor_imp += %/100` |
| `AJUSTE_MARGEN_PUNTOS` | Suma/resta puntos al margen | `margen += %` |
| `AJUSTE_MARGEN_PROPORCIONAL` | Modifica margen proporcionalmente | `margen × (1 + %/100)` |
| `RECARGO_CUPON` | Divisor adicional | `PVP / (1 - %/100)` |
| `DESCUENTO_PORCENTUAL` | Descuento sobre PVP | `PVP × (1 - %/100)` |
| `INFLACION_DIVISOR` | Calcula PVP_INFLADO | `PVP / (1 - %/100)` |
| `FLAG_INCLUIR_ENVIO` | Agrega precio_envio del MLA | `PVP += mla.precio_envio` |

**Conceptos tipo FLAG (el % se ignora, solo importa si está asignado):**
- `FLAG_APLICAR_IVA`: Habilita aplicar el IVA del producto
- `FLAG_USAR_MARGEN_MINORISTA`: Usa el margen minorista del producto
- `FLAG_USAR_MARGEN_MAYORISTA`: Usa el margen mayorista del producto
- `FLAG_APLICAR_PROMOCIONES`: Habilita promociones asignadas al producto-canal
- `CALCULO_SOBRE_CANAL_BASE`: Calcula sobre PVP del canal padre (ver sección canales)
- `FLAG_FINANCIACION_PROVEEDOR`: Usa el % financiación del proveedor del producto

#### 5. `canal_concepto` - Conceptos Asignados a Canales
Tabla intermedia que relaciona qué conceptos aplican a cada canal.

**Ejemplo:**
- Canal "ML" tiene: GTML, IIBB, FLAG_APLICAR_IVA, FLAG_USAR_MARGEN_MINORISTA
- Canal "MAYORISTA" tiene: FLAG_APLICAR_IVA, FLAG_USAR_MARGEN_MAYORISTA

#### 6. `canal_concepto_cuota` - Opciones de Cuotas por Canal
| Campo | Descripción |
|-------|-------------|
| `cuotas` | -1=transferencia, 0=contado, >0=cuotas |
| `porcentaje` | Recargo (+) o descuento (-) |
| `descripcion` | Texto a mostrar |

**Ejemplo para canal ML:**
| cuotas | porcentaje | descripcion |
|--------|------------|-------------|
| -1 | -15 | Transferencia |
| 0 | 0 | Contado |
| 1 | 0 | 1 cuota sin interés |
| 3 | 10 | 3 cuotas |
| 6 | 20 | 6 cuotas |

#### 6.1. `canal_concepto_regla` - Reglas Condicionales de Conceptos
Permite incluir o excluir un concepto del cálculo según atributos del producto.

| Campo | Descripción |
|-------|-------------|
| `canal_id` | Canal donde aplica la regla |
| `concepto_id` | Concepto afectado por la regla |
| `tipo_regla` | INCLUIR o EXCLUIR |
| `tipo_id` | Filtro por tipo de producto (opcional) |
| `clasif_gastro_id` | Filtro por clasificación gastro (opcional) |
| `clasif_gral_id` | Filtro por clasificación general (opcional) |
| `marca_id` | Filtro por marca (opcional) |
| `es_maquina` | Filtro si es máquina (opcional) |

**Tipos de regla:**
| Tipo | Comportamiento |
|------|----------------|
| `INCLUIR` | El concepto SOLO aplica si el producto cumple la condición |
| `EXCLUIR` | El concepto NO aplica si el producto cumple la condición |

**Ejemplo 1 - EXCLUIR:**
```
Canal: ML
Concepto: EMBALAJE (aplica_sobre: GASTO_SOBRE_COSTO, 2%)
Regla: EXCLUIR si es_maquina = true
```
Resultado: Las máquinas no pagan embalaje en ML, los demás productos sí.

**Ejemplo 2 - INCLUIR:**
```
Canal: KT GASTRO
Concepto: DESC_GASTRO (aplica_sobre: DESCUENTO_PORCENTUAL, -5%)
Regla: INCLUIR si clasif_gastro_id = 3 (Cafeteras)
```
Resultado: Solo las cafeteras tienen el descuento gastro, los demás productos no.

**Nota:** Si un concepto tiene múltiples reglas, se evalúan todas. El concepto aplica si pasa todas las condiciones.

#### 7. `producto_canal_precios` - Precios Calculados
| Campo | Descripción |
|-------|-------------|
| `pvp` | Precio de venta al público (precio real de venta) |
| `pvp_inflado` | PVP con inflación (solo para mostrar tachado en UI) |
| `costo_producto` | Costo base × (1 + financiación proveedor) |
| `costos_venta` | Σ conceptos con AplicaSobre: COMISION_SOBRE_PVP, DESCUENTO_PORCENTUAL, RECARGO_CUPON, FLAG_INCLUIR_ENVIO (incluye embalaje, comisiones, cuotas) |
| `ingreso_neto_vendedor` | PVP - IVA - impuestos - costosVenta |
| `ganancia` | Ingreso neto - costo producto |
| `margen_sobre_ingreso_neto` | (ganancia / ingreso neto) × 100 - Rentabilidad real después de gastos |
| `margen_sobre_pvp` | (ganancia / pvp) × 100 - Margen tradicional sobre precio de venta |
| `markup_porcentaje` | (ganancia / costo) × 100 |

**Importante:** Todas las métricas (`costos_venta`, `ingreso_neto_vendedor`, `ganancia`, `margen_sobre_ingreso_neto`, `margen_sobre_pvp`, `markup_porcentaje`) se calculan sobre el **`pvp`**, NO sobre `pvp_inflado`. El campo `pvp_inflado` es solo para mostrar un precio "tachado" en la UI (ej: ~~$12,000~~ $10,000).

**Clave única:** (producto, canal, cuotas)

#### 8. `reglas_descuento` - Descuentos Automáticos
| Campo | Descripción |
|-------|-------------|
| `canal_id` | Canal donde aplica |
| `monto_minimo` | Monto mínimo para aplicar |
| `descuento_porcentaje` | % de descuento |
| `prioridad` | Orden de evaluación |
| `catalogo_id` | Filtro por catálogo (opcional) |
| `clasif_gral_id` | Filtro por clasificación (opcional) |

#### 9. `promociones` - Promociones Globales
| Campo | Descripción |
|-------|-------------|
| `codigo` | Código único |
| `tipo` | MULTIPLICADOR, DESCUENTO_PORC, DIVISOR, PRECIO_FIJO |
| `valor` | Valor según el tipo |

**Tipos de promoción y cómo afectan el PVP:**

| Tipo | Fórmula | Ejemplo | Resultado |
|------|---------|---------|-----------|
| `MULTIPLICADOR` | PVP × valor | PVP=$1000, valor=1.1 | $1,100 (+10%) |
| `DESCUENTO_PORC` | PVP / (1 - valor/100) | PVP=$1000, valor=30 | $1,428.57 (+42.8%) |
| `DIVISOR` | PVP / valor | PVP=$1000, valor=0.9 | $1,111.11 (+11.1%) |
| `PRECIO_FIJO` | valor | PVP=$1000, valor=500 | $500 (fijo) |

**Notas:**
- Las promociones se asignan a producto+canal específico mediante `producto_canal_promocion`
- Solo aplican si el canal tiene el concepto `PROMOCION` asignado
- `DESCUENTO_PORC` usa la fórmula del Excel original: divide por (1-porcentaje), lo que **incrementa** el precio

#### 10. `mlas` - Datos de Mercado Libre
| Campo | Descripción |
|-------|-------------|
| `mla` | Código MLA (ej: "MLA123456") |
| `mlau` | Código MLAU (variante) |
| `precio_envio` | Costo de envío para concepto ENVIO |

### Tablas de Clasificación (Maestros)

| Tabla | Descripción |
|-------|-------------|
| `marcas` | Marcas de productos (jerárquica) |
| `tipos` | Tipos de producto (jerárquica) |
| `origenes` | País/origen del producto |
| `materiales` | Material del producto |
| `aptos` | Certificaciones (apto celíaco, vegano, etc.) |
| `clasif_gral` | Clasificación general (jerárquica) |
| `clasif_gastro` | Clasificación gastronómica (jerárquica, tiene `es_maquina`) |
| `catalogos` | Catálogos/listas de precios |
| `clientes` | Clientes especiales |
| `proveedores` | Proveedores con % financiación |

#### `catalogos` - Detalle
| Campo | Descripción |
|-------|-------------|
| `catalogo` | Nombre del catálogo |
| `exportar_con_iva` | Si el precio exportado incluye IVA (default: true) |
| `recargo_porcentaje` | Recargo % a aplicar sobre el PVP al exportar (default: 0) |

**Uso de catálogos:**
- Los productos se asignan a catálogos mediante la relación `producto_catalogo` (many-to-many)
- Al exportar un catálogo (`/api/excel/exportar-catalogo`), se aplica el recargo configurado
- Los catálogos pueden tener reglas de descuento asociadas (`reglas_descuento.catalogo_id`)
- Útil para generar listas de precios para distintos clientes/canales con márgenes diferentes

**Ejemplo:**
```
Catálogo "LISTA MAYORISTA":
  - exportarConIva: false (precios sin IVA)
  - recargoPorcentaje: 5 (agrega 5% al PVP)
```

---

## Convenciones Generales

### Paginación

Los endpoints de listado soportan paginación con los siguientes parámetros:

| Parámetro | Tipo   | Default | Descripción                          |
|-----------|--------|---------|--------------------------------------|
| `page`    | number | 0       | Número de página (0-indexed)         |
| `size`    | number | 20      | Cantidad de elementos por página     |
| `sort`    | string | -       | Campo y dirección: `campo,asc/desc`  |

**Ejemplo:**
```
GET /api/productos?page=0&size=10&sort=descripcion,asc
```

**Respuesta paginada:**
```typescript
interface PageResponse<T> {
  content: T[];
  page: {
    size: number;           // elementos por página
    number: number;         // página actual (0-indexed)
    totalElements: number;  // total de registros
    totalPages: number;     // total de páginas
  };
}
```

### Búsqueda con `search`

La mayoría de los endpoints de listado soportan un parámetro `search` para filtrado por texto:

| Parámetro | Tipo   | Descripción                                      |
|-----------|--------|--------------------------------------------------|
| `search`  | string | Texto para buscar (case-insensitive, parcial)    |

**Endpoints que soportan `search`:**
- `/api/productos` - busca en descripción, SKU, título web
- `/api/canales` - busca en nombre del canal
- `/api/conceptos-gasto` - busca en concepto y descripción
- `/api/catalogos` - busca en nombre
- `/api/clientes` - busca en nombre
- `/api/proveedores` - busca en nombre
- `/api/marcas` - busca en nombre
- `/api/tipos` - busca en nombre
- `/api/origenes` - busca en nombre
- `/api/materiales` - busca en nombre
- `/api/aptos` - busca en nombre
- `/api/clasif-gral` - busca en nombre
- `/api/clasif-gastro` - busca en nombre
- `/api/promociones` - busca en código y descripción
- `/api/mlas` - busca en MLA y MLAU

**Ejemplo:**
```
GET /api/productos?search=cafetera&page=0&size=10
GET /api/marcas?search=oster
```

**Nota:** El parámetro `search` se puede combinar con paginación y ordenamiento.

### Formato de Errores

```typescript
interface ApiError {
  message: string;
  path: string;
}
```

### Códigos HTTP

| Código | Significado                        |
|--------|------------------------------------|
| 200    | GET/PUT exitoso                    |
| 201    | POST exitoso (Created)             |
| 204    | DELETE exitoso (No Content)        |
| 400    | Validación fallida / Bad Request   |
| 404    | Recurso no encontrado              |
| 409    | Conflicto (duplicado / integridad) |
| 500    | Error interno del servidor         |

---

## Tipos TypeScript

### Producto

```typescript
interface Producto {
  id: number;
  sku: string;
  codExt: string | null;
  descripcion: string;
  tituloWeb: string;
  esCombo: boolean | null;
  uxb: number | null;
  imagenUrl: string | null;
  stock: number | null;
  activo: boolean | null;

  // IDs de relaciones
  marcaId: number | null;
  origenId: number;
  clasifGralId: number;
  clasifGastroId: number | null;
  tipoId: number;
  proveedorId: number | null;
  materialId: number | null;

  // Atributos físicos
  capacidad: string | null;
  largo: number | null;   // BigDecimal
  ancho: number | null;
  alto: number | null;
  diamboca: string | null;
  diambase: string | null;
  espesor: string | null;

  // Precios
  costo: number;          // BigDecimal
  fechaUltCosto: string | null;  // ISO DateTime
  iva: number;            // BigDecimal (0-100)

  // Fechas
  fechaCreacion: string;      // ISO DateTime
  fechaModificacion: string;  // ISO DateTime
}

interface ProductoCreate {
  sku: string;                    // @NotNull, max 45
  codExt?: string;                // max 45
  descripcion: string;            // @NotNull, max 100
  tituloWeb: string;              // @NotNull, max 100
  esCombo?: boolean;
  uxb?: number;                   // > 0
  imagenUrl?: string;             // max 500
  stock?: number;                 // >= 0
  activo?: boolean;

  marcaId?: number;
  origenId: number;               // @NotNull
  clasifGralId: number;           // @NotNull
  clasifGastroId?: number;
  tipoId: number;                 // @NotNull
  proveedorId?: number;
  materialId?: number;

  capacidad?: string;
  largo?: number;
  ancho?: number;
  alto?: number;
  diamboca?: string;
  diambase?: string;
  espesor?: string;

  costo: number;                  // @NotNull, >= 0
  iva: number;                    // @NotNull, 0-100
}

// Para PUT (update parcial - solo enviar campos a modificar)
interface ProductoUpdate extends Partial<ProductoCreate> {}
```

### Producto con Precios (para tablas)

```typescript
interface ProductoConPrecios {
  // Identificación
  id: number;
  sku: string;

  // MLA
  mla: string | null;
  mlau: string | null;
  precioEnvio: number | null;

  codExt: string | null;
  descripcion: string;
  tituloWeb: string;
  esCombo: boolean | null;
  esMaquina: boolean | null;
  imagenUrl: string | null;
  stock: number | null;
  activo: boolean | null;

  // Nombres de relaciones (no IDs)
  marcaNombre: string | null;
  origenNombre: string | null;
  clasifGralNombre: string | null;
  clasifGastroNombre: string | null;
  tipoNombre: string | null;
  proveedorNombre: string | null;
  materialNombre: string | null;

  // Dimensiones
  uxb: number | null;
  capacidad: string | null;
  largo: number | null;
  ancho: number | null;
  alto: number | null;
  diamboca: string | null;
  diambase: string | null;
  espesor: string | null;

  // Precios y costos
  costo: number | null;
  fechaUltCosto: string | null;
  iva: number | null;

  // Márgenes
  margenMinorista: number | null;
  margenMayorista: number | null;

  // Fechas
  fechaCreacion: string;
  fechaModificacion: string;

  // Precios por canal
  canales: CanalPrecios[];
}

interface CanalPrecios {
  canalId: number;
  canalNombre: string;
  precios: Precio[];
}

interface Precio {
  cuotas: number;                  // -1 = transferencia, 0 = contado, >0 = cuotas
  descripcion: string;             // "Transferencia", "Contado", "3 cuotas", etc.
  pvp: number;
  pvpInflado: number | null;
  costoProducto: number;           // Costo base × (1 + financiación proveedor)
  costosVenta: number;             // Σ conceptos PVP + DESCUENTO + RECARGO_CUPON + ENVIO + cuotas
  ingresoNetoVendedor: number;     // PVP - IVA - impuestos - costosVenta
  ganancia: number;                // ingresoNetoVendedor - costoProducto
  margenSobreIngresoNeto: number;  // (ganancia / ingresoNetoVendedor) × 100 - Rentabilidad real
  margenSobrePvp: number;          // (ganancia / pvp) × 100 - Margen tradicional
  markupPorcentaje: number;        // (ganancia / costoProducto) × 100
  fechaUltimoCalculo: string;
}
```

### Filtros de Producto

```typescript
interface ProductoFilter {
  // Filtro por ID
  productoId?: number;

  // Búsqueda por texto (SKU, descripción, título)
  search?: string;

  // Booleanos / Numéricos
  esCombo?: boolean;
  uxb?: number;
  esMaquina?: boolean;
  tieneMla?: boolean;
  activo?: boolean;

  // Many-to-One (IDs)
  marcaId?: number;
  origenId?: number;
  tipoId?: number;
  clasifGralId?: number;
  clasifGastroId?: number;
  proveedorId?: number;
  materialId?: number;

  // Rangos
  costoMin?: number;
  costoMax?: number;
  ivaMin?: number;
  ivaMax?: number;
  stockMin?: number;
  stockMax?: number;

  // Rangos PVP (requiere pvpCanalId)
  pvpMin?: number;
  pvpMax?: number;
  pvpCanalId?: number;

  // Rangos de fechas (ISO date: YYYY-MM-DD)
  desdeFechaUltCosto?: string;
  hastaFechaUltCosto?: string;
  desdeFechaCreacion?: string;
  hastaFechaCreacion?: string;
  desdeFechaModificacion?: string;
  hastaFechaModificacion?: string;

  // Many-to-Many (arrays de IDs)
  aptoIds?: number[];
  canalIds?: number[];
  catalogoIds?: number[];
  clienteIds?: number[];
  mlaIds?: number[];

  // Filtrar precios por canal (también usado para ordenamiento por campos de precio)
  canalId?: number;

  // Filtrar precios por cuotas (también usado para ordenamiento por campos de precio)
  cuotas?: number;

  // Ordenamiento especial: campos de precio (pvp, costoProducto, ganancia, etc.)
  // usan canalId/cuotas para filtrar, o MAX de todos si no se especifican
}
```

### Canal

```typescript
interface Canal {
  id: number;
  canal: string;
  canalBaseId: number | null;
}

interface CanalCreate {
  canal: string;
  canalBaseId?: number;
}

interface CanalUpdate {
  canal?: string;
  canalBaseId?: number;
}
```

### Concepto de Gasto

```typescript
interface ConceptoGasto {
  id: number;
  concepto: string;
  porcentaje: number;           // -100 a 100 (negativos para MARGEN_PTS/MARGEN_PROP)
  aplicaSobre: AplicaSobre;
  descripcion: string | null;
}

interface ConceptoGastoCreate {
  concepto: string;             // @NotBlank, max 45
  porcentaje: number;           // @NotNull, -100 a 100
  aplicaSobre: AplicaSobre;     // @NotBlank
  descripcion?: string;         // max 255
}

interface ConceptoGastoUpdate {
  concepto?: string;            // max 45
  porcentaje?: number;          // -100 a 100
  aplicaSobre?: AplicaSobre;
  descripcion?: string;         // max 255
}

type AplicaSobre =
  // ===== ETAPA: COSTO =====
  | 'GASTO_SOBRE_COSTO'           // Gasto que multiplica el costo base
  | 'FLAG_FINANCIACION_PROVEEDOR' // Flag: usa proveedor.porcentaje

  // ===== ETAPA: MARGEN =====
  | 'AJUSTE_MARGEN_PUNTOS'        // Suma puntos al margen (+ aumenta, - reduce)
  | 'AJUSTE_MARGEN_PROPORCIONAL'  // Multiplica el margen (+ aumenta, - reduce)
  | 'FLAG_USAR_MARGEN_MINORISTA'  // Flag: usa margenMinorista
  | 'FLAG_USAR_MARGEN_MAYORISTA'  // Flag: usa margenMayorista
  | 'GASTO_POST_GANANCIA'         // Gasto después de calcular ganancia

  // ===== ETAPA: IMPUESTOS =====
  | 'FLAG_APLICAR_IVA'            // Flag: aplica IVA del producto
  | 'IMPUESTO_ADICIONAL'          // Impuesto que suma al factor IMP
  | 'GASTO_POST_IMPUESTOS'        // Gasto después de impuestos

  // ===== ETAPA: PRECIO =====
  | 'FLAG_INCLUIR_ENVIO'          // Flag: incluye mla.precioEnvio
  | 'COMISION_SOBRE_PVP'          // Comisión que divide el PVP
  | 'CALCULO_SOBRE_CANAL_BASE'    // Calcula sobre PVP del canal base

  // ===== ETAPA: POST_PRECIO =====
  | 'RECARGO_CUPON'               // Recargo como divisor sobre PVP
  | 'DESCUENTO_PORCENTUAL'        // Descuento que reduce PVP
  | 'INFLACION_DIVISOR'           // Inflación como divisor
  | 'FLAG_APLICAR_PROMOCIONES';   // Flag: habilita promociones

// NOTA: AJUSTE_MARGEN_PUNTOS y AJUSTE_MARGEN_PROPORCIONAL usan el signo del porcentaje:
//   - Porcentaje positivo (+25): aumenta el margen
//   - Porcentaje negativo (-20): reduce el margen
```

### Canal Concepto (relación canal-concepto)

```typescript
interface CanalConcepto {
  canalId: number;
  conceptoId: number;
  concepto: string;
  porcentaje: number;
  aplicaSobre: string;
  descripcion: string | null;
}
```

### Canal Concepto Cuota

```typescript
interface CanalConceptoCuota {
  id: number;
  canalId: number;
  cuotas: number;         // -1 = transferencia, 0 = contado, >0 = cuotas
  porcentaje: number;     // positivo = recargo, negativo = descuento
  descripcion: string | null;  // "Transferencia", "Contado", "3 cuotas", etc.
}

interface CanalConceptoCuotaCreate {
  canalId: number;        // @NotNull, @Positive
  cuotas: number;         // @NotNull: -1 = transferencia, 0 = contado, >0 = cuotas
  porcentaje: number;     // @NotNull, -100 a 100
  descripcion?: string;   // max 255
}

### Canal Concepto Regla

```typescript
interface CanalConceptoRegla {
  id: number;
  canalId: number;
  conceptoId: number;
  tipoRegla: 'INCLUIR' | 'EXCLUIR';
  tipoId: number | null;
  clasifGastroId: number | null;
  clasifGralId: number | null;
  marcaId: number | null;
  esMaquina: boolean | null;
}

interface CanalConceptoReglaCreate {
  canalId: number;              // @NotNull, @Positive
  conceptoId: number;           // @NotNull, @Positive
  tipoRegla: 'INCLUIR' | 'EXCLUIR';  // @NotBlank
  tipoId?: number;              // @Positive
  clasifGastroId?: number;      // @Positive
  clasifGralId?: number;        // @Positive
  marcaId?: number;             // @Positive
  esMaquina?: boolean;
}

interface CanalConceptoReglaUpdate {
  tipoRegla?: 'INCLUIR' | 'EXCLUIR';
  tipoId?: number;              // @Positive
  clasifGastroId?: number;      // @Positive
  clasifGralId?: number;        // @Positive
  marcaId?: number;             // @Positive
  esMaquina?: boolean;
}
```
```

### Regla de Descuento

```typescript
interface ReglaDescuento {
  id: number;
  canalId: number;
  catalogoId: number | null;
  clasifGralId: number | null;
  clasifGastroId: number | null;
  montoMinimo: number;
  descuentoPorcentaje: number;
  prioridad: number;
  activo: boolean;
  descripcion: string | null;
}

interface ReglaDescuentoCreate {
  canalId: number;              // @NotNull, @Positive
  catalogoId?: number;          // @Positive
  clasifGralId?: number;        // @Positive
  clasifGastroId?: number;      // @Positive
  montoMinimo: number;          // @NotNull, >= 0
  descuentoPorcentaje: number;  // @NotNull, 0-100
  prioridad?: number;           // default 1
  activo?: boolean;             // default true
  descripcion?: string;         // max 200
}

interface ReglaDescuentoUpdate {
  catalogoId?: number;          // @Positive
  clasifGralId?: number;        // @Positive
  clasifGastroId?: number;      // @Positive
  montoMinimo?: number;         // >= 0
  descuentoPorcentaje?: number; // 0-100
  prioridad?: number;
  activo?: boolean;
  descripcion?: string;         // max 200
}
```

### Promoción

```typescript
interface Promocion {
  id: number;
  codigo: string;
  tipo: TipoPromocion;
  valor: number;
}

interface PromocionCreate {
  codigo: string;               // @NotBlank, max 20
  tipo: TipoPromocion;          // @NotNull
  valor: number;                // @NotNull, >= 0
}

interface PromocionUpdate {
  codigo?: string;              // max 20
  tipo?: TipoPromocion;
  valor?: number;               // >= 0
}

type TipoPromocion = 'MULTIPLICADOR' | 'DESCUENTO_PORC' | 'DIVISOR' | 'PRECIO_FIJO';
```

### Producto-Canal Promoción

```typescript
interface ProductoCanalPromocion {
  id: number;
  productoId: number;
  canalId: number;
  promocion: Promocion;
  activa: boolean;
  fechaDesde: string | null;  // ISO date
  fechaHasta: string | null;
  notas: string | null;
}
```

### Producto Margen

```typescript
interface ProductoMargen {
  id: number | null;
  productoId: number;
  margenMinorista: number;      // 0-99.999 (%)
  margenMayorista: number;      // 0-99.999 (%)
  margenFijoMinorista: number | null;  // valor absoluto
  margenFijoMayorista: number | null;
  notas: string | null;
}
```

### Fórmula de Cálculo

```typescript
interface FormulaCalculo {
  nombreCanal: string;
  numeroCuotas: number;      // ahora es requerido
  formulaGeneral: string;
  pasos: PasoCalculo[];
  resultadoFinal: number;
}

interface PasoCalculo {
  numeroPaso: number;
  descripcion: string;
  formula: string;
  valor: number;
  detalle: string;
}
```

### MLA (Mercado Libre)

```typescript
interface Mla {
  id: number;
  mla: string;        // código MLA
  mlau: string | null;
  precioEnvio: number | null;
}
```

### Proveedor

```typescript
interface Proveedor {
  id: number;
  proveedor: string;
  apodo: string | null;
  plazoPago: string | null;
  entrega: boolean | null;
  porcentaje: number | null;  // % financiación
}
```

### Otros Maestros

```typescript
interface Marca {
  id: number;
  nombre: string;
  padreId: number | null;  // Marca padre (jerárquica)
}

interface Tipo {
  id: number;
  nombre: string;
  padreId: number | null;  // Tipo padre (jerárquico)
}

interface Origen {
  id: number;
  origen: string;
}

interface Material {
  id: number;
  material: string;
}

interface Apto {
  id: number;
  apto: string;
}

interface ClasifGral {
  id: number;
  nombre: string;
}

interface ClasifGastro {
  id: number;
  nombre: string;
  esMaquina: boolean;
  padreId: number | null;  // Clasificación padre (jerárquica)
}

interface ClasifGastroCreate {
  nombre: string;            // @NotNull, max 100
  esMaquina?: boolean;       // default false
}

interface ClasifGastroUpdate {
  nombre?: string;           // max 100
  esMaquina?: boolean;
}

interface Catalogo {
  id: number;
  catalogo: string;                    // Nombre del catálogo
  exportarConIva: boolean;             // Si exportar con IVA (default: true)
  recargoPorcentaje: number;           // Recargo % al exportar (default: 0)
}

interface Cliente {
  id: number;
  cliente: string;  // Nombre del cliente
}
```

---

## Endpoints por Módulo

### Productos

#### Listar productos (básico)
```http
GET /api/productos
```
**Query params:** `page`, `size`, `sort`
**Response:** `PageResponse<Producto>`

#### Obtener producto por ID
```http
GET /api/productos/{id}
```
**Response:** `Producto`

#### Crear producto
```http
POST /api/productos
Content-Type: application/json

{
  "sku": "BOL-001",
  "descripcion": "Bolsa kraft",
  "tituloWeb": "Bolsa de papel kraft",
  "origenId": 1,
  "clasifGralId": 1,
  "tipoId": 1,
  "costo": 150.00,
  "iva": 21
}
```
**Response:** `201 Created` + `Producto`

#### Actualizar producto
```http
PUT /api/productos/{id}
Content-Type: application/json

{
  "descripcion": "Bolsa kraft actualizada",
  "costo": 180.00
}
```
**Response:** `Producto`

#### Eliminar producto
```http
DELETE /api/productos/{id}
```
**Response:** `204 No Content`

#### Filtrar productos (con parámetros)
```http
GET /api/productos?search=texto&marcaId=1&activo=true
```
**Query params:** `search`, `page`, `size`, `sort` + filtros básicos (marcaId, tipoId, etc.)
**Response:** `PageResponse<Producto>`

**Nota:** Para filtros avanzados con precios, usar `/api/precios` que soporta todos los parámetros de `ProductoFilter`.

---

### Producto - Márgenes

#### Obtener margen del producto
```http
GET /api/productos/{productoId}/margen
```
**Response:** `ProductoMargen`

#### Guardar/actualizar margen
```http
PUT /api/productos/{productoId}/margen
Content-Type: application/json

{
  "margenMinorista": 45.5,
  "margenMayorista": 30.0,
  "margenFijoMinorista": null,
  "margenFijoMayorista": null,
  "notas": "Margen especial"
}
```
**Response:** `ProductoMargen`

#### Eliminar margen
```http
DELETE /api/productos/{productoId}/margen
```
**Response:** `204 No Content`

---

### Producto - Promociones por Canal

#### Obtener promoción
```http
GET /api/productos/{productoId}/canales/{canalId}/promociones
```
**Response:** `ProductoCanalPromocion`

#### Crear promoción
```http
POST /api/productos/{productoId}/canales/{canalId}/promociones
Content-Type: application/json

{
  "promocionId": 1,
  "activa": true,
  "fechaDesde": "2025-01-01",
  "fechaHasta": "2025-12-31",
  "notas": "Promo verano"
}
```
**Response:** `201 Created` + `ProductoCanalPromocion`

#### Actualizar promoción
```http
PUT /api/productos/{productoId}/canales/{canalId}/promociones
```

#### Eliminar promoción
```http
DELETE /api/productos/{productoId}/canales/{canalId}/promociones
```
**Response:** `204 No Content`

---

### Producto - Relaciones Many-to-Many

#### Aptos
```http
GET    /api/productos/{productoId}/aptos              # Listar aptos del producto
POST   /api/productos/{productoId}/aptos/{aptoId}     # Agregar apto
DELETE /api/productos/{productoId}/aptos/{aptoId}     # Quitar apto
```

#### Catálogos
```http
GET    /api/productos/{productoId}/catalogos
POST   /api/productos/{productoId}/catalogos/{catalogoId}
DELETE /api/productos/{productoId}/catalogos/{catalogoId}
```

#### Clientes
```http
GET    /api/productos/{productoId}/clientes
POST   /api/productos/{productoId}/clientes/{clienteId}
DELETE /api/productos/{productoId}/clientes/{clienteId}
```

---

### Precios

#### Listar productos con precios (para tablas)
```http
GET /api/precios
```
**Query params:** Todos los de `ProductoFilter` + `page`, `size`, `sort`
**Response:** `PageResponse<ProductoConPrecios>`

**Ejemplo con filtros:**
```
GET /api/precios?search=bol&marcaId=5&costoMin=100&costoMax=500&canalIds=1,2&page=0&size=20
```

#### Obtener fórmula del cálculo paso a paso
```http
GET /api/precios/formula?productoId=1&canalId=2&cuotas=0
```
**Query params:**
- `productoId` (required): ID del producto
- `canalId` (required): ID del canal
- `cuotas` (required): Número de cuotas (-1 = transferencia, 0 = contado, >0 = cuotas)

**Response:** `FormulaCalculo`

**Ejemplo respuesta:**
```json
{
  "nombreCanal": "KT GASTRO",
  "numeroCuotas": 0,
  "formulaGeneral": "PVP = ((COSTO * (1 + GANANCIA/100)) * IMP / (1 - GTML[%]/100)",
  "pasos": [
    {
      "numeroPaso": 1,
      "descripcion": "Costo base del producto",
      "formula": "COSTO",
      "valor": 4144.89,
      "detalle": "Costo: $4144.89"
    },
    {
      "numeroPaso": 2,
      "descripcion": "Ganancia ajustada",
      "formula": "GANANCIA = MARGEN * (1 - REL_ML_KTG/100)",
      "valor": 52.80,
      "detalle": "MARGEN: 60.000% * (1 - REL_ML_KTG: 12.00%/100) → GANANCIA = 52.80%"
    }
  ],
  "resultadoFinal": 10165.70
}
```

#### Calcular y guardar precios
```http
POST /api/precios/calcular
```
**Query params (todos opcionales):**
- `productoId`: ID del producto
- `canalId`: ID del canal
- `cuotas`: Cantidad de cuotas (-1=transferencia, 0=contado, >0=cuotas)

**Combinaciones:**
| productoId | canalId | cuotas | Comportamiento |
|------------|---------|--------|----------------|
| - | - | - | Recalcula TODOS los productos en TODOS los canales |
| ✓ | - | - | Recalcula el producto en TODOS sus canales (todas las cuotas) |
| ✓ | - | ✓ | Recalcula el producto en TODOS sus canales (solo esas cuotas) |
| ✓ | ✓ | - | Recalcula el producto en ese canal (todas las cuotas) |
| ✓ | ✓ | ✓ | Recalcula solo para esas cuotas en ese canal |

**Response:** `CalculoResultadoDTO`

```typescript
interface CalculoResultadoDTO {
  totalPreciosCalculados: number;
  fechaCalculo: string;                    // ISO DateTime
  canales: CanalPrecios[] | null;          // null para cálculo masivo
  // Campos adicionales para cálculo masivo:
  productosIgnoradosSinCosto: number | null;
  productosIgnoradosSinMargen: number | null;
  errores: number | null;
  skusSinCosto: string[] | null;
  skusSinMargen: string[] | null;
  skusConErrores: string[] | null;
}
```

**Validaciones:**
- Si el producto no tiene **costo** (null o <= 0): se ignora en cálculo masivo, error 400 en cálculo individual
- Si el producto no tiene **margen** requerido por el canal: se ignora en cálculo masivo, error 400 en cálculo individual
  - *"El producto no tiene margen mayorista cargado"*
  - *"El producto no tiene margen minorista cargado"*

**Ejemplo respuesta (producto + canal):**
```json
{
  "totalPreciosCalculados": 2,
  "fechaCalculo": "2026-01-15T17:00:00",
  "canales": [
    {
      "canalId": 3,
      "canalNombre": "KT GASTRO",
      "precios": [
        {
          "cuotas": -1,
          "descripcion": "Transferencia",
          "pvp": 8640.85,
          "pvpInflado": null,
          "costoProducto": 4500.00,
          "costosVenta": 1833.39,
          "ingresoNetoVendedor": 6807.46,
          "ganancia": 2307.46,
          "margenSobreIngresoNeto": 33.90,
          "margenSobrePvp": 26.70,
          "markupPorcentaje": 51.28,
          "fechaUltimoCalculo": "2026-01-15T17:00:00"
        }
      ]
    }
  ]
}
```

**Ejemplo respuesta (masivo - sin parámetros):**
```json
{
  "totalPreciosCalculados": 1250,
  "fechaCalculo": "2026-01-15T15:30:00",
  "canales": null,
  "productosIgnoradosSinCosto": 5,
  "productosIgnoradosSinMargen": 12,
  "errores": 2,
  "skusSinCosto": ["SKU001", "SKU002", "SKU003", "SKU004", "SKU005"],
  "skusSinMargen": ["SKU100", "SKU101", "SKU102", "..."],
  "skusConErrores": ["SKU500", "SKU501"]
}
```

---

### Canales

#### CRUD básico
```http
GET    /api/canales                    # Listar (paginado)
POST   /api/canales                    # Crear
GET    /api/canales/{id}               # Obtener
PUT    /api/canales/{id}               # Actualizar
DELETE /api/canales/{id}               # Eliminar
```

#### Conceptos asignados al canal
```http
GET    /api/canales/{canalId}/conceptos                   # Listar conceptos
POST   /api/canales/{canalId}/conceptos/{conceptoId}      # Asignar concepto
DELETE /api/canales/{canalId}/conceptos/{conceptoId}      # Quitar concepto
```

---

### Canal - Cuotas

```http
GET  /api/canal-concepto-cuotas                           # Listar todas (paginado)
GET  /api/canal-concepto-cuotas/{id}                      # Obtener por ID
GET  /api/canal-concepto-cuotas/canal/{canalId}           # Listar por canal
GET  /api/canal-concepto-cuotas/canal/{canalId}/cuotas/{cuotas}  # Por canal y cuotas
POST /api/canal-concepto-cuotas                           # Crear
PUT  /api/canal-concepto-cuotas/{id}                      # Actualizar
DELETE /api/canal-concepto-cuotas/{id}                    # Eliminar
```

**Crear cuota:**
```json
{
  "canalId": 1,
  "cuotas": -1,
  "porcentaje": -15,
  "descripcion": "Transferencia bancaria"
}
```

**Ejemplos de configuración:**
| cuotas | porcentaje | descripcion | Significado |
|--------|------------|-------------|-------------|
| -1 | -15 | Transferencia | 15% descuento por transferencia |
| 0 | 0 | Contado | Precio contado sin cambio |
| 1 | 0 | 1 cuota | 1 cuota sin interés |
| 3 | 10 | 3 cuotas | 3 cuotas con 10% recargo |
| 6 | 20 | 6 cuotas | 6 cuotas con 20% recargo |

---

### Canal - Reglas de Concepto

```http
GET  /api/canal-concepto-reglas                           # Listar todas (paginado)
GET  /api/canal-concepto-reglas/{id}                      # Obtener por ID
GET  /api/canal-concepto-reglas/canal/{canalId}           # Listar por canal
GET  /api/canal-concepto-reglas/concepto/{conceptoId}     # Listar por concepto
POST /api/canal-concepto-reglas                           # Crear
PUT  /api/canal-concepto-reglas/{id}                      # Actualizar
DELETE /api/canal-concepto-reglas/{id}                    # Eliminar
```

---

### Conceptos de Gasto

```http
GET    /api/conceptos-gastos           # Listar (paginado)
POST   /api/conceptos-gastos           # Crear
GET    /api/conceptos-gastos/{id}      # Obtener
PUT    /api/conceptos-gastos/{id}      # Actualizar
DELETE /api/conceptos-gastos/{id}      # Eliminar
```

**Crear concepto:**
```json
{
  "concepto": "IIBB",
  "porcentaje": 3.5,
  "aplicaSobre": "IMPUESTO_ADICIONAL",
  "descripcion": "Ingresos Brutos"
}
```

---

### Reglas de Descuento

```http
GET    /api/reglas-descuento                    # Listar (paginado)
GET    /api/reglas-descuento/canal/{canalId}    # Listar por canal
GET    /api/reglas-descuento/{id}               # Obtener
POST   /api/reglas-descuento                    # Crear
PUT    /api/reglas-descuento/{id}               # Actualizar
DELETE /api/reglas-descuento/{id}               # Eliminar
```

**Crear regla:**
```json
{
  "canalId": 1,
  "catalogoId": null,
  "clasifGralId": 5,
  "clasifGastroId": null,
  "montoMinimo": 10000,
  "descuentoPorcentaje": 5.0,
  "prioridad": 1,
  "activo": true,
  "descripcion": "Descuento por clasificación"
}
```

---

### Promociones

```http
GET    /api/promociones                    # Listar todas
GET    /api/promociones/{id}               # Obtener por ID
GET    /api/promociones/codigo/{codigo}    # Obtener por código
POST   /api/promociones                    # Crear
PUT    /api/promociones/{id}               # Actualizar
DELETE /api/promociones/{id}               # Eliminar
```

**Crear promoción:**
```json
{
  "codigo": "VERANO25",
  "tipo": "DESCUENTO_PORC",
  "valor": 10.0
}
```

**Tipos válidos:** `MULTIPLICADOR`, `DESCUENTO_PORC`, `DIVISOR`, `PRECIO_FIJO`

---

### Catálogos y Clientes

#### Catálogos
```http
GET    /api/catalogos
POST   /api/catalogos
GET    /api/catalogos/{id}
PUT    /api/catalogos/{id}
DELETE /api/catalogos/{id}
```

#### Clientes
```http
GET    /api/clientes
POST   /api/clientes
GET    /api/clientes/{id}
PUT    /api/clientes/{id}
DELETE /api/clientes/{id}
```

---

### Atributos Maestros

Todos siguen el patrón CRUD estándar:

| Recurso           | Endpoint              |
|-------------------|-----------------------|
| Marcas            | `/api/marcas`         |
| Tipos             | `/api/tipos`          |
| Orígenes          | `/api/origenes`       |
| Materiales        | `/api/materiales`     |
| Aptos             | `/api/aptos`          |
| Clasif. General   | `/api/clasif-gral`    |
| Clasif. Gastro    | `/api/clasif-gastro`  |
| MLAs              | `/api/mlas`           |
| Proveedores       | `/api/proveedores`    |

---

### Excel (Import/Export)

#### Exportar precios a Excel
```http
GET /api/excel/exportar-precios
GET /api/excel/exportar-precios?formato=completo    # default
GET /api/excel/exportar-precios?formato=mercadolibre&cuotas=0
GET /api/excel/exportar-precios?formato=nube&cuotas=0
```
**Query params:**
- `formato` (optional, default=completo): Formato de exportación (completo, mercadolibre, nube)
- Para formato `completo`: acepta todos los params de `ProductoFilter`
- Para formato `mercadolibre` y `nube`: requiere `cuotas` (-1=transferencia, 0=contado, >0=cuotas)

**Response:** Archivo Excel (application/octet-stream)
- Headers fijos (freeze panes): los headers permanecen visibles al hacer scroll
- Formato `completo`: super headers con bordes gruesos, canales separados visualmente con bordes
- Formatos `mercadolibre` y `nube` incluyen header `X-Advertencias-Count` si hay advertencias (el detalle se registra en el log del servidor)

#### Exportar catálogo a Excel
```http
GET /api/excel/exportar-catalogo?catalogoId=4&canalId=1&cuotas=0
```
**Query params:**
- `catalogoId` (required): ID del catálogo
- `canalId` (required): ID del canal
- `cuotas` (required): Cantidad de cuotas (-1=transferencia, 0=contado, >0=cuotas)
- `clasifGralId` (optional): Filtrar por clasificación general
- `clasifGastroId` (optional): Filtrar por clasificación gastro
- `tipoId` (optional): Filtrar por tipo
- `marcaId` (optional): Filtrar por marca
- `esMaquina` (optional): Filtrar por máquina
- `ordenarPor` (optional): Campos de ordenamiento separados por coma

**Response:** Archivo Excel (application/octet-stream)

#### Importar costos (actualizar productos existentes)
```http
POST /api/excel/importar-costos
Content-Type: multipart/form-data

archivo: [COSTOS.xls]
```

**Columnas esperadas en el Excel:**
| Columna Excel        | Campo en BD           | Notas                                    |
|----------------------|-----------------------|------------------------------------------|
| CODIGO               | sku                   | Requerido. Busca producto existente      |
| PRODUCTO             | descripcion           | Max 100 caracteres                       |
| COSTO                | costo                 | Formato: 47.639,88 (punto miles, coma decimal) |
| CODIGO EXTERNO       | cod_ext               | Max 45 caracteres                        |
| PROVEEDOR            | proveedor_id          | Se crea si no existe                     |
| TIPO DE PRODUCTO     | es_combo              | "COMBO" → true, otro → false             |
| ULTIMA ACT. COSTO    | fecha_ult_costo       | Formato: dd/MM/yyyy                      |
| UNIDADES POR BULTO   | uxb                   | Se redondea a entero                     |
| PORCENTAJE IVA       | iva                   | Formato: 21,00                           |

**Response:**
```typescript
interface ImportCostosResult {
  productosActualizados: number;
  productosNoEncontrados: number;
  proveedoresCreados: number;
  skusNoEncontrados: string[];    // Lista de SKUs que no existen
  errores: string[];              // Errores de formato/validación
}
```

**Recálculo automático:** Si la importación modifica `costo`, `iva` o `proveedor`, los precios de esos productos se recalculan automáticamente en todos sus canales.

#### Importar migración completa
```http
POST /api/excel/importar-migracion
Content-Type: multipart/form-data

archivo: [archivo.xlsx]
```
**Response:**
```typescript
interface ImportCompletoResult {
  totalHojas: number;
  hojasProcesadas: number;
  hojasConErrores: number;
  resultadosPorHoja: Record<string, ImportResult>;
  erroresGenerales: string[];
  message: string;
}

interface ImportResult {
  totalRows: number;
  successRows: number;
  errorRows: number;
  errors: string[];
  message: string;
}
```

**Ejemplo respuesta:**
```json
{
  "totalHojas": 5,
  "hojasProcesadas": 5,
  "hojasConErrores": 0,
  "resultadosPorHoja": {
    "PRODUCTOS": {
      "totalRows": 150,
      "successRows": 150,
      "errorRows": 0,
      "errors": [],
      "message": "Importación exitosa: 150 de 150 filas procesadas correctamente"
    }
  },
  "erroresGenerales": [],
  "message": "Importación completa exitosa: 5 de 5 hojas procesadas correctamente"
}
```

---

## Ejemplos de Uso (fetch)

### Listar productos con precios y filtros
```typescript
const params = new URLSearchParams({
  search: 'bolsa',
  marcaId: '5',
  costoMin: '100',
  page: '0',
  size: '20',
  sort: 'descripcion,asc'
});

const response = await fetch(`http://localhost:8080/api/precios?${params}`);
const data: PageResponse<ProductoConPrecios> = await response.json();
```

### Crear producto
```typescript
const response = await fetch('http://localhost:8080/api/productos', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    sku: 'BOL-001',
    descripcion: 'Bolsa kraft',
    tituloWeb: 'Bolsa de papel kraft',
    origenId: 1,
    clasifGralId: 1,
    tipoId: 1,
    costo: 150.00,
    iva: 21
  })
});

if (response.status === 201) {
  const producto: Producto = await response.json();
}
```

### Recalcular precios de un producto-canal
```typescript
const params = new URLSearchParams({
  productoId: '123',
  canalId: '1'
});

const response = await fetch(
  `http://localhost:8080/api/precios/calcular?${params}`,
  { method: 'POST' }
);
const precios: CanalPrecios = await response.json();
```

### Obtener fórmula de cálculo
```typescript
const params = new URLSearchParams({
  productoId: '123',
  canalId: '1',
  cuotas: '0'
});

const response = await fetch(`http://localhost:8080/api/precios/formula?${params}`);
const formula: FormulaCalculo = await response.json();
```

### Manejo de errores
```typescript
try {
  const response = await fetch(`http://localhost:8080/api/productos/${id}`);

  if (!response.ok) {
    const error: ApiError = await response.json();
    throw new Error(error.message);
  }

  return await response.json();
} catch (error) {
  console.error('Error:', error);
}
```

---

## Comportamiento de Eliminación (ON DELETE)

Al eliminar entidades, el comportamiento varía según las relaciones configuradas:

### Entidades que se eliminan en cascada (CASCADE)

Al eliminar estas entidades, **se eliminan automáticamente** sus registros relacionados:

| Al eliminar... | Se eliminan automáticamente... |
|----------------|-------------------------------|
| **Producto** | ProductoApto, ProductoCatalogo, ProductoCliente, ProductoCanalPrecio, ProductoCanalPromocion, ProductoMargen |
| **Catálogo** | ProductoCatalogo (relaciones con productos), ReglaDescuento |
| **Canal** | ProductoCanalPrecio, ProductoCanalPromocion, CanalConceptoCuota, CanalConceptoRegla, ReglaDescuento |
| **ConceptoGasto** | CanalConcepto, CanalConceptoRegla |
| **Apto** | ProductoApto (relaciones con productos) |
| **Cliente** | ProductoCliente (relaciones con productos) |

### Entidades que setean NULL (SET_NULL)

Al eliminar estas entidades, los registros relacionados **mantienen su FK en NULL**:

| Al eliminar... | Se setea NULL en... |
|----------------|---------------------|
| **Marca** (padre) | Submarcas (padre_id) |
| **Tipo** (padre) | Subtipos (padre_id) |
| **ClasifGral** (padre) | Subclasificaciones (padre_id) |
| **ClasifGastro** (padre) | Subclasificaciones (padre_id) |
| **Canal** (base) | Canales hijos (canal_base_id) |
| **Catálogo** | ReglaDescuento (catalogo_id) |
| **ClasifGral** | ReglaDescuento, CanalConceptoRegla |
| **ClasifGastro** | ReglaDescuento, CanalConceptoRegla |
| **Tipo** | CanalConceptoRegla (tipo_id) |
| **Marca** | CanalConceptoRegla (marca_id) |

### ⚠️ Entidades que BLOQUEAN la eliminación (RESTRICT)

**No se puede eliminar** estas entidades si tienen registros relacionados:

| Entidad | Bloqueada por | Mensaje de error |
|---------|---------------|------------------|
| **Marca** | Producto | "No se puede eliminar porque tiene registros relacionados en: Productos" |
| **Origen** | Producto | "No se puede eliminar porque tiene registros relacionados en: Productos" |
| **Tipo** | Producto | "No se puede eliminar porque tiene registros relacionados en: Productos" |
| **ClasifGral** | Producto | "No se puede eliminar porque tiene registros relacionados en: Productos" |
| **ClasifGastro** | Producto | "No se puede eliminar porque tiene registros relacionados en: Productos" |
| **Proveedor** | Producto | "No se puede eliminar porque tiene registros relacionados en: Productos" |
| **Material** | Producto | "No se puede eliminar porque tiene registros relacionados en: Productos" |
| **Mla** | Producto | "No se puede eliminar porque tiene registros relacionados en: Productos" |
| **Promocion** | ProductoCanalPromocion | "No se puede eliminar porque tiene registros relacionados en: Producto canal promocion" |

### Formato de error 409 (Conflict)

```typescript
interface ErrorResponse {
  success: false;
  message: string;  // "No se puede eliminar porque tiene registros relacionados en: Producto apto"
  path: string;     // "/api/aptos/5"
  timestamp: string;
}
```

### Recomendación para el Frontend

Antes de eliminar entidades que pueden estar bloqueadas, considerar:

1. **Mostrar confirmación** indicando qué se eliminará en cascada
2. **Verificar relaciones** usando los endpoints GET inversos:
   - `GET /api/catalogos/{id}/productos` - ver productos del catálogo
   - `GET /api/aptos/{id}/productos` - ver productos con ese apto
   - `GET /api/clientes/{id}/productos` - ver productos del cliente
   - `GET /api/marcas/{id}/productos` - ver productos de la marca
   - `GET /api/origenes/{id}/productos` - ver productos del origen
   - `GET /api/tipos/{id}/productos` - ver productos del tipo
   - `GET /api/clasif-gral/{id}/productos` - ver productos de la clasificación general
   - `GET /api/clasif-gastro/{id}/productos` - ver productos de la clasificación gastro
   - `GET /api/proveedores/{id}/productos` - ver productos del proveedor
   - `GET /api/materiales/{id}/productos` - ver productos del material
   - `GET /api/mlas/{id}/productos` - ver productos del MLA
3. **Manejar el error 409** mostrando el mensaje descriptivo al usuario

### Endpoints GET inversos (Productos por entidad)

Todos estos endpoints devuelven `List<ProductoResumenDTO>`:

```typescript
interface ProductoResumenDTO {
  id: number;
  sku: string;
  descripcion: string;
}
```

| Endpoint | Descripción |
|----------|-------------|
| `GET /api/catalogos/{id}/productos` | Productos asignados al catálogo |
| `GET /api/aptos/{id}/productos` | Productos con el apto |
| `GET /api/clientes/{id}/productos` | Productos asignados al cliente |
| `GET /api/marcas/{id}/productos` | Productos de la marca |
| `GET /api/origenes/{id}/productos` | Productos del origen |
| `GET /api/tipos/{id}/productos` | Productos del tipo |
| `GET /api/clasif-gral/{id}/productos` | Productos de la clasificación general |
| `GET /api/clasif-gastro/{id}/productos` | Productos de la clasificación gastro |
| `GET /api/proveedores/{id}/productos` | Productos del proveedor |
| `GET /api/materiales/{id}/productos` | Productos del material |
| `GET /api/mlas/{id}/productos` | Productos del MLA |

**Respuestas:**
- `200 OK` + lista de productos (puede estar vacía)
- `404 Not Found` si la entidad no existe

---

## Notas para el Frontend

1. **Recálculo automático:** El backend recalcula automáticamente los precios cuando se modifican:

   | Dato modificado | Alcance del recálculo |
   |-----------------|----------------------|
   | Producto (costo, IVA, clasifGastro) | Ese producto en todos sus canales |
   | ProductoMargen (márgenes) | Ese producto en todos sus canales |
   | ConceptoGasto (porcentaje o aplicaSobre) | Todos los productos de canales que usan ese concepto |
   | CanalConcepto (asignar/quitar concepto) | Todos los productos del canal |
   | CanalConceptoCuota (porcentaje cuotas) | Todos los productos del canal |
   | Canal (canalBase) | Todos los productos del canal cuyo canalBase cambió |
   | Proveedor (porcentaje financiación) | Todos los productos de ese proveedor |
   | ReglaDescuento | Todos los productos del canal de la regla |
   | Promoción (asignar/desasignar) | Ese producto en ese canal |
   | MLA (precioEnvio) | Todos los productos con ese MLA |
   | ClasifGastro (esMaquina) | Todos los productos de esa clasificación en todos sus canales |

2. **Filtros many-to-many:** Para filtrar por múltiples valores, enviar como array o separados por coma:
   ```
   GET /api/precios?canalIds=1,2,3&aptoIds=1,2
   ```

3. **Ordenamiento especial:** Campos calculados de precio y otros campos especiales usan el parámetro `sort` estándar de Spring:

   **Campos de precio** (usan los filtros `canalId`/`cuotas`, o MAX de todos si no se especifican):
   - `pvp`, `pvpInflado`, `costoProducto`, `costosVenta`
   - `ingresoNetoVendedor`, `ganancia`
   - `margenSobreIngreso`, `margenSobrePvp`, `markup`

   **Otros campos:** `mla`, `esMaquina`, `costo`

   ```
   GET /api/precios?sort=ganancia,desc                       # MAX ganancia de todos los canales
   GET /api/precios?sort=pvp,asc&canalId=1                   # PVP del canal 1
   GET /api/precios?sort=margenSobrePvp,desc&canalId=2&cuotas=0  # Margen del canal 2, contado
   GET /api/precios?sort=mla,desc
   GET /api/precios?sort=costo,asc
   ```

4. **Fechas:** Enviar en formato ISO:
   - Date: `YYYY-MM-DD` (ej: `2025-01-15`)
   - DateTime: `YYYY-MM-DDTHH:mm:ss` (ej: `2025-01-15T14:30:00`)

5. **Decimales:** Se manejan con 2 decimales de precisión para precios y porcentajes.

6. **Conceptos flag:** Algunos conceptos (`FLAG_APLICAR_IVA`, `FLAG_USAR_MARGEN_MINORISTA`, `FLAG_USAR_MARGEN_MAYORISTA`, `FLAG_APLICAR_PROMOCIONES`, `FLAG_FINANCIACION_PROVEEDOR`, `FLAG_INCLUIR_ENVIO`) actúan como flags. Solo importa si están asignados al canal o no, el porcentaje se ignora.

7. **Cuotas:** El sistema de cuotas usa:
   - `cuotas = -1` → Transferencia (generalmente con descuento)
   - `cuotas = 0` → Contado (precio base)
   - `cuotas > 0` → Cantidad de cuotas (1, 3, 6, 12, etc.)

   Solo se calculan las opciones configuradas en `canal_concepto_cuota` para cada canal.

8. **Porcentaje en cuotas:** Valores positivos son recargos (ej: +10% para 3 cuotas), valores negativos son descuentos (ej: -15% para transferencia).
