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
| `margen_fijo_*` | Ganancia fija en $ (opcional, tiene prioridad) |

Cada producto puede tener UN registro de márgenes. El canal determina cuál usar según tenga el concepto `MARGEN_MINORISTA` o `MARGEN_MAYORISTA`.

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

#### 4. `conceptos_gastos` - Conceptos de Costo/Gasto
| Campo | Descripción |
|-------|-------------|
| `concepto` | Nombre (ej: "GTML", "IIBB", "IVA") |
| `porcentaje` | Valor del concepto |
| `aplica_sobre` | Cómo se aplica en la fórmula |

**Valores de `aplica_sobre`:**

| Valor | Descripción | Ejemplo |
|-------|-------------|---------|
| `COSTO` | Se suma al costo base | Embalaje +2% |
| `PVP` | Se aplica sobre precio final | Comisión ML -13% |
| `IMP` | Se suma a impuestos | IIBB +3.5% |
| `MARGEN_PTS` | Ajusta margen en puntos | +5 puntos al margen |
| `MARGEN_PROP` | Ajusta margen proporcional | -12% del margen |
| `RECARGO_CUPON` | Divisor sobre PVP | Cupones +5% |
| `DESCUENTO` | Descuento final | Promo -10% |
| `ENVIO` | Usa precio de envío del MLA | Envío gratis |
| `INFLACION` | Divisor de inflación | Inflación +8% |
| `PROVEEDOR_FIN` | Usa % financiación del proveedor | |
| `IVA` | Flag: aplicar IVA del producto | |
| `MARGEN_MINORISTA` | Flag: usar margen minorista | |
| `MARGEN_MAYORISTA` | Flag: usar margen mayorista | |
| `PROMOCION` | Flag: aplicar promociones | |
| `SOBRE_PVP_BASE` | Calcula sobre PVP del canal base | |

#### 5. `canal_concepto` - Conceptos Asignados a Canales
Tabla intermedia que relaciona qué conceptos aplican a cada canal.

**Ejemplo:**
- Canal "ML" tiene: GTML, IIBB, IVA, MARGEN_MINORISTA
- Canal "MAYORISTA" tiene: IVA, MARGEN_MAYORISTA

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

#### 7. `producto_canal_precios` - Precios Calculados
| Campo | Descripción |
|-------|-------------|
| `pvp` | Precio de venta al público |
| `pvp_inflado` | PVP con inflación (para mostrar tachado) |
| `costo_producto` | Costo ajustado (con financiación/embalaje) |
| `costos_venta` | Suma de comisiones y gastos |
| `ingreso_neto_vendedor` | Lo que queda después de todo |
| `ganancia` | Ingreso neto - costo producto |
| `margen_porcentaje` | (ganancia / ingreso neto) × 100 |
| `markup_porcentaje` | (ganancia / costo) × 100 |

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
interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;        // página actual
  first: boolean;
  last: boolean;
  empty: boolean;
}
```

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
  costoProducto: number;           // Costo base × financiación × embalaje
  costosVenta: number;             // Suma de conceptos de venta aplicables
  ingresoNetoVendedor: number;     // PVP - IVA - impuestos - costosVenta
  ganancia: number;                // ingresoNetoVendedor - costoProducto
  margenPorcentaje: number;        // (ganancia / ingresoNetoVendedor) × 100
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

  // Ordenamiento especial
  sortBy?: 'pvp' | 'costo' | 'mla' | 'esMaquina';
  sortDir?: 'asc' | 'desc';
  sortCanalId?: number;   // canal para ordenar por PVP (requerido si sortBy=pvp)

  // Filtrar precios por canal específico
  canalId?: number;

  // Filtrar precios por cuotas
  cuotas?: number;
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
  concepto: string;             // @NotNull, max 100
  porcentaje: number;           // @NotNull, -100 a 100
  aplicaSobre: AplicaSobre;     // @NotNull
  descripcion?: string;         // max 255
}

interface ConceptoGastoUpdate {
  concepto?: string;            // max 100
  porcentaje?: number;          // -100 a 100
  aplicaSobre?: AplicaSobre;
  descripcion?: string;         // max 255
}

type AplicaSobre =
  | 'COSTO'              // Se suma al costo base
  | 'PVP'                // Se aplica sobre el PVP
  | 'COSTO_IVA'          // Después de IVA
  | 'MARGEN_PTS'         // Modifica margen en puntos (+ aumenta, - reduce)
  | 'MARGEN_PROP'        // Modifica margen proporcionalmente (+ aumenta, - reduce)
  | 'IMP'                // Se suma al factor de impuestos
  | 'RECARGO_CUPON'      // Divisor adicional sobre PVP
  | 'DESCUENTO'          // Descuento final sobre PVP
  | 'ENVIO'              // Usa precio_envio de MLA
  | 'INFLACION'          // Divisor de inflación
  | 'PROVEEDOR_FIN'      // % financiación del proveedor
  | 'COSTO_GANANCIA'     // Después de ganancia, antes de IMP
  | 'IVA'                // Habilita IVA para el canal (flag)
  | 'SOBRE_PVP_BASE'     // Calcula sobre PVP del canal base
  | 'MARGEN_MINORISTA'   // Canal usa margen minorista (flag)
  | 'MARGEN_MAYORISTA'   // Canal usa margen mayorista (flag)
  | 'PROMOCION';         // Habilita promociones (flag)

// NOTA: MARGEN_PTS y MARGEN_PROP usan el signo del porcentaje:
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
  canalId: number;
  cuotas: number;         // -1 = transferencia, 0 = contado, >0 = cuotas
  porcentaje: number;     // -100 a 100
  descripcion?: string;
}
```

### Regla de Descuento

```typescript
interface ReglaDescuento {
  id: number;
  canalId: number;
  catalogoId: number | null;
  clasifGralId: number | null;
  clasifGastroId: number | null;
  montoMinimo: number | null;
  descuentoPorcentaje: number;
  prioridad: number;
  activo: boolean;
  descripcion: string | null;
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

type TipoPromocion = 'PORCENTAJE' | 'MONTO_FIJO';
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
}

interface Tipo {
  id: number;
  nombre: string;
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
  nombre: string;
}

interface Cliente {
  id: number;
  nombre: string;
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
**Response:** `Page<Producto>`

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

#### Buscar/filtrar productos (sin precios)
```http
GET /api/productos/buscar
```
**Query params:** Todos los de `ProductoFilter` excepto `canalId` y `cuotas`
**Response:** `Page<Producto>`

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
**Response:** `Page<ProductoConPrecios>`

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
  fechaCalculo: string;           // ISO DateTime
  canales: CanalPrecios[] | null; // null para cálculo masivo
}
```

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
          "margenPorcentaje": 33.90,
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
  "canales": null
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
  "aplicaSobre": "IMP",
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
  "tipo": "PORCENTAJE",
  "valor": 10.0
}
```

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
GET /api/excel/exportar-precios?formato=mercadolibre
GET /api/excel/exportar-precios?formato=nube
```
**Query params:**
- `formato` (optional, default=completo): Formato de exportación (completo, mercadolibre, nube)
- Para formato `completo`: acepta todos los params de `ProductoFilter`
- Para formato `mercadolibre` y `nube`: solo acepta `cuotas`

**Response:** Archivo Excel (application/octet-stream)
- Formatos `mercadolibre` y `nube` incluyen header `X-Advertencias` si hay advertencias

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
const data: Page<ProductoConPrecios> = await response.json();
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

## Notas para el Frontend

1. **Recálculo automático:** El backend recalcula automáticamente los precios cuando se modifican:

   | Dato modificado | Alcance del recálculo |
   |-----------------|----------------------|
   | Producto (costo, IVA) | Ese producto en todos sus canales |
   | ProductoMargen (márgenes) | Ese producto en todos sus canales |
   | ConceptoGasto (porcentaje o aplicaSobre) | Todos los productos de canales que usan ese concepto |
   | CanalConceptoCuota (porcentaje cuotas) | Todos los productos del canal |
   | Proveedor (porcentaje financiación) | Todos los productos de ese proveedor |
   | ReglaDescuento | Todos los productos del canal de la regla |
   | Promoción (asignar/desasignar) | Ese producto en ese canal |
   | MLA (precioEnvio) | Todos los productos con ese MLA |
   | ClasifGastro (esMaquina) | Todos los productos de esa clasificación en todos sus canales |

2. **Filtros many-to-many:** Para filtrar por múltiples valores, enviar como array o separados por coma:
   ```
   GET /api/precios?canalIds=1,2,3&aptoIds=1,2
   ```

3. **Ordenamiento por PVP:** Para ordenar por precio de un canal específico (requiere `sortCanalId`):
   ```
   GET /api/precios?sortBy=pvp&sortDir=asc&sortCanalId=1
   ```

4. **Fechas:** Enviar en formato ISO:
   - Date: `YYYY-MM-DD` (ej: `2025-01-15`)
   - DateTime: `YYYY-MM-DDTHH:mm:ss` (ej: `2025-01-15T14:30:00`)

5. **Decimales:** Se manejan con 2 decimales de precisión para precios y porcentajes.

6. **Conceptos flag:** Algunos conceptos (`IVA`, `MARGEN_MINORISTA`, `MARGEN_MAYORISTA`, `PROMOCION`) actúan como flags. Solo importa si están asignados al canal o no, el porcentaje se ignora.

7. **Cuotas:** El sistema de cuotas usa:
   - `cuotas = -1` → Transferencia (generalmente con descuento)
   - `cuotas = 0` → Contado (precio base)
   - `cuotas > 0` → Cantidad de cuotas (1, 3, 6, 12, etc.)

   Solo se calculan las opciones configuradas en `canal_concepto_cuota` para cada canal.

8. **Porcentaje en cuotas:** Valores positivos son recargos (ej: +10% para 3 cuotas), valores negativos son descuentos (ej: -15% para transferencia).
