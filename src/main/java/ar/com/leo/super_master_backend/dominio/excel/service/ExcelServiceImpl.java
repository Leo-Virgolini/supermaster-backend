package ar.com.leo.super_master_backend.dominio.excel.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.AssertionFailure;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.catalogo.entity.Catalogo;
import ar.com.leo.super_master_backend.dominio.catalogo.repository.CatalogoRepository;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.entity.ClasifGastro;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.repository.ClasifGastroRepository;
import ar.com.leo.super_master_backend.dominio.clasif_gral.entity.ClasifGral;
import ar.com.leo.super_master_backend.dominio.clasif_gral.repository.ClasifGralRepository;
import ar.com.leo.super_master_backend.dominio.excel.dto.ImportCompletoResultDTO;
import ar.com.leo.super_master_backend.dominio.excel.dto.ImportResultDTO;
import ar.com.leo.super_master_backend.dominio.marca.entity.Marca;
import ar.com.leo.super_master_backend.dominio.marca.repository.MarcaRepository;
import ar.com.leo.super_master_backend.dominio.material.entity.Material;
import ar.com.leo.super_master_backend.dominio.material.repository.MaterialRepository;
import ar.com.leo.super_master_backend.dominio.origen.entity.Origen;
import ar.com.leo.super_master_backend.dominio.origen.repository.OrigenRepository;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanal;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCatalogo;
import ar.com.leo.super_master_backend.dominio.producto.mla.entity.Mla;
import ar.com.leo.super_master_backend.dominio.producto.mla.repository.MlaRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCatalogoRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoSpecifications;
import ar.com.leo.super_master_backend.dominio.proveedor.entity.Proveedor;
import ar.com.leo.super_master_backend.dominio.proveedor.repository.ProveedorRepository;
import ar.com.leo.super_master_backend.dominio.tipo.entity.Tipo;
import ar.com.leo.super_master_backend.dominio.tipo.repository.TipoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Record para almacenar información de hojas a procesar
record SheetInfo(org.apache.poi.ss.usermodel.Sheet sheet, String nombre, int prioridad) {}

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelServiceImpl implements ExcelService {

    private static final ZoneId ZONA_ARG = ZoneId.of("America/Argentina/Buenos_Aires");

    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoCanalPrecioRepository productoCanalPrecioRepository;
    private final MarcaRepository marcaRepository;
    private final OrigenRepository origenRepository;
    private final TipoRepository tipoRepository;
    private final ClasifGralRepository clasifGralRepository;
    private final ClasifGastroRepository clasifGastroRepository;
    private final MlaRepository mlaRepository;
    private final CatalogoRepository catalogoRepository;
    private final CanalRepository canalRepository;
    private final MaterialRepository materialRepository;
    private final ProductoCatalogoRepository productoCatalogoRepository;
    private final ProductoCanalRepository productoCanalRepository;
    
    // Caches en memoria para evitar consultas repetidas durante la importación
    private final Map<String, Marca> cacheMarcas = new ConcurrentHashMap<>();
    private final Map<String, Origen> cacheOrigenes = new ConcurrentHashMap<>();
    private final Map<String, ClasifGral> cacheClasifGral = new ConcurrentHashMap<>();
    private final Map<String, ClasifGastro> cacheClasifGastro = new ConcurrentHashMap<>();
    private final Map<String, Proveedor> cacheProveedores = new ConcurrentHashMap<>();
    private final Map<String, Tipo> cacheTipos = new ConcurrentHashMap<>();
    private final Map<String, Catalogo> cacheCatalogos = new ConcurrentHashMap<>();
    private final Map<String, Canal> cacheCanales = new ConcurrentHashMap<>();
    private final Map<String, Material> cacheMateriales = new ConcurrentHashMap<>();

    private boolean esFilaVacia(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null) {
                String valor = obtenerValorCelda(row, i);
                if (valor != null && !valor.isBlank()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public byte[] exportar(String tipo, Integer canalId, Integer catalogoId, Integer clasifGralId) throws IOException {
        log.info("Iniciando exportación de tipo: {}, canalId: {}, catalogoId: {}, clasifGralId: {}",
                tipo, canalId, catalogoId, clasifGralId);

        // Validar parámetros requeridos para catálogo
        if ("catalogo".equalsIgnoreCase(tipo)) {
            if (canalId == null) {
                throw new IllegalArgumentException("El canalId es requerido para exportar catálogo");
            }
            if (catalogoId == null) {
                throw new IllegalArgumentException("El catalogoId es requerido para exportar catálogo");
            }
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Datos");

            // Crear encabezados según el tipo
            Row headerRow = sheet.createRow(0);
            List<String> headers = obtenerEncabezados(tipo);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(crearEstiloEncabezado(workbook));
            }

            // Llenar datos según el tipo
            llenarDatos(sheet, tipo, canalId, catalogoId, clasifGralId);

            // Auto-ajustar columnas
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            // Escribir a bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private boolean isExcelFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null)
            return false;
        return filename.endsWith(".xls") || filename.endsWith(".xlsx") ||
                filename.endsWith(".xlsm") || filename.endsWith(".XLS") ||
                filename.endsWith(".XLSX") || filename.endsWith(".XLSM");
    }

    /**
     * Abre un Workbook de manera eficiente.
     * Para archivos .xlsx grandes, usa OPCPackage para mejor rendimiento y menor uso de memoria.
     * Para archivos .xls o como fallback, usa WorkbookFactory.
     * 
     * Nota: El OPCPackage se cierra automáticamente cuando se cierra el Workbook.
     * 
     * @param file El archivo Excel a abrir
     * @return Workbook abierto
     * @throws IOException Si hay un error al leer el archivo
     */
    private Workbook abrirWorkbookEficiente(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        boolean esXlsx = filename != null && 
                (filename.endsWith(".xlsx") || filename.endsWith(".XLSX") || 
                 filename.endsWith(".xlsm") || filename.endsWith(".XLSM"));
        
        if (esXlsx) {
            // Para archivos .xlsx, usar OPCPackage para mejor rendimiento
            // OPCPackage lee el archivo de manera más eficiente para archivos grandes
            // y reduce el uso de memoria al no cargar todo el archivo de una vez
            try {
                // OPCPackage.open(InputStream) no requiere PackageAccess (solo lectura implícita)
                OPCPackage pkg = OPCPackage.open(file.getInputStream());
                XSSFWorkbook workbook = new XSSFWorkbook(pkg);
                // El OPCPackage se cierra automáticamente cuando se cierra el Workbook
                return workbook;
            } catch (Exception e) {
                // Si falla con OPCPackage, leer el contenido completo y usar WorkbookFactory
                // Esto es necesario porque el InputStream ya se consumió
                log.warn("No se pudo abrir con OPCPackage, usando WorkbookFactory con contenido en memoria: {}", e.getMessage());
                byte[] contenido = file.getBytes();
                return WorkbookFactory.create(new java.io.ByteArrayInputStream(contenido));
            }
        } else {
            // Para archivos .xls, usar WorkbookFactory
            return WorkbookFactory.create(file.getInputStream());
        }
    }

    /**
     * Mapea los nombres de las columnas a sus índices
     */
    private Map<String, Integer> mapearColumnasPorNombre(Row headerRow) {
        Map<String, Integer> columnasMap = new HashMap<>();

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String nombreColumna = obtenerValorCelda(headerRow, i);
                if (nombreColumna != null && !nombreColumna.isBlank()) {
                    // Normalizar nombre de columna (trim, uppercase para comparación)
                    String nombreNormalizado = nombreColumna.trim().toUpperCase();
                    columnasMap.put(nombreNormalizado, i);
                }
            }
        }

        return columnasMap;
    }

    /**
     * Obtiene el índice de una columna por su nombre (case-insensitive)
     */
    private Integer obtenerIndiceColumna(Map<String, Integer> columnasMap, String nombreColumna) {
        String nombreNormalizado = nombreColumna.trim().toUpperCase();
        Integer indice = columnasMap.get(nombreNormalizado);
        if (indice == null) {
            throw new IllegalArgumentException(
                    String.format("Columna '%s' no encontrada en el archivo. Columnas disponibles: %s",
                            nombreColumna, columnasMap.keySet()));
        }
        return indice;
    }

    private void procesarFila(Row row, String tipo, int rowIndex, Map<String, Integer> columnasMap) {
        switch (tipo.toLowerCase()) {
            case "master":
                procesarFilaMaster(row, rowIndex, columnasMap);
                break;
            case "titulos_web":
                procesarFilaTitulosWeb(row, rowIndex, columnasMap);
                break;
            case "validaciones":
                procesarFilaValidaciones(row, rowIndex, columnasMap);
                break;
            case "mla_envios":
                procesarFilaMlaEnvios(row, rowIndex, columnasMap);
                break;
            default:
                throw new IllegalArgumentException("Tipo de importación no soportado: " + tipo);
        }
    }


    private String obtenerValorCelda(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null)
            return null;

        return switch (cell.getCellType()) {
            case STRING -> {
                String valor = cell.getStringCellValue();
                yield valor != null ? valor.trim() : null;
            }
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Para fechas, retornar en formato DUX
                    java.util.Date fecha = cell.getDateCellValue();
                    LocalDateTime ldt = fecha.toInstant()
                            .atZone(ZoneId.of("America/Argentina/Buenos_Aires"))
                            .toLocalDateTime();
                    yield ldt.format(DateTimeFormatter.ofPattern("d/M/yyyy  HH:mm:ss"));
                } else {
                    // Para números, mantener decimales si los hay
                    double num = cell.getNumericCellValue();
                    if (num == (long) num) {
                        yield String.valueOf((long) num);
                    } else {
                        yield String.valueOf(num);
                    }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                // Evaluar fórmula correctamente
                try {
                    org.apache.poi.ss.usermodel.FormulaEvaluator evaluator = row.getSheet().getWorkbook()
                            .getCreationHelper().createFormulaEvaluator();
                    org.apache.poi.ss.usermodel.CellValue cellValue = evaluator.evaluate(cell);

                    if (cellValue == null) {
                        yield null;
                    }

                    String resultado = switch (cellValue.getCellType()) {
                        case STRING -> {
                            String valor = cellValue.getStringValue();
                            yield (valor != null && !valor.isBlank()) ? valor.trim() : null;
                        }
                        case NUMERIC -> {
                            double num = cellValue.getNumberValue();
                            if (num == (long) num) {
                                yield String.valueOf((long) num);
                            } else {
                                yield String.valueOf(num);
                            }
                        }
                        case BOOLEAN -> String.valueOf(cellValue.getBooleanValue());
                        case BLANK, ERROR -> null;
                        default -> null;
                    };
                    yield resultado;
                } catch (Exception e) {
                    // Si hay error al evaluar la fórmula, retornar null en lugar de la fórmula
                    log.debug("Error evaluando fórmula en celda {}{}: {}",
                            row.getRowNum() + 1, columnIndex, e.getMessage());
                    yield null;
                }
            }
            case BLANK -> null;
            default -> "";
        };
    }

    /**
     * Trunca un string a un máximo de caracteres
     */
    private String truncar(String valor, int maxLength) {
        if (valor == null) {
            return null;
        }
        if (valor.length() <= maxLength) {
            return valor;
        }
        return valor.substring(0, maxLength);
    }

    /**
     * Obtiene una fecha directamente de una celda de Excel
     * Si la celda es de tipo NUMERIC y está formateada como fecha, la convierte directamente
     * Si es STRING, intenta parsearla
     */
    private LocalDateTime obtenerFechaDeCelda(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }

        // Si la celda es NUMERIC y está formateada como fecha
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            try {
                java.util.Date fecha = cell.getDateCellValue();
                return fecha.toInstant().atZone(ZONA_ARG).toLocalDateTime();
            } catch (Exception e) {
                log.warn("Error al obtener fecha de celda numérica: {}", e.getMessage());
            }
        }

        // Si es una fórmula, evaluarla primero
        if (cell.getCellType() == CellType.FORMULA) {
            try {
                org.apache.poi.ss.usermodel.FormulaEvaluator evaluator = row.getSheet().getWorkbook()
                        .getCreationHelper().createFormulaEvaluator();
                org.apache.poi.ss.usermodel.CellValue cellValue = evaluator.evaluate(cell);
                if (cellValue != null && cellValue.getCellType() == CellType.NUMERIC) {
                    // Intentar como fecha
                    try {
                        java.util.Date fecha = DateUtil.getJavaDate(cellValue.getNumberValue());
                        return fecha.toInstant().atZone(ZONA_ARG).toLocalDateTime();
                    } catch (Exception e) {
                        // No es una fecha, continuar con parseo de string
                    }
                }
            } catch (Exception e) {
                log.warn("Error al evaluar fórmula de fecha: {}", e.getMessage());
            }
        }

        // Si es STRING o no se pudo obtener como fecha numérica, parsear el string
        String fechaStr = obtenerValorCelda(row, columnIndex);
        if (fechaStr != null && !fechaStr.isBlank()) {
            // Intentar diferentes formatos
            DateTimeFormatter[] formatos = {
                    DateTimeFormatter.ofPattern("d/M/yyyy  HH:mm:ss"),
                    DateTimeFormatter.ofPattern("M/d/yyyy  HH:mm:ss"),
                    DateTimeFormatter.ofPattern("d/M/yyyy HH:mm:ss"),
                    DateTimeFormatter.ofPattern("M/d/yyyy HH:mm:ss"),
                    DateTimeFormatter.ofPattern("d/M/yyyy"),
                    DateTimeFormatter.ofPattern("M/d/yyyy"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd")
            };

            for (DateTimeFormatter formato : formatos) {
                try {
                    return LocalDateTime.parse(fechaStr, formato);
                } catch (DateTimeParseException e) {
                    // Continuar con el siguiente formato
                }
            }

            // Si es una fecha de Excel (número serial)
            try {
                double numeroSerial = Double.parseDouble(fechaStr);
                return org.apache.poi.ss.usermodel.DateUtil.getLocalDateTime(numeroSerial);
            } catch (NumberFormatException e) {
                // No es un número serial
            }
        }

        return null;
    }

    private List<String> obtenerEncabezados(String tipo) {
        return switch (tipo.toLowerCase()) {
            case "catalogo" -> List.of("SKU", "PRODUCTO", "PVP", "UxB");
            case "productos" -> List.of("SKU", "Descripción", "Costo", "IVA", "Marca", "Origen");
            case "precios" -> List.of("Producto ID", "Canal ID", "PVP", "Margen %");
            default -> throw new IllegalArgumentException("Tipo de exportación no soportado: " + tipo);
        };
    }

    /**
     * Llena los datos en la hoja de Excel para exportación de catálogos
     * Filtra productos por: canal, catálogo y clasificación general (primer nivel)
     */
    private void llenarDatos(Sheet sheet, String tipo, Integer canalId, Integer catalogoId, Integer clasifGralId) {
        if (!"catalogo".equalsIgnoreCase(tipo)) {
            log.warn("Tipo de exportación '{}' no implementado completamente", tipo);
            return;
        }

        // Construir especificación de filtros
        List<Specification<Producto>> specs = new ArrayList<>();

        if (canalId != null) {
            specs.add(ProductoSpecifications.canalIds(List.of(canalId)));
        }

        if (catalogoId != null) {
            specs.add(ProductoSpecifications.catalogoIds(List.of(catalogoId)));
        }

        if (clasifGralId != null) {
            specs.add(ProductoSpecifications.clasifGralId(clasifGralId));
        }

        // Buscar productos que cumplan los filtros
        List<Producto> productos;
        if (specs.isEmpty()) {
            // Si no hay filtros, obtener todos los productos
            productos = productoRepository.findAll();
        } else {
            Specification<Producto> spec = specs.stream()
                    .reduce(Specification::and)
                    .orElse(null);
            productos = productoRepository.findAll(spec);
        }
        log.info("Productos encontrados para exportar: {}", productos.size());

        int rowNum = 1; // Empezar desde la fila 1 (la 0 es encabezados)
        for (Producto producto : productos) {
            Row row = sheet.createRow(rowNum++);

            // SKU
            Cell cellSku = row.createCell(0);
            cellSku.setCellValue(producto.getSku() != null ? producto.getSku() : "");

            // PRODUCTO (titulo_web o descripcion)
            Cell cellProducto = row.createCell(1);
            String productoNombre = producto.getTituloWeb();
            if (productoNombre == null || productoNombre.isBlank()) {
                productoNombre = producto.getDescripcion();
            }
            cellProducto.setCellValue(productoNombre != null ? productoNombre : "");

            // PVP (de producto_canal_precios)
            Cell cellPvp = row.createCell(2);
            if (canalId != null) {
                var precioOpt = productoCanalPrecioRepository
                        .findByProductoIdAndCanalId(producto.getId(), canalId);
                if (precioOpt.isPresent() && precioOpt.get().getPvp() != null) {
                    cellPvp.setCellValue(precioOpt.get().getPvp().doubleValue());
                } else {
                    cellPvp.setCellValue("");
                }
            } else {
                cellPvp.setCellValue("");
            }

            // UxB
            Cell cellUxb = row.createCell(3);
            if (producto.getUxb() != null) {
                cellUxb.setCellValue(producto.getUxb());
            } else {
                cellUxb.setCellValue("");
            }
        }
    }

    private CellStyle crearEstiloEncabezado(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * Determina el tipo de importación según el nombre de la hoja
     * Mapea nombres de hojas comunes a tipos de importación
     */
    private String determinarTipoPorNombreHoja(String nombreHoja) {
        String nombreNormalizado = nombreHoja.trim().toUpperCase();

        // Mapeo de nombres de hojas a tipos (solo las que se procesan en importarMigracionCompleta)
        return switch (nombreNormalizado) {
            case "MASTER" -> "master"; // Hoja principal con productos completos
            case "TITULOSWEB", "TITULOS WEB" -> "titulos_web"; // Actualizar títulos web
            case "VALIDACIONES" -> "validaciones"; // Entidades maestras (marcas, tipos, orígenes, clasificaciones)
            case "MLA-ENVIOS", "MLA ENVIOS" -> "mla_envios"; // MLAs y envíos
            default -> null; // Hoja no reconocida
        };
    }

    /**
     * Procesa todas las tablas estructuradas (ListObjects) de la hoja VALIDACIONES
     * Cada tabla se identifica por su nombre y se procesa según su tipo
     */
    private void procesarTablasEstructuradas(Sheet sheet) {
        // Solo funciona con XSSFSheet (archivos .xlsx)
        if (!(sheet instanceof XSSFSheet)) {
            log.warn("⚠️  Las tablas estructuradas solo están disponibles en archivos .xlsx. Saltando procesamiento de tablas estructuradas");
            return;
        }
        
        XSSFSheet xssfSheet = (XSSFSheet) sheet;
        List<XSSFTable> tablas = xssfSheet.getTables();
        
        if (tablas.isEmpty()) {
            log.info("No se encontraron tablas estructuradas en la hoja VALIDACIONES");
            return;
        }
        
        log.info("🔍 Encontradas {} tablas estructuradas en la hoja VALIDACIONES", tablas.size());
        for (XSSFTable tabla : tablas) {
            String nombreTabla = tabla.getName();
            if (nombreTabla != null) {
                log.info("  - Tabla: '{}'", nombreTabla);
                procesarTablaEstructurada(sheet, tabla, nombreTabla);
            }
        }
    }

    /**
     * Procesa una tabla estructurada (ListObject) específica de la hoja VALIDACIONES
     * Busca la tabla por nombre y procesa sus filas con su propio mapeo de columnas
     */
    private void procesarTablaEstructurada(Sheet sheet, XSSFTable tabla, String nombreTabla) {
        // Solo funciona con XSSFSheet (archivos .xlsx)
        if (!(sheet instanceof XSSFSheet)) {
            log.warn("⚠️  Las tablas estructuradas solo están disponibles en archivos .xlsx. Saltando procesamiento de tabla '{}'", nombreTabla);
            return;
        }
        
        // Obtener el rango de la tabla
        AreaReference areaRef = tabla.getArea();
        if (areaRef == null) {
            log.warn("⚠️  La tabla '{}' no tiene un rango válido", nombreTabla);
            return;
        }
        
        CellReference primeraCelda = areaRef.getFirstCell();
        CellReference ultimaCelda = areaRef.getLastCell();
        
        int filaInicio = primeraCelda.getRow();
        int filaFin = ultimaCelda.getRow();
        int colInicio = primeraCelda.getCol();
        int colFin = ultimaCelda.getCol();
        
        log.info("Tabla '{}' rango: filas {} a {}, columnas {} a {}", nombreTabla, 
                filaInicio + 1, filaFin + 1, colInicio + 1, colFin + 1);
        
        // Obtener el encabezado (primera fila del rango de datos, que es la segunda fila del área total)
        Row headerRow = sheet.getRow(filaInicio);
        if (headerRow == null) {
            log.warn("⚠️  No se encontró el encabezado de la tabla '{}' en fila {}", nombreTabla, filaInicio + 1);
            return;
        }
        
        // Mapear las columnas de esta tabla específica
        Map<String, Integer> columnasTabla = new HashMap<>();
        for (int colIdx = colInicio; colIdx <= colFin; colIdx++) {
            String nombreColumna = obtenerValorCelda(headerRow, colIdx);
            if (nombreColumna != null && !nombreColumna.isBlank()) {
                String nombreNormalizado = nombreColumna.trim().toUpperCase();
                columnasTabla.put(nombreNormalizado, colIdx);
            }
        }
        
        log.info("Columnas mapeadas para tabla '{}': {}", nombreTabla, columnasTabla.keySet());
        
        // Procesar las filas de datos (desde filaInicio + 1 hasta filaFin)
        int filasProcesadas = 0;
        for (int i = filaInicio + 1; i <= filaFin; i++) {
            Row row = sheet.getRow(i);
            if (row == null || esFilaVacia(row)) {
                continue;
            }
            
            try {
                procesarFilaTablaEstructurada(row, nombreTabla, columnasTabla, i);
                filasProcesadas++;
            } catch (Exception e) {
                log.warn("❌ Error procesando fila {} de tabla '{}': {}", i + 1, nombreTabla, e.getMessage());
            }
        }
        
        log.info("✓ Tabla estructurada '{}' procesada: {} filas procesadas", nombreTabla, filasProcesadas);
    }

    /**
     * Procesa una fila de una tabla estructurada según el tipo de tabla
     */
    private void procesarFilaTablaEstructurada(Row row, String nombreTabla, Map<String, Integer> columnasTabla, int filaIndex) {
        String nombreTablaUpper = nombreTabla.toUpperCase();
        
        try {
            // VMARCA/VLINEA - Marcas con jerarquía
            if (nombreTablaUpper.equals("VMARCA") || nombreTablaUpper.equals("VMARCAS")) {
                String marcaPadreNombreTemp = null;
                String marcaHijoNombreTemp = null;
                
                for (Map.Entry<String, Integer> entry : columnasTabla.entrySet()) {
                    String nombreCol = entry.getKey();
                    String valor = obtenerValorCelda(row, entry.getValue());
                    if (valor != null && !valor.isBlank()) {
                        if (nombreCol.equals("VMARCA")) {
                            marcaPadreNombreTemp = valor.trim();
                        } else if (nombreCol.equals("VLINEA")) {
                            marcaHijoNombreTemp = valor.trim();
                        }
                    }
                }
                
                final String marcaPadreNombre = marcaPadreNombreTemp;
                final String marcaHijoNombre = marcaHijoNombreTemp;
                
                if (marcaPadreNombre != null && !marcaPadreNombre.isBlank()) {
                    Marca marcaPadre = buscarOCrearMarca(marcaPadreNombre);
                    if (marcaPadre.getId() == null) {
                        marcaRepository.flush();
                        marcaPadre = marcaRepository.findByNombreIgnoreCase(marcaPadreNombre)
                                .orElseThrow(() -> new RuntimeException("No se pudo crear o encontrar marca: " + marcaPadreNombre));
                    }
                    
                    if (marcaHijoNombre != null && !marcaHijoNombre.isBlank()) {
                        Marca marcaHijo = buscarOCrearMarca(marcaHijoNombre);
                        if (marcaHijo.getId() == null) {
                            marcaRepository.flush();
                            marcaHijo = marcaRepository.findByNombreIgnoreCase(marcaHijoNombre)
                                    .orElseThrow(() -> new RuntimeException("No se pudo crear o encontrar marca: " + marcaHijoNombre));
                        }
                        Marca padreActual = marcaHijo.getPadre();
                        if (padreActual == null || !padreActual.getId().equals(marcaPadre.getId())) {
                            marcaHijo.setPadre(marcaPadre);
                            marcaRepository.save(marcaHijo);
                        }
                    }
                }
            }
            // MATERIAL - Materiales
            else if (nombreTablaUpper.equals("MATERIAL") || nombreTablaUpper.equals("MATERIALES")) {
                String materialNombre = null;
                for (Map.Entry<String, Integer> entry : columnasTabla.entrySet()) {
                    String nombreCol = entry.getKey();
                    if (nombreCol.equals("MATERIAL")) {
                        String valor = obtenerValorCelda(row, entry.getValue());
                        if (valor != null && !valor.isBlank()) {
                            materialNombre = valor.trim();
                            break;
                        }
                    }
                }
                if (materialNombre != null && !materialNombre.isBlank()) {
                    buscarOCrearMaterial(materialNombre);
                }
            }
            // PROVEEDOR - Proveedores
            else if (nombreTablaUpper.equals("PROVEEDOR") || nombreTablaUpper.equals("PROVEEDORES")) {
                String proveedorNombre = null;
                String alias = null;
                for (Map.Entry<String, Integer> entry : columnasTabla.entrySet()) {
                    String nombreCol = entry.getKey();
                    String valor = obtenerValorCelda(row, entry.getValue());
                    if (valor != null && !valor.isBlank()) {
                        if (nombreCol.equals("PROVEEDOR")) {
                            proveedorNombre = valor.trim();
                        } else if (nombreCol.equals("ALIAS")) {
                            alias = valor.trim();
                        }
                    }
                }
                if (proveedorNombre != null && !proveedorNombre.isBlank()) {
                    buscarOCrearProveedor(proveedorNombre, alias);
                }
            }
            // VORIGEN - Orígenes
            else if (nombreTablaUpper.equals("VORIGEN") || nombreTablaUpper.equals("ORIGEN")) {
                String origenNombre = null;
                for (Map.Entry<String, Integer> entry : columnasTabla.entrySet()) {
                    String nombreCol = entry.getKey();
                    if (nombreCol.equals("VORIGEN") || nombreCol.equals("ORIGEN")) {
                        String valor = obtenerValorCelda(row, entry.getValue());
                        if (valor != null && !valor.isBlank()) {
                            origenNombre = valor.trim();
                            break;
                        }
                    }
                }
                if (origenNombre != null && !origenNombre.isBlank()) {
                    buscarOCrearOrigen(origenNombre);
                }
            }
            // TIPO1 - Tabla estructurada TIPO1: Tipos nivel 1 (sin padre)
            // Estructura: TIPO1 (nombre) e ID TIPO2 (que contiene el ID del TIPO1)
            else if (nombreTablaUpper.equals("TIPO1")) {
                String tipoNombre = null;
                if (columnasTabla.containsKey("TIPO1")) {
                    tipoNombre = obtenerValorCelda(row, columnasTabla.get("TIPO1"));
                    if (tipoNombre != null && !tipoNombre.isBlank()) {
                        tipoNombre = tipoNombre.trim();
                    } else {
                        tipoNombre = null;
                    }
                }
                if (tipoNombre != null && !tipoNombre.isBlank()) {
                    buscarOCrearTipo(tipoNombre, null);
                }
            }
            // TIPO2 - Tabla estructurada TIPO2: Tipos nivel 2 (hijo de TIPO1)
            // Estructura: TIPO2 (nombre), ID TIPO1 (padre), ID TIPO2 (ID del TIPO2)
            else if (nombreTablaUpper.equals("TIPO2")) {
                String tipoNombre = null;
                Integer idTipo1 = null;
                
                if (columnasTabla.containsKey("TIPO2")) {
                    String valor = obtenerValorCelda(row, columnasTabla.get("TIPO2"));
                    if (valor != null && !valor.isBlank()) {
                        tipoNombre = valor.trim();
                    }
                }
                
                if (columnasTabla.containsKey("ID TIPO1")) {
                    String valor = obtenerValorCelda(row, columnasTabla.get("ID TIPO1"));
                    if (valor != null && !valor.isBlank()) {
                        try {
                            idTipo1 = Integer.parseInt(valor.trim());
                        } catch (NumberFormatException e) {
                            log.warn("⚠️  Fila {} tabla TIPO2 - ID TIPO1='{}' no es un número válido", 
                                    filaIndex + 1, valor);
                        }
                    }
                }
                
                if (tipoNombre != null && !tipoNombre.isBlank()) {
                    Tipo padre = null;
                    if (idTipo1 != null) {
                        padre = tipoRepository.findById(idTipo1).orElse(null);
                        if (padre == null) {
                            log.warn("⚠️  Fila {} tabla TIPO2 - ID TIPO1={} no encontrado para tipo='{}'", 
                                    filaIndex + 1, idTipo1, tipoNombre);
                        }
                    }
                    buscarOCrearTipo(tipoNombre, padre);
                }
            }
            // TIPO3 - Tabla estructurada TIPO3: Tipos nivel 3 (hijo de TIPO2)
            // Estructura: TIPO3 (nombre), ID TIPO2 (padre), ID TIPO3 (ID del TIPO3)
            else if (nombreTablaUpper.equals("TIPO3")) {
                String tipoNombre = null;
                Integer idTipo2 = null;
                
                if (columnasTabla.containsKey("TIPO3")) {
                    String valor = obtenerValorCelda(row, columnasTabla.get("TIPO3"));
                    if (valor != null && !valor.isBlank()) {
                        tipoNombre = valor.trim();
                    }
                }
                
                if (columnasTabla.containsKey("ID TIPO2")) {
                    String valor = obtenerValorCelda(row, columnasTabla.get("ID TIPO2"));
                    if (valor != null && !valor.isBlank()) {
                        try {
                            idTipo2 = Integer.parseInt(valor.trim());
                        } catch (NumberFormatException e) {
                            log.warn("⚠️  Fila {} tabla TIPO3 - ID TIPO2='{}' no es un número válido", 
                                    filaIndex + 1, valor);
                        }
                    }
                }
                
                if (tipoNombre != null && !tipoNombre.isBlank()) {
                    Tipo padre = null;
                    if (idTipo2 != null) {
                        padre = tipoRepository.findById(idTipo2).orElse(null);
                        if (padre == null) {
                            log.warn("⚠️  Fila {} tabla TIPO3 - ID TIPO2={} no encontrado para tipo='{}'", 
                                    filaIndex + 1, idTipo2, tipoNombre);
                        }
                    }
                    buscarOCrearTipo(tipoNombre, padre);
                }
            }
            // VCLASIF - Clasificaciones (VCLASIF1, VCLASIF2, VCLASIF3, VCLASIF4)
            else if (nombreTablaUpper.equals("VCLASIF") || nombreTablaUpper.contains("CLASIF")) {
                String clasif1Nombre = null;
                String clasif2Nombre = null;
                String clasif3Nombre = null;
                String clasif4Nombre = null;
                
                for (Map.Entry<String, Integer> entry : columnasTabla.entrySet()) {
                    String nombreCol = entry.getKey();
                    String valor = obtenerValorCelda(row, entry.getValue());
                    if (valor != null && !valor.isBlank()) {
                        if (nombreCol.equals("VCLASIF1")) {
                            clasif1Nombre = valor.trim();
                        } else if (nombreCol.equals("VCLASIF2")) {
                            clasif2Nombre = valor.trim();
                        } else if (nombreCol.equals("VCLASIF3")) {
                            clasif3Nombre = valor.trim();
                        } else if (nombreCol.equals("VCLASIF4")) {
                            clasif4Nombre = valor.trim();
                        }
                    }
                }
                
                // Procesar ClasifGral (VCLASIF1 y VCLASIF2)
                ClasifGral clasifGralPadre = null;
                if (clasif1Nombre != null && !clasif1Nombre.isBlank()) {
                    clasifGralPadre = buscarOCrearClasifGral(clasif1Nombre, null);
                }
                if (clasif2Nombre != null && !clasif2Nombre.isBlank()) {
                    buscarOCrearClasifGral(clasif2Nombre, clasifGralPadre);
                }
                
                // Procesar ClasifGastro (VCLASIF3 y VCLASIF4)
                ClasifGastro clasifGastroPadre = null;
                if (clasif3Nombre != null && !clasif3Nombre.isBlank()) {
                    clasifGastroPadre = buscarOCrearClasifGastro(clasif3Nombre, null);
                }
                if (clasif4Nombre != null && !clasif4Nombre.isBlank()) {
                    buscarOCrearClasifGastro(clasif4Nombre, clasifGastroPadre);
                }
            }
            else {
                log.debug("Tabla '{}' no tiene procesador específico, se omite", nombreTabla);
            }
        } catch (Exception e) {
            log.warn("❌ Error procesando fila {} de tabla '{}': {}", filaIndex + 1, nombreTabla, e.getMessage());
            throw e;
        }
    }

    /**
     * Procesa una hoja en su propia transacción para aislar errores
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    private ImportResultDTO procesarHojaEnTransaccionSeparada(Sheet sheet, String tipo, String nombreHoja) {
        log.debug("Iniciando transacción separada para hoja '{}' (tipo: '{}')", nombreHoja, tipo);
        try {
            log.debug("Ejecutando procesarHoja para hoja '{}' dentro de la transacción", nombreHoja);
            return procesarHoja(sheet, tipo, nombreHoja);
        } catch (Exception e) {
            log.error("Error dentro de la transacción para hoja '{}': {} - {}", nombreHoja, 
                    e.getMessage(), e.getClass().getSimpleName(), e);
            throw e; // @Transactional hará rollback automáticamente por rollbackFor = Exception.class
        }
    }

    /**
     * Procesa una hoja específica del Excel
     */
    private ImportResultDTO procesarHoja(Sheet sheet, String tipo, String nombreHoja) {
        log.info("Procesando hoja '{}' como tipo '{}'", nombreHoja, tipo);

        // Para la hoja MASTER, los encabezados reales están en la fila 1 (índice 1), no
        // en la 0
        int headerRowIndex = "master".equalsIgnoreCase(tipo) ? 1 : 0;
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) {
            throw new IllegalArgumentException("La hoja no tiene encabezados en la fila " + (headerRowIndex + 1));
        }

        Map<String, Integer> columnasMap = mapearColumnasPorNombre(headerRow);
        log.info("Columnas mapeadas para hoja '{}': {}", nombreHoja, columnasMap.keySet());
        
        // Contadores iniciales para rastrear entidades creadas (solo para VALIDACIONES)
        int marcasInicial = 0;
        int origenesInicial = 0;
        int materialesInicial = 0;
        int proveedoresInicial = 0;
        int tiposInicial = 0;
        int clasifGralInicial = 0;
        int clasifGastroInicial = 0;
        
        if ("validaciones".equalsIgnoreCase(tipo)) {
            marcasInicial = cacheMarcas.size();
            origenesInicial = cacheOrigenes.size();
            materialesInicial = cacheMateriales.size();
            proveedoresInicial = cacheProveedores.size();
            tiposInicial = cacheTipos.size();
            clasifGralInicial = cacheClasifGral.size();
            clasifGastroInicial = cacheClasifGastro.size();
            log.info("📊 Estado inicial de entidades relacionadas: {} marcas, {} orígenes, {} materiales, {} proveedores, {} tipos, {} clasifGral, {} clasifGastro",
                    marcasInicial, origenesInicial, materialesInicial, proveedoresInicial, 
                    tiposInicial, clasifGralInicial, clasifGastroInicial);
            
            // Procesar todas las tablas estructuradas (ListObjects) de la hoja VALIDACIONES
            log.info("🔍 Buscando y procesando todas las tablas estructuradas (ListObjects)...");
            procesarTablasEstructuradas(sheet);
        }
        
        // ====================================================================
        // FASE 1: PREPROCESAMIENTO - Crear todas las entidades relacionadas
        // ====================================================================
        // IMPORTANTE: Todas las entidades relacionadas deben estar creadas
        // ANTES de procesar las filas de productos para evitar errores de
        // foreign key y mejorar el rendimiento.
        //
        // Entidades que vienen de VALIDACIONES (ya procesadas antes):
        // - marcas, tipos, orígenes, materiales, proveedores, clasificaciones
        //
        // Entidades que se crean aquí (valores fijos):
        // - catálogos (LG GASTRO, LG HOGAR, LG HUDSON, KT GASTRO, DEPTOS)
        // - canales (ML, KT HOGAR, KT GASTRO, LINEA GE, LIZZY)
        // ====================================================================
        if ("master".equalsIgnoreCase(tipo)) {
            log.info("🔧 FASE 1: Preprocesando y creando entidades relacionadas para hoja MASTER...");
            preprocesarEntidadesRelacionadas(sheet, columnasMap);
            log.info("✅ FASE 1 completada: Todas las entidades relacionadas están listas");
        }

        // ====================================================================
        // FASE 2: PROCESAMIENTO - Procesar todas las filas de productos
        // ====================================================================
        List<String> errors = new ArrayList<>();
        int totalRows = 0;
        int successRows = 0;
        
        // Tamaño del batch para optimizar rendimiento (hacer flush cada N productos)
        int batchSize = 50;
        int productosEnBatch = 0;

        // Procesar datos: para MASTER desde la fila 3 (índice 2), para otras desde la
        // fila 2 (índice 1)
        int startRow = "master".equalsIgnoreCase(tipo) ? 2 : 1;
        int lastRowNum = sheet.getLastRowNum();
        
        // Límite de seguridad: máximo 100,000 filas para evitar loops infinitos
        int maxRows = Math.min(lastRowNum, startRow + 100000);
        int filasVaciasConsecutivas = 0;
        int maxFilasVaciasConsecutivas = 100; // Detener si hay 100 filas vacías consecutivas
        
        log.info("Procesando filas desde {} hasta {} (total: {} filas posibles, límite de seguridad: {}, batch size: {})", 
                startRow + 1, lastRowNum + 1, lastRowNum - startRow + 1, maxRows - startRow + 1, batchSize);
        
        for (int i = startRow; i <= maxRows; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                filasVaciasConsecutivas++;
                if (filasVaciasConsecutivas >= maxFilasVaciasConsecutivas) {
                    log.info("Deteniendo procesamiento: {} filas vacías consecutivas encontradas (última fila procesada: {})", 
                            filasVaciasConsecutivas, i);
                    break;
                }
                continue;
            }

            if (esFilaVacia(row)) {
                filasVaciasConsecutivas++;
                if (filasVaciasConsecutivas >= maxFilasVaciasConsecutivas) {
                    log.info("Deteniendo procesamiento: {} filas vacías consecutivas encontradas (última fila procesada: {})", 
                            filasVaciasConsecutivas, i);
                    break;
                }
                continue;
            }
            
            // Resetear contador de filas vacías si encontramos una fila con datos
            filasVaciasConsecutivas = 0;

            totalRows++;
            String skuFila = null;
            try {
                // Intentar obtener el SKU de la fila para mejor logging
                try {
                    Integer skuColIndex = columnasMap.get("SKU");
                    if (skuColIndex != null && row.getCell(skuColIndex) != null) {
                        String skuStr = obtenerValorCelda(row, skuColIndex);
                        if (skuStr != null && !skuStr.trim().isEmpty()) {
                            skuFila = skuStr.trim();
                        }
                    }
                } catch (Exception ex) {
                    // Ignorar errores al obtener SKU para logging
                }
                
                // Log cada 100 filas para ver el progreso
                if (totalRows % 100 == 0) {
                    log.info("📊 Procesando fila {} (total procesadas: {}, exitosas: {})", i + 1, totalRows, successRows);
                }
                
                // Log cada fila para debugging (solo las primeras 10 y luego cada 1000)
                if (totalRows <= 10 || totalRows % 1000 == 0) {
                    if (skuFila != null && !skuFila.isEmpty()) {
                        log.debug("📄 Procesando fila {} (SKU: {})", i + 1, skuFila);
                    } else {
                        log.debug("📄 Procesando fila {}", i + 1);
                    }
                }
                
                procesarFila(row, tipo, i, columnasMap);
                successRows++;
                productosEnBatch++;
                
                // Hacer flush cada batchSize productos para optimizar rendimiento
                // Esto permite que Hibernate agrupe múltiples inserts en un solo batch
                if (productosEnBatch >= batchSize) {
                    productoRepository.flush();
                    productosEnBatch = 0;
                    if (log.isTraceEnabled()) {
                        log.trace("Flush realizado después de procesar {} productos", successRows);
                    }
                }
            } catch (UnsupportedOperationException e) {
                // Si el tipo no está implementado, agregar error pero continuar
                String errorMsg = String.format("Fila %d%s: %s", i + 1, 
                        skuFila != null ? " (SKU: " + skuFila + ")" : "", e.getMessage());
                errors.add(errorMsg);
                log.warn("❌ ERROR en fila {} de hoja '{}'{}: {}", i + 1, nombreHoja,
                        skuFila != null ? " (SKU: " + skuFila + ")" : "", e.getMessage());
            } catch (DataIntegrityViolationException e) {
                // Manejar errores de integridad (duplicados, etc.) sin invalidar la transacción
                String errorMsg = String.format("Fila %d%s: Violación de integridad - %s", i + 1,
                        skuFila != null ? " (SKU: " + skuFila + ")" : "",
                        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                errors.add(errorMsg);
                log.error("❌ ERROR en fila {} de hoja '{}'{}: Violación de integridad - {}", i + 1, nombreHoja,
                        skuFila != null ? " (SKU: " + skuFila + ")" : "",
                        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                // Continuar con la siguiente fila
            } catch (AssertionFailure e) {
                // Si la sesión está invalidada, registrar el error y detener el procesamiento
                // de esta hoja
                String errorMsg = String.format("Fila %d%s: Sesión de Hibernate invalidada (posible error previo) - %s",
                        i + 1, skuFila != null ? " (SKU: " + skuFila + ")" : "",
                        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                errors.add(errorMsg);
                log.error("❌ ERROR CRÍTICO en fila {} de hoja '{}'{}: Sesión de Hibernate invalidada. Deteniendo procesamiento de esta hoja. Error: {}",
                        i + 1, nombreHoja, skuFila != null ? " (SKU: " + skuFila + ")" : "", e.getMessage());
                // No continuar procesando más filas si la sesión está invalidada
                break;
            } catch (RuntimeException e) {
                // Verificar si es un error de sesión invalidada (relanzado desde procesarFilaMaster)
                if (e.getMessage() != null && e.getMessage().contains("Sesión de Hibernate invalidada")) {
                    String errorMsg = String.format("Fila %d%s: %s", i + 1,
                            skuFila != null ? " (SKU: " + skuFila + ")" : "", e.getMessage());
                    errors.add(errorMsg);
                    log.error("❌ ERROR CRÍTICO en fila {} de hoja '{}'{}: Sesión de Hibernate invalidada. Deteniendo procesamiento de esta hoja. Error: {}",
                            i + 1, nombreHoja, skuFila != null ? " (SKU: " + skuFila + ")" : "", e.getMessage());
                    // No continuar procesando más filas si la sesión está invalidada
                    break;
                }
                // Si no es un error de sesión invalidada, tratarlo como error genérico
                String errorMsg = String.format("Fila %d%s: %s", i + 1,
                        skuFila != null ? " (SKU: " + skuFila + ")" : "", e.getMessage());
                errors.add(errorMsg);
                log.error("❌ ERROR en fila {} de hoja '{}'{}: {} - {}", i + 1, nombreHoja,
                        skuFila != null ? " (SKU: " + skuFila + ")" : "",
                        e.getMessage(), e.getClass().getSimpleName());
                if (log.isDebugEnabled()) {
                    log.debug("Stack trace completo:", e);
                }
            } catch (Exception e) {
                // Verificar si es un error de configuración (sesión invalidada)
                String exceptionName = e.getClass().getName();
                if (exceptionName.contains("ConfigurationPropertiesBindException") ||
                        (e.getMessage() != null && e.getMessage().contains("DataSourceProperties"))) {
                    String errorMsg = String.format("Fila %d%s: Error de configuración (sesión invalidada) - %s", i + 1,
                            skuFila != null ? " (SKU: " + skuFila + ")" : "",
                            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                    errors.add(errorMsg);
                    log.error("❌ ERROR CRÍTICO en fila {} de hoja '{}'{}: Error de configuración (sesión invalidada). Deteniendo procesamiento. Error: {}",
                            i + 1, nombreHoja, skuFila != null ? " (SKU: " + skuFila + ")" : "", e.getMessage());
                    // No continuar procesando más filas si la sesión está invalidada
                    break;
                }
                // Verificar si es un error de sesión cerrada
                if (e instanceof IllegalStateException &&
                        e.getMessage() != null && e.getMessage().contains("closed")) {
                    String errorMsg = String.format("Fila %d%s: Sesión de Hibernate cerrada", i + 1,
                            skuFila != null ? " (SKU: " + skuFila + ")" : "");
                    errors.add(errorMsg);
                    log.error("❌ ERROR CRÍTICO en fila {} de hoja '{}'{}: Sesión de Hibernate cerrada. Deteniendo procesamiento. Error: {}",
                            i + 1, nombreHoja, skuFila != null ? " (SKU: " + skuFila + ")" : "", e.getMessage());
                    // No continuar procesando más filas si la sesión está cerrada
                    break;
                }

                // Error genérico
                String errorMsg = String.format("Fila %d%s: %s", i + 1,
                        skuFila != null ? " (SKU: " + skuFila + ")" : "", e.getMessage());
                errors.add(errorMsg);
                log.error("❌ ERROR en fila {} de hoja '{}'{}: {} - {}", i + 1, nombreHoja,
                        skuFila != null ? " (SKU: " + skuFila + ")" : "",
                        e.getMessage(), e.getClass().getSimpleName());
                if (log.isDebugEnabled()) {
                    log.debug("Stack trace completo:", e);
                }
            }
        }
        
        // Flush final para asegurar que todos los productos pendientes se guarden
        if (productosEnBatch > 0) {
            productoRepository.flush();
            log.debug("Flush final realizado: {} productos pendientes guardados", productosEnBatch);
        }

        // Mostrar resumen de entidades relacionadas creadas (solo para VALIDACIONES)
        if ("validaciones".equalsIgnoreCase(tipo)) {
            int marcasFinal = cacheMarcas.size();
            int origenesFinal = cacheOrigenes.size();
            int materialesFinal = cacheMateriales.size();
            int proveedoresFinal = cacheProveedores.size();
            int tiposFinal = cacheTipos.size();
            int clasifGralFinal = cacheClasifGral.size();
            int clasifGastroFinal = cacheClasifGastro.size();
            
            int marcasCreadas = marcasFinal - marcasInicial;
            int origenesCreados = origenesFinal - origenesInicial;
            int materialesCreados = materialesFinal - materialesInicial;
            int proveedoresCreados = proveedoresFinal - proveedoresInicial;
            int tiposCreados = tiposFinal - tiposInicial;
            int clasifGralCreadas = clasifGralFinal - clasifGralInicial;
            int clasifGastroCreadas = clasifGastroFinal - clasifGastroInicial;
            
            log.info("📊 RESUMEN DE ENTIDADES RELACIONADAS CREADAS en hoja '{}':", nombreHoja);
            log.info("   ✓ Marcas: {} creadas (total en cache: {})", marcasCreadas, marcasFinal);
            log.info("   ✓ Orígenes: {} creados (total en cache: {})", origenesCreados, origenesFinal);
            log.info("   ✓ Materiales: {} creados (total en cache: {})", materialesCreados, materialesFinal);
            log.info("   ✓ Proveedores: {} creados (total en cache: {})", proveedoresCreados, proveedoresFinal);
            log.info("   ✓ Tipos: {} creados (total en cache: {})", tiposCreados, tiposFinal);
            log.info("   ✓ ClasifGral: {} creadas (total en cache: {})", clasifGralCreadas, clasifGralFinal);
            log.info("   ✓ ClasifGastro: {} creadas (total en cache: {})", clasifGastroCreadas, clasifGastroFinal);
        }

        if (errors.isEmpty()) {
            log.info("✅ Hoja '{}' procesada exitosamente: {} filas procesadas de {} totales", nombreHoja, successRows, totalRows);
            return ImportResultDTO.success(totalRows, successRows);
        } else {
            log.error("⚠️  Hoja '{}' procesada con errores: {} exitosas, {} con errores de {} totales", 
                    nombreHoja, successRows, errors.size(), totalRows);
            log.error("📋 RESUMEN DE ERRORES en hoja '{}':", nombreHoja);
            for (int j = 0; j < Math.min(errors.size(), 10); j++) {
                log.error("   {}. {}", j + 1, errors.get(j));
            }
            if (errors.size() > 10) {
                log.error("   ... y {} errores más (total: {})", errors.size() - 10, errors.size());
            }
            return ImportResultDTO.withErrors(totalRows, successRows, errors.size(), errors);
        }
    }

    /**
     * Preprocesa y crea todas las entidades relacionadas antes de procesar los productos.
     * Esto mejora el rendimiento al crear todas las entidades en batch primero.
     */
    private void preprocesarEntidadesRelacionadas(Sheet sheet, Map<String, Integer> columnasMap) {
        log.info("Iniciando preprocesamiento de entidades relacionadas...");
        
        // Nota: Las siguientes entidades ahora se obtienen de la hoja VALIDACIONES, no de MASTER:
        // - marcas (tabla VMARCAS)
        // - origenes (VORIGEN)
        // - materiales (tabla MATERIALES)
        // - proveedores (tabla PROVEEDORES)
        // - tipos (tabla TIPO1, TIPO2, TIPO3)
        // - clasificaciones (ClasifGral y ClasifGastro, tabla VCLASIF)
        // IMPORTANTE: La hoja VALIDACIONES debe procesarse ANTES que MASTER para que todas estas
        // entidades estén disponibles cuando se procesen los productos.
        
        // Segunda fase: crear todas las entidades en batch
        log.info("🔨 Creando entidades relacionadas...");
        
        // Nota: marcas, tipos, orígenes, materiales, proveedores y clasificaciones se procesan desde VALIDACIONES,
        // no desde aquí
        
        // Crear catálogos fijos
        String[] catalogosFijos = { "LG GASTRO", "LG HOGAR", "LG HUDSON", "KT GASTRO", "DEPTOS" };
        for (String nombre : catalogosFijos) {
            buscarOCrearCatalogo(nombre);
        }
        log.info("   ✓ {} catálogos fijos creados: {}", catalogosFijos.length, String.join(", ", catalogosFijos));
        
        // Crear canales fijos
        String[] canalesFijos = { "ML", "KT HOGAR", "KT GASTRO", "LINEA GE", "LIZZY" };
        for (String nombre : canalesFijos) {
            buscarOCrearCanal(nombre);
        }
        log.info("   ✓ {} canales fijos creados: {}", canalesFijos.length, String.join(", ", canalesFijos));
        
        // Flush final para asegurar que todas las entidades se guarden
        catalogoRepository.flush();
        canalRepository.flush();
        // marcaRepository, tipoRepository, origenRepository, materialRepository, proveedorRepository,
        // clasifGralRepository y clasifGastroRepository se flushan al procesar VALIDACIONES
        
        log.info("✅ Todas las entidades relacionadas creadas y guardadas");
    }

    /**
     * Importación única de migración: Importa TODO el Excel completo a la base de
     * datos
     * Este método está diseñado para ser usado UNA SOLA VEZ para migrar todos los
     * datos
     * del archivo SUPER MASTER.xlsm a la base de datos MySQL
     */
    @Override
    public ImportCompletoResultDTO importarMigracionCompleta(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        if (!isExcelFile(file)) {
            throw new IllegalArgumentException("El archivo debe ser un Excel (.xls o .xlsx)");
        }

        log.info("========================================");
        log.info("INICIANDO IMPORTACIÓN COMPLETA DE MIGRACIÓN");
        log.info("========================================");
        
        // Limpiar caches al inicio de la importación
        cacheMarcas.clear();
        cacheOrigenes.clear();
        cacheClasifGral.clear();
        cacheProveedores.clear();
        cacheTipos.clear();
        cacheCatalogos.clear();
        cacheCanales.clear();
        cacheMateriales.clear();

        try (Workbook workbook = abrirWorkbookEficiente(file)) {
            int totalHojas = workbook.getNumberOfSheets();
            log.info("Archivo contiene {} hojas", totalHojas);

            // Listar todas las hojas
            for (int i = 0; i < totalHojas; i++) {
                log.info("  - Hoja {}: '{}'", i + 1, workbook.getSheetName(i));
            }

            Map<String, ImportResultDTO> resultadosPorHoja = new LinkedHashMap<>();
            List<String> erroresGenerales = new ArrayList<>();
            int hojasProcesadas = 0;
            int hojasConErrores = 0;
            int hojasOmitidas = 0;

            // Recolectar hojas a procesar y ordenarlas: VALIDACIONES primero, luego MASTER
            List<SheetInfo> hojasAProcesar = new ArrayList<>();
            for (int i = 0; i < totalHojas; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String nombreHoja = sheet.getSheetName();
                String nombreNormalizado = nombreHoja.trim().toUpperCase();
                
                if ("VALIDACIONES".equals(nombreNormalizado) || "MASTER".equals(nombreNormalizado)) {
                    // Prioridad: VALIDACIONES = 0, MASTER = 1 (para ordenar)
                    int prioridad = "VALIDACIONES".equals(nombreNormalizado) ? 0 : 1;
                    hojasAProcesar.add(new SheetInfo(sheet, nombreHoja, prioridad));
                } else {
                    hojasOmitidas++;
                }
            }
            
            // Ordenar por prioridad (VALIDACIONES primero)
            hojasAProcesar.sort(Comparator.comparingInt(SheetInfo::prioridad));
            
            log.info("Hojas a procesar (en orden): {}", 
                    hojasAProcesar.stream().map(SheetInfo::nombre).collect(Collectors.toList()));

            // Procesar cada hoja en el orden correcto
            for (int idx = 0; idx < hojasAProcesar.size(); idx++) {
                SheetInfo sheetInfo = hojasAProcesar.get(idx);
                Sheet sheet = sheetInfo.sheet();
                String nombreHoja = sheetInfo.nombre();

                log.info("\n>>> Procesando hoja {} de {}: '{}'", idx + 1, hojasAProcesar.size(), nombreHoja);

                try {
                    // Determinar el tipo de importación según el nombre de la hoja
                    String tipo = determinarTipoPorNombreHoja(nombreHoja);

                    if (tipo == null) {
                        log.warn("⚠️  Hoja '{}' no reconocida (determinarTipoPorNombreHoja devolvió null), se omite", nombreHoja);
                        hojasOmitidas++;
                        continue;
                    }

                    log.info("   ✓ Tipo detectado: '{}'", tipo);

                    // Procesar la hoja en su propia transacción
                    log.info("   🔄 Iniciando procesamiento de hoja '{}' en transacción separada...", nombreHoja);
                    ImportResultDTO resultado = procesarHojaEnTransaccionSeparada(sheet, tipo, nombreHoja);
                    resultadosPorHoja.put(nombreHoja, resultado);

                    log.info("   ✓ Procesamiento de hoja '{}' completado. Resultado: {} exitosas, {} con errores, {} totales",
                            nombreHoja, resultado.successRows(), resultado.errorRows(), resultado.totalRows());

                    if (resultado.errorRows() > 0) {
                        hojasConErrores++;
                        log.warn("   ⚠️  Hoja '{}' procesada con errores: {} exitosas, {} con errores",
                                nombreHoja, resultado.successRows(), resultado.errorRows());
                    } else {
                        hojasProcesadas++;
                        log.info("   ✅ Hoja '{}' procesada exitosamente: {} filas", nombreHoja, resultado.successRows());
                    }

                } catch (UnsupportedOperationException e) {
                    log.error("   ❌ ERROR: Tipo no implementado para hoja '{}': {}", nombreHoja, e.getMessage());
                    erroresGenerales.add(String.format("Hoja '%s': %s", nombreHoja, e.getMessage()));
                    hojasConErrores++;

                    resultadosPorHoja.put(nombreHoja, ImportResultDTO.withErrors(
                            0, 0, 0, List.of(e.getMessage())));
                } catch (Exception e) {
                    log.error("   ❌ ERROR CRÍTICO procesando hoja '{}': {} - {}", nombreHoja, 
                            e.getMessage(), e.getClass().getSimpleName(), e);
                    erroresGenerales.add(String.format("Hoja '%s': %s", nombreHoja, e.getMessage()));
                    hojasConErrores++;

                    resultadosPorHoja.put(nombreHoja, ImportResultDTO.withErrors(
                            0, 0, 0, List.of(e.getMessage())));
                }
            }

            // Resumen final
            log.info("\n========================================");
            log.info("RESUMEN DE IMPORTACIÓN");
            log.info("========================================");
            log.info("Total de hojas: {}", totalHojas);
            log.info("Hojas procesadas exitosamente: {}", hojasProcesadas);
            log.info("Hojas con errores: {}", hojasConErrores);
            log.info("Hojas omitidas (no reconocidas): {}", hojasOmitidas);
            log.info("========================================\n");

            // Generar resultado final
            if (erroresGenerales.isEmpty() && hojasConErrores == 0) {
                return ImportCompletoResultDTO.success(totalHojas, hojasProcesadas, resultadosPorHoja);
            } else {
                return ImportCompletoResultDTO.withErrors(
                        totalHojas, hojasProcesadas, hojasConErrores, resultadosPorHoja, erroresGenerales);
            }
        }
    }

    // ============================================================
    // MÉTODOS DE PROCESAMIENTO PARA MIGRACIÓN COMPLETA
    // ============================================================

    /**
     * Procesa una fila de la hoja MASTER (productos completos)
     * Columnas esperadas: SKU, PRODUCTO, TITULO WEB, TIPO DE PRODUCTO, CODEXT, UxB,
     * MARCA, LINEA, TIPO, ORIGEN
     */
    private void procesarFilaMaster(Row row, int rowIndex, Map<String, Integer> columnasMap) {
        try {
            String skuRaw = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "SKU"));
            if (skuRaw == null || skuRaw.isBlank()) {
                throw new IllegalArgumentException("SKU es requerido en fila " + (rowIndex + 1));
            }

            // Normalizar SKU (eliminar .0 si es un número decimal)
            String sku = skuRaw.trim();
            if (sku.endsWith(".0")) {
                sku = sku.substring(0, sku.length() - 2);
            }

            // Hacer sku final para usar en lambda
            final String skuFinal = sku;

            // Log de inicio de procesamiento (solo para debugging)
            if (log.isTraceEnabled()) {
                log.trace("Iniciando procesamiento de fila {} (SKU: {})", rowIndex + 1, skuFinal);
            }

            // Buscar o crear producto
            Optional<Producto> productoOpt;
            try {
                productoOpt = productoRepository.findBySku(skuFinal);
            } catch (org.hibernate.AssertionFailure e) {
                // Si la sesión está invalidada, relanzar para detener el procesamiento de la hoja
                log.error("Sesión de Hibernate invalidada al buscar producto SKU {} en fila {}: {}. Deteniendo procesamiento de esta hoja.",
                        skuFinal, rowIndex + 1, e.getMessage());
                throw new RuntimeException("Sesión de Hibernate invalidada - deteniendo procesamiento de hoja", e);
            }
            boolean esNuevo = productoOpt.isEmpty();
            Producto producto = productoOpt.orElseGet(() -> {
                Producto nuevo = new Producto();
                nuevo.setSku(skuFinal);
                // Establecer valores por defecto para campos requeridos
                nuevo.setIva(BigDecimal.ZERO); // IVA por defecto
                nuevo.setFechaCreacion(LocalDateTime.now(ZONA_ARG)); // Fecha de creación
                // Valores por defecto para campos @NotNull (se actualizarán si vienen en el
                // Excel)
                nuevo.setDescripcion(""); // Valor por defecto mínimo
                nuevo.setTituloWeb(""); // Valor por defecto mínimo
                return nuevo;
            });

            // PRODUCTO (descripcion) - máximo 100 caracteres
            if (columnasMap.containsKey("PRODUCTO")) {
                String descripcion = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "PRODUCTO"));
                if (descripcion != null && !descripcion.isBlank()) {
                    producto.setDescripcion(truncar(descripcion.trim(), 100));
                } else if (esNuevo && (producto.getDescripcion() == null || producto.getDescripcion().isEmpty())) {
                    // Si es nuevo y no tiene descripción, usar valor por defecto mínimo
                    producto.setDescripcion("");
                }
            } else if (esNuevo && (producto.getDescripcion() == null || producto.getDescripcion().isEmpty())) {
                // Si es nuevo y no viene la columna, usar valor por defecto mínimo
                producto.setDescripcion("");
            }

            // TITULO WEB - máximo 100 caracteres
            if (columnasMap.containsKey("TITULO WEB")) {
                String tituloWeb = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "TITULO WEB"));
                if (tituloWeb != null && !tituloWeb.isBlank()) {
                    producto.setTituloWeb(truncar(tituloWeb.trim(), 100));
                } else if (esNuevo && (producto.getTituloWeb() == null || producto.getTituloWeb().isEmpty())) {
                    // Si es nuevo y no tiene título web, usar valor por defecto mínimo
                    producto.setTituloWeb("");
                }
            } else if (esNuevo && (producto.getTituloWeb() == null || producto.getTituloWeb().isEmpty())) {
                // Si es nuevo y no viene la columna, usar valor por defecto mínimo
                producto.setTituloWeb("");
            }

            // TIPO DE PRODUCTO (SIMPLE o COMBO) - Guardar 1 si es COMBO, 0 si es SIMPLE
            if (columnasMap.containsKey("TIPO DE PRODUCTO")) {
                String tipoProducto = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "TIPO DE PRODUCTO"));
                if (tipoProducto != null && !tipoProducto.isBlank()) {
                    String tipoUpper = tipoProducto.trim().toUpperCase();
                    // Guardar true (1) si es COMBO, false (0) si es SIMPLE
                    producto.setEsCombo("COMBO".equals(tipoUpper));
                }
            }

            // CODEXT - máximo 45 caracteres
            if (columnasMap.containsKey("CODEXT")) {
                String codExt = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "CODEXT"));
                if (codExt != null && !codExt.isBlank()) {
                    producto.setCodExt(truncar(codExt.trim(), 45));
                }
            }

            // UxB
            if (columnasMap.containsKey("UXB") || columnasMap.containsKey("UxB")) {
                String columnaUxb = columnasMap.containsKey("UXB") ? "UXB" : "UxB";
                String uxbStr = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, columnaUxb));
                if (uxbStr != null && !uxbStr.isBlank()) {
                    try {
                        Integer uxb = Integer.parseInt(uxbStr.trim());
                        if (uxb > 0) {
                            producto.setUxb(uxb);
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("UXB inválido: " + uxbStr);
                    }
                }
            }

            // MARCA
            if (columnasMap.containsKey("MARCA")) {
                String marcaNombre = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "MARCA"));
                if (marcaNombre != null && !marcaNombre.isBlank()) {
                    Marca marca = buscarOCrearMarca(marcaNombre.trim());
                    producto.setMarca(marca);
                }
            }

            // TIPO - Buscar o crear tipo
            Tipo tipoProducto = null;
            if (columnasMap.containsKey("TIPO")) {
                String tipoNombre = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "TIPO"));
                if (tipoNombre != null && !tipoNombre.isBlank()) {
                    tipoProducto = buscarOCrearTipo(tipoNombre.trim());
                }
            }
            // Si no hay tipo del Excel, buscar o crear uno por defecto
            if (tipoProducto == null) {
                tipoProducto = buscarOCrearTipoPorId(0, "SIN TIPO");
            }
            producto.setTipo(tipoProducto);

            // ORIGEN - Campo requerido @NotNull, establecer valor por defecto si no viene
            if (columnasMap.containsKey("ORIGEN")) {
                String origenNombre = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "ORIGEN"));
                if (origenNombre != null && !origenNombre.isBlank()) {
                    Origen origen = buscarOCrearOrigen(origenNombre.trim());
                    producto.setOrigen(origen);
                }
            }
            // Si no tiene origen y es nuevo, establecer uno por defecto (requerido por
            // @NotNull)
            if (producto.getOrigen() == null) {
                Origen origenDefault = buscarOCrearOrigen("SIN ORIGEN");
                producto.setOrigen(origenDefault);
            }

            // MATERIAL
            if (columnasMap.containsKey("MATERIAL")) {
                String materialNombre = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "MATERIAL"));
                if (materialNombre != null && !materialNombre.isBlank()) {
                    Material material = buscarOCrearMaterial(materialNombre.trim());
                    producto.setMaterial(material);
                }
            }

            // CLASIF1, CLASIF2, CLASIF3, CLASIF4 (clasificaciones jerárquicas)
            // La última clasificación (CLASIF4 o la última disponible) se asigna al
            // producto
            ClasifGral padre = null;
            ClasifGral ultimaClasif = null;
            for (int i = 1; i <= 4; i++) {
                String columna = "CLASIF" + i;
                if (columnasMap.containsKey(columna)) {
                    String clasifNombre = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, columna));
                    if (clasifNombre != null && !clasifNombre.isBlank()) {
                        ClasifGral clasif = buscarOCrearClasifGral(clasifNombre.trim(), padre);
                        padre = clasif; // El siguiente nivel será hijo de este
                        ultimaClasif = clasif; // Guardar la última clasificación procesada
                    }
                }
            }
            // Asignar la última clasificación al producto
            // Campo requerido @NotNull, establecer valor por defecto si no viene
            if (ultimaClasif != null) {
                producto.setClasifGral(ultimaClasif);
            } else if (producto.getClasifGral() == null) {
                // Si no tiene clasificación, establecer una por defecto (requerido por
                // @NotNull)
                ClasifGral clasifDefault = buscarOCrearClasifGral("SIN CLASIFICACION");
                producto.setClasifGral(clasifDefault);
            }

            // PROVEEDOR - Crear si no existe
            if (columnasMap.containsKey("PROVEEDOR")) {
                String proveedorNombre = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "PROVEEDOR"));
                if (proveedorNombre != null && !proveedorNombre.isBlank()) {
                    Proveedor proveedor = buscarOCrearProveedor(proveedorNombre.trim());
                    producto.setProveedor(proveedor);
                }
            }

            // COSTO
            if (columnasMap.containsKey("COSTO")) {
                String costoStr = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "COSTO"));
                if (costoStr != null && !costoStr.isBlank()) {
                    try {
                        BigDecimal costo = new BigDecimal(costoStr.replace(",", "."));
                        producto.setCosto(costo);
                    } catch (NumberFormatException e) {
                        log.warn("Costo inválido para producto SKU {}: {}", skuFinal, costoStr);
                    }
                }
            }

            // ULTIMA ACT. COSTO - fecha_ult_costo
            if (columnasMap.containsKey("ULTIMA ACT. COSTO")) {
                try {
                    LocalDateTime fechaUltCosto = obtenerFechaDeCelda(row,
                            obtenerIndiceColumna(columnasMap, "ULTIMA ACT. COSTO"));
                    if (fechaUltCosto != null) {
                        producto.setFechaUltCosto(fechaUltCosto);
                    }
                } catch (Exception e) {
                    log.warn("Fecha de última actualización de costo inválida para producto SKU {}: {}",
                            skuFinal, e.getMessage());
                }
            }

            // IVA - Campo requerido, establecer si no está definido
            // NOTA: En el Excel el IVA viene como decimal (ej: 0.21), se multiplica por 100 para convertirlo a porcentaje (21)
            if (producto.getIva() == null) {
                // Intentar obtener IVA del Excel si existe la columna "IVA"
                if (columnasMap.containsKey("IVA")) {
                    String ivaStr = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "IVA"));
                    if (ivaStr != null && !ivaStr.isBlank()) {
                        try {
                            BigDecimal ivaDecimal = new BigDecimal(ivaStr.replace(",", "."));
                            // Multiplicar por 100 para convertir de decimal (0.21) a porcentaje (21)
                            BigDecimal iva = ivaDecimal.multiply(BigDecimal.valueOf(100));
                            // Validar que el IVA esté entre 0 y 100 (después de multiplicar)
                            if (iva.compareTo(BigDecimal.ZERO) < 0 || iva.compareTo(new BigDecimal("100")) > 0) {
                                log.warn("IVA fuera de rango para producto SKU {}: {} (convertido a {}%), usando 0", 
                                        skuFinal, ivaStr, iva);
                                producto.setIva(BigDecimal.ZERO);
                            } else {
                                producto.setIva(iva);
                            }
                        } catch (NumberFormatException e) {
                            log.warn("IVA inválido para producto SKU {}, usando 0: {}", skuFinal, ivaStr);
                            producto.setIva(BigDecimal.ZERO);
                        }
                    } else {
                        producto.setIva(BigDecimal.ZERO);
                    }
                } else {
                    producto.setIva(BigDecimal.ZERO); // Valor por defecto
                }
            } else {
                // Si ya tiene IVA pero existe la columna, actualizarlo
                if (columnasMap.containsKey("IVA")) {
                    String ivaStr = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "IVA"));
                    if (ivaStr != null && !ivaStr.isBlank()) {
                        try {
                            BigDecimal ivaDecimal = new BigDecimal(ivaStr.replace(",", "."));
                            // Multiplicar por 100 para convertir de decimal (0.21) a porcentaje (21)
                            BigDecimal iva = ivaDecimal.multiply(BigDecimal.valueOf(100));
                            // Validar que el IVA esté entre 0 y 100 (después de multiplicar)
                            if (iva.compareTo(BigDecimal.ZERO) >= 0 && iva.compareTo(new BigDecimal("100")) <= 0) {
                                producto.setIva(iva);
                            } else {
                                log.warn("IVA fuera de rango para producto SKU {}: {} (convertido a {}%), manteniendo valor anterior",
                                        skuFinal, ivaStr, iva);
                            }
                        } catch (NumberFormatException e) {
                            log.warn("IVA inválido para producto SKU {}: {}, manteniendo valor anterior", skuFinal,
                                    ivaStr);
                        }
                    }
                }
            }

            // Fecha de creación - Solo para productos nuevos
            if (producto.getFechaCreacion() == null) {
                producto.setFechaCreacion(LocalDateTime.now(ZONA_ARG));
            }

            // Guardar producto
            Producto productoGuardado = productoRepository.save(producto);
            
            // Si el producto es nuevo (sin ID) y necesitamos crear relaciones que requieren el ID,
            // hacer flush inmediatamente para obtener el ID. Para productos existentes, el ID ya está disponible.
            // Esto optimiza el rendimiento: solo hacemos flush cuando es absolutamente necesario.
            boolean productoEsNuevo = productoGuardado.getId() == null;
            boolean tieneRelaciones = columnasMap.containsKey("MLA") || 
                    columnasMap.keySet().stream().anyMatch(k -> 
                        k.contains("LG ") || k.contains("KT ") || k.contains("DEPTOS") || k.equals("CANAL"));
            
            if (productoEsNuevo && tieneRelaciones) {
                // Flush inmediato solo para productos nuevos que necesitan relaciones
                productoRepository.flush();
                // Si aún no tiene ID después del flush, buscar nuevamente
                if (productoGuardado.getId() == null) {
                    productoGuardado = productoRepository.findBySku(skuFinal)
                            .orElseThrow(() -> new RuntimeException(
                                    "No se pudo obtener el producto guardado con SKU: " + skuFinal));
                }
            }
            
            final Producto productoFinal = productoGuardado; // Variable final para usar en lambdas

            // MIX DE PRODUCTOS - Catalogos (LG GASTRO, LG HOGAR, LG HUDSON, KT GASTRO,
            // DEPTOS)
            // Crear relaciones ProductoCatalogo si el valor de la columna es "VERDADERO" o "TRUE"
            String[] nombresCatalogos = { "LG GASTRO", "LG HOGAR", "LG HUDSON", "KT GASTRO", "DEPTOS" };
            for (String nombreCatalogo : nombresCatalogos) {
                try {
                    if (columnasMap.containsKey(nombreCatalogo)) {
                        String valor = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, nombreCatalogo));
                        // Solo crear la relación si el valor es "VERDADERO" o "TRUE"
                        if (valor != null && !valor.isBlank() && 
                            (valor.equalsIgnoreCase("VERDADERO") || valor.equalsIgnoreCase("TRUE"))) {
                            Catalogo catalogoTemp = buscarOCrearCatalogo(nombreCatalogo);
                            // Asegurar que el catálogo tenga ID
                            if (catalogoTemp.getId() == null) {
                                catalogoTemp = catalogoRepository.save(catalogoTemp);
                            }
                            final Catalogo catalogo = catalogoTemp; // Variable final para usar en lambda
                            // Verificar si la relación ya existe
                            boolean existe = productoCatalogoRepository.findByProductoId(productoFinal.getId())
                                    .stream()
                                    .anyMatch(pc -> pc.getCatalogo().getId().equals(catalogo.getId()));
                            if (!existe && productoFinal.getId() != null && catalogo.getId() != null) {
                                ProductoCatalogo productoCatalogo = new ProductoCatalogo(productoFinal, catalogo);
                                productoCatalogoRepository.save(productoCatalogo);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error procesando catálogo '{}' para producto SKU {}: {}", nombreCatalogo, skuFinal,
                            e.getMessage(), e);
                    // Continuar con el siguiente catálogo sin fallar toda la fila
                }
            }

            // MLA - mlas.mla y mlas.precio_envio
            if (columnasMap.containsKey("MLA")) {
                try {
                    String mlaStr = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "MLA"));
                    if (mlaStr != null && !mlaStr.isBlank()) {
                        // Buscar si ya existe un MLA con este código
                        Optional<Mla> mlaExistente = mlaRepository.findByMla(mlaStr.trim());

                        Mla mla;
                        if (mlaExistente.isPresent()) {
                            mla = mlaExistente.get();
                        } else {
                            mla = new Mla();
                            mla.setMla(mlaStr.trim());
                        }

                        // ENVIO (precio_envio)
                        if (columnasMap.containsKey("ENVIO")) {
                            String envioStr = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "ENVIO"));
                            if (envioStr != null && !envioStr.isBlank()) {
                                try {
                                    BigDecimal precioEnvio = new BigDecimal(envioStr.replace(",", "."));
                                    mla.setPrecioEnvio(precioEnvio);
                                } catch (NumberFormatException e) {
                                    log.warn("Precio de envío inválido para producto SKU {}: {}", skuFinal, envioStr);
                                }
                            }
                        }

                        mlaRepository.save(mla);
                        // Asignar MLA al producto
                        productoFinal.setMla(mla);
                        productoRepository.save(productoFinal);
                    }
                } catch (Exception e) {
                    log.warn("Error procesando MLA para producto SKU {}: {}", skuFinal, e.getMessage());
                    // Continuar sin fallar toda la fila
                }
            }

            // Canales (ML, KT HOGAR, KT GASTRO, LINEA GE, LIZZY)
            // Asociar TODOS los productos con TODOS los canales
            String[] nombresCanales = { "ML", "KT HOGAR", "KT GASTRO", "LINEA GE", "LIZZY" };
            // Crear ProductoCanal solo si no existe (ahora es 1 por producto, no por producto+canal)
            if (productoFinal.getId() != null) {
                Optional<ProductoCanal> productoCanalOpt = productoCanalRepository
                        .findByProductoId(productoFinal.getId());
                if (productoCanalOpt.isEmpty()) {
                    ProductoCanal productoCanal = new ProductoCanal();
                    productoCanal.setProducto(productoFinal);
                    // Valores por defecto
                    productoCanal.setMargenMinorista(BigDecimal.ZERO);
                    productoCanal.setMargenMayorista(BigDecimal.ZERO);
                    productoCanalRepository.save(productoCanal);
                }
            }
            // Asegurar que los canales existen (sin crear ProductoCanal por cada uno)
            for (String nombreCanal : nombresCanales) {
                try {
                    Canal canal = buscarOCrearCanal(nombreCanal);
                    if (canal.getId() == null) {
                        canalRepository.save(canal);
                    }
                } catch (Exception e) {
                    log.warn("Error procesando canal '{}' para producto SKU {}: {}", nombreCanal, skuFinal,
                            e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error en procesarFilaMaster, fila {}: {}", rowIndex + 1, e.getMessage(), e);
            throw new RuntimeException("Error procesando fila " + (rowIndex + 1) + ": " + e.getMessage(), e);
        }
    }

    private ClasifGastro buscarOCrearClasifGastro(String nombre, ClasifGastro padre) {
        // IMPORTANTE: La restricción UNIQUE está solo en 'nombre', no en (nombre, id_padre)
        // Por lo tanto, solo puede haber UNA clasificación con ese nombre,
        // independientemente del padre
        
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        String nombreNormalizado = nombre.trim();
        String key = nombreNormalizado.toUpperCase();
        return cacheClasifGastro.computeIfAbsent(key, k -> {
            // Primero buscar por nombre (sin considerar el padre, ya que la restricción es solo en nombre)
            Optional<ClasifGastro> existente = clasifGastroRepository.findByNombreIgnoreCase(nombreNormalizado);

            if (existente.isPresent()) {
                // Si existe, verificar si el padre coincide. Si no coincide, usar la existente de todas formas
                // (porque la restricción UNIQUE no permite crear otra con el mismo nombre)
                ClasifGastro encontrada = existente.get();
                if (padre != null && encontrada.getPadre() != null
                        && !encontrada.getPadre().getId().equals(padre.getId())) {
                    log.warn("Clasificación Gastro '{}' existe con padre diferente. Usando la existente (ID: {})",
                            nombreNormalizado, encontrada.getId());
                } else if (padre == null && encontrada.getPadre() != null) {
                    log.warn("Clasificación Gastro '{}' existe con padre. Usando la existente (ID: {})",
                            nombreNormalizado, encontrada.getId());
                } else if (padre != null && encontrada.getPadre() == null) {
                    log.warn("Clasificación Gastro '{}' existe sin padre pero se esperaba con padre. Usando la existente (ID: {})",
                            nombreNormalizado, encontrada.getId());
                }
                return encontrada;
            }

            // Si no existe, intentar crear
            try {
                ClasifGastro nueva = new ClasifGastro();
                nueva.setNombre(nombreNormalizado);
                nueva.setPadre(padre);
                return clasifGastroRepository.save(nueva);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // Si falla por duplicado (puede haber sido creado por otra fila en paralelo), buscar nuevamente
                log.debug("Intento de crear clasificación Gastro duplicada '{}', buscando existente...", nombreNormalizado);
                return clasifGastroRepository.findByNombreIgnoreCase(nombreNormalizado)
                        .orElseThrow(() -> new RuntimeException(
                                "Error al crear clasificación Gastro '" + nombreNormalizado + "': " + e.getMessage()));
            }
        });
    }

    /**
     * Procesa una fila de la hoja TitulosWeb
     * Columnas: SKU, Nombre
     */
    private void procesarFilaTitulosWeb(Row row, int rowIndex, Map<String, Integer> columnasMap) {
        String sku = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "SKU"));
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU es requerido");
        }

        Producto producto = productoRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Producto con SKU '" + sku + "' no encontrado"));

        String nombre = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "NOMBRE"));
        if (nombre != null && !nombre.isBlank()) {
            producto.setTituloWeb(truncar(nombre.trim(), 100));
            productoRepository.save(producto);
        }
    }

    /**
     * Procesa una fila de la hoja Validaciones (entidades maestras)
     * 
     * Tabla VMARCAS:
     *   - VMARCA (primer nivel/padre), VLINEA (segundo nivel/hijo)
     * 
     * Tabla MATERIALES:
     *   - MATERIAL
     * 
     * Tabla PROVEEDORES:
     *   - PROVEEDOR, ALIAS (para apodo)
     * 
     * Tabla TIPOS:
     *   - TIPO1 (nivel 1, sin padre)
     *   - TIPO2 (nivel 2, hijo de TIPO1), columna ID TIPO1 indica el padre
     *   - TIPO3 (nivel 3, hijo de TIPO2), columna ID TIPO2 indica el padre
     * 
     * Tabla VCLASIF:
     *   - VCLASIF1, VCLASIF2 → ClasifGral (VCLASIF2 es hija de VCLASIF1)
     *   - VCLASIF3, VCLASIF4 → ClasifGastro (VCLASIF4 es hija de VCLASIF3)
     * 
     * Otras:
     *   - VORIGEN
     */
    private void procesarFilaValidaciones(Row row, int rowIndex, Map<String, Integer> columnasMap) {
        List<String> erroresFila = new ArrayList<>();
        
        try {
            // VMARCA y VLINEA - Procesar marcas con jerarquía
            // VMARCA es el primer nivel (padre), VLINEA es el segundo nivel (hijo)
            if (columnasMap.containsKey("VMARCA")) {
                String marcaPadreNombre = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "VMARCA"));
                if (marcaPadreNombre != null && !marcaPadreNombre.isBlank()) {
                    try {
                        Marca marcaPadre = buscarOCrearMarca(marcaPadreNombre.trim());
                        // Asegurar que la marca padre tenga ID
                        if (marcaPadre.getId() == null) {
                            marcaRepository.flush();
                            marcaPadre = marcaRepository.findByNombreIgnoreCase(marcaPadreNombre.trim())
                                    .orElseThrow(() -> new RuntimeException(
                                            "No se pudo crear o encontrar marca: " + marcaPadreNombre));
                        }

                        // Si hay VLINEA, crear la marca hijo asociada al padre
                        if (columnasMap.containsKey("VLINEA")) {
                            String marcaHijoNombre = obtenerValorCelda(row,
                                    obtenerIndiceColumna(columnasMap, "VLINEA"));
                            if (marcaHijoNombre != null && !marcaHijoNombre.isBlank()) {
                                try {
                                    Marca marcaHijo = buscarOCrearMarca(marcaHijoNombre.trim());
                                    // Asegurar que la marca hijo tenga ID
                                    if (marcaHijo.getId() == null) {
                                        marcaRepository.flush();
                                        marcaHijo = marcaRepository.findByNombreIgnoreCase(marcaHijoNombre.trim())
                                                .orElseThrow(() -> new RuntimeException(
                                                        "No se pudo crear o encontrar marca: " + marcaHijoNombre));
                                    }
                                    // Establecer la relación padre-hijo solo si no tiene padre o tiene un padre
                                    // diferente
                                    Marca padreActual = marcaHijo.getPadre();
                                    if (padreActual == null || !padreActual.getId().equals(marcaPadre.getId())) {
                                        marcaHijo.setPadre(marcaPadre);
                                        marcaRepository.save(marcaHijo);
                                    }
                                } catch (Exception e) {
                                    String errorMsg = String.format("Error en columna VLINEA='%s' (padre: '%s'): %s",
                                            marcaHijoNombre, marcaPadreNombre, e.getMessage());
                                    erroresFila.add(errorMsg);
                                    log.warn("❌ Fila {} VALIDACIONES - {}", rowIndex + 1, errorMsg);
                                    // Continuar sin fallar toda la fila
                                }
                            }
                        }
                    } catch (Exception e) {
                        String errorMsg = String.format("Error en columna VMARCA='%s': %s", marcaPadreNombre, e.getMessage());
                        erroresFila.add(errorMsg);
                        log.warn("❌ Fila {} VALIDACIONES - {}", rowIndex + 1, errorMsg);
                        // Continuar sin fallar toda la fila
                    }
                }
            }

            // VORIGEN
            if (columnasMap.containsKey("VORIGEN")) {
                String origenNombre = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "VORIGEN"));
                if (origenNombre != null && !origenNombre.isBlank()) {
                    try {
                        buscarOCrearOrigen(origenNombre.trim());
                    } catch (Exception e) {
                        String errorMsg = String.format("Error en columna VORIGEN='%s': %s", origenNombre, e.getMessage());
                        erroresFila.add(errorMsg);
                        log.warn("❌ Fila {} VALIDACIONES - {}", rowIndex + 1, errorMsg);
                        // Continuar sin fallar toda la fila
                    }
                }
            }

            // MATERIALES - Tabla MATERIALES, columna MATERIAL
            if (columnasMap.containsKey("MATERIAL")) {
                String materialNombre = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "MATERIAL"));
                if (materialNombre != null && !materialNombre.isBlank()) {
                    try {
                        buscarOCrearMaterial(materialNombre.trim());
                    } catch (Exception e) {
                        String errorMsg = String.format("Error en columna MATERIAL='%s': %s", materialNombre, e.getMessage());
                        erroresFila.add(errorMsg);
                        log.warn("❌ Fila {} VALIDACIONES - {}", rowIndex + 1, errorMsg);
                    }
                }
            }

            // PROVEEDORES - Tabla PROVEEDORES, columna PROVEEDOR y ALIAS para apodo
            if (columnasMap.containsKey("PROVEEDOR")) {
                String proveedorNombre = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "PROVEEDOR"));
                if (proveedorNombre != null && !proveedorNombre.isBlank()) {
                    try {
                        String alias = null;
                        if (columnasMap.containsKey("ALIAS")) {
                            alias = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "ALIAS"));
                        }
                        buscarOCrearProveedor(proveedorNombre.trim(), alias);
                    } catch (Exception e) {
                        String errorMsg = String.format("Error en columna PROVEEDOR='%s': %s", proveedorNombre, e.getMessage());
                        erroresFila.add(errorMsg);
                        log.warn("❌ Fila {} VALIDACIONES - {}", rowIndex + 1, errorMsg);
                    }
                }
            }

            // TIPOS - Tabla TIPO1, TIPO2, TIPO3 con jerarquía
            // TIPO1: nivel 1 (sin padre)
            // TIPO2: nivel 2 (hijo de TIPO1), columna ID TIPO1 indica el padre
            // TIPO3: nivel 3 (hijo de TIPO2), columna ID TIPO2 indica el padre
            if (columnasMap.containsKey("TIPO1")) {
                String tipo1Nombre = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "TIPO1"));
                if (tipo1Nombre != null && !tipo1Nombre.isBlank()) {
                    try {
                        buscarOCrearTipo(tipo1Nombre.trim(), null); // Sin padre para nivel 1
                    } catch (Exception e) {
                        String errorMsg = String.format("Error en columna TIPO1='%s': %s", tipo1Nombre, e.getMessage());
                        erroresFila.add(errorMsg);
                        log.warn("❌ Fila {} VALIDACIONES - {}", rowIndex + 1, errorMsg);
                    }
                }
            }

            // TIPO2: necesita ID TIPO1 para encontrar el padre
            if (columnasMap.containsKey("TIPO2")) {
                String tipo2Nombre = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "TIPO2"));
                if (tipo2Nombre != null && !tipo2Nombre.isBlank()) {
                    try {
                        Tipo padreTipo2 = null;
                        if (columnasMap.containsKey("ID TIPO1")) {
                            String idTipo1Str = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "ID TIPO1"));
                            if (idTipo1Str != null && !idTipo1Str.isBlank()) {
                                try {
                                    Integer idTipo1 = Integer.parseInt(idTipo1Str.trim());
                                    padreTipo2 = tipoRepository.findById(idTipo1).orElse(null);
                                    if (padreTipo2 == null) {
                                        log.warn("⚠️  Fila {} VALIDACIONES - ID TIPO1={} no encontrado para TIPO2='{}'", 
                                                rowIndex + 1, idTipo1, tipo2Nombre);
                                    }
                                } catch (NumberFormatException e) {
                                    log.warn("⚠️  Fila {} VALIDACIONES - ID TIPO1='{}' no es un número válido para TIPO2='{}'", 
                                            rowIndex + 1, idTipo1Str, tipo2Nombre);
                                }
                            }
                        }
                        buscarOCrearTipo(tipo2Nombre.trim(), padreTipo2);
                    } catch (Exception e) {
                        String errorMsg = String.format("Error en columna TIPO2='%s': %s", tipo2Nombre, e.getMessage());
                        erroresFila.add(errorMsg);
                        log.warn("❌ Fila {} VALIDACIONES - {}", rowIndex + 1, errorMsg);
                    }
                }
            }

            // TIPO3: necesita ID TIPO2 para encontrar el padre
            if (columnasMap.containsKey("TIPO3")) {
                String tipo3Nombre = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "TIPO3"));
                if (tipo3Nombre != null && !tipo3Nombre.isBlank()) {
                    try {
                        Tipo padreTipo3 = null;
                        if (columnasMap.containsKey("ID TIPO2")) {
                            String idTipo2Str = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "ID TIPO2"));
                            if (idTipo2Str != null && !idTipo2Str.isBlank()) {
                                try {
                                    Integer idTipo2 = Integer.parseInt(idTipo2Str.trim());
                                    padreTipo3 = tipoRepository.findById(idTipo2).orElse(null);
                                    if (padreTipo3 == null) {
                                        log.warn("⚠️  Fila {} VALIDACIONES - ID TIPO2={} no encontrado para TIPO3='{}'", 
                                                rowIndex + 1, idTipo2, tipo3Nombre);
                                    }
                                } catch (NumberFormatException e) {
                                    log.warn("⚠️  Fila {} VALIDACIONES - ID TIPO2='{}' no es un número válido para TIPO3='{}'", 
                                            rowIndex + 1, idTipo2Str, tipo3Nombre);
                                }
                            }
                        }
                        buscarOCrearTipo(tipo3Nombre.trim(), padreTipo3);
                    } catch (Exception e) {
                        String errorMsg = String.format("Error en columna TIPO3='%s': %s", tipo3Nombre, e.getMessage());
                        erroresFila.add(errorMsg);
                        log.warn("❌ Fila {} VALIDACIONES - {}", rowIndex + 1, errorMsg);
                    }
                }
            }

            // VCLASIF1, VCLASIF2 → ClasifGral (VCLASIF2 es hija de VCLASIF1)
            // VCLASIF3, VCLASIF4 → ClasifGastro (VCLASIF4 es hija de VCLASIF3)
            
            // Procesar ClasifGral (VCLASIF1 y VCLASIF2)
            ClasifGral clasifGralPadre = null;
            if (columnasMap.containsKey("VCLASIF1")) {
                String clasif1Nombre = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "VCLASIF1"));
                if (clasif1Nombre != null && !clasif1Nombre.isBlank()) {
                    try {
                        clasifGralPadre = buscarOCrearClasifGral(clasif1Nombre.trim(), null);
                    } catch (Exception e) {
                        String errorMsg = String.format("Error en columna VCLASIF1='%s': %s",
                                clasif1Nombre, e.getMessage());
                        erroresFila.add(errorMsg);
                        log.warn("❌ Fila {} VALIDACIONES - {}", rowIndex + 1, errorMsg);
                    }
                }
            }
            
            if (columnasMap.containsKey("VCLASIF2")) {
                String clasif2Nombre = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "VCLASIF2"));
                if (clasif2Nombre != null && !clasif2Nombre.isBlank()) {
                    try {
                        buscarOCrearClasifGral(clasif2Nombre.trim(), clasifGralPadre);
                    } catch (Exception e) {
                        String errorMsg = String.format("Error en columna VCLASIF2='%s' (hija de VCLASIF1): %s",
                                clasif2Nombre, e.getMessage());
                        erroresFila.add(errorMsg);
                        log.warn("❌ Fila {} VALIDACIONES - {}", rowIndex + 1, errorMsg);
                    }
                }
            }
            
            // Procesar ClasifGastro (VCLASIF3 y VCLASIF4)
            ClasifGastro clasifGastroPadre = null;
            if (columnasMap.containsKey("VCLASIF3")) {
                String clasif3Nombre = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "VCLASIF3"));
                if (clasif3Nombre != null && !clasif3Nombre.isBlank()) {
                    try {
                        clasifGastroPadre = buscarOCrearClasifGastro(clasif3Nombre.trim(), null);
                    } catch (Exception e) {
                        String errorMsg = String.format("Error en columna VCLASIF3='%s': %s",
                                clasif3Nombre, e.getMessage());
                        erroresFila.add(errorMsg);
                        log.warn("❌ Fila {} VALIDACIONES - {}", rowIndex + 1, errorMsg);
                    }
                }
            }
            
            if (columnasMap.containsKey("VCLASIF4")) {
                String clasif4Nombre = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "VCLASIF4"));
                if (clasif4Nombre != null && !clasif4Nombre.isBlank()) {
                    try {
                        buscarOCrearClasifGastro(clasif4Nombre.trim(), clasifGastroPadre);
                    } catch (Exception e) {
                        String errorMsg = String.format("Error en columna VCLASIF4='%s' (hija de VCLASIF3): %s",
                                clasif4Nombre, e.getMessage());
                        erroresFila.add(errorMsg);
                        log.warn("❌ Fila {} VALIDACIONES - {}", rowIndex + 1, errorMsg);
                    }
                }
            }
            
            // Si hay errores en la fila, lanzar excepción con todos los errores
            if (!erroresFila.isEmpty()) {
                String mensajeCompleto = String.format("Fila %d de VALIDACIONES tiene %d error(es): %s",
                        rowIndex + 1, erroresFila.size(), String.join("; ", erroresFila));
                throw new RuntimeException(mensajeCompleto);
            }
        } catch (RuntimeException e) {
            // Re-lanzar RuntimeException tal cual (ya tiene el mensaje formateado)
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Error general procesando fila %d de VALIDACIONES: %s",
                    rowIndex + 1, e.getMessage());
            log.error("❌ ERROR CRÍTICO en fila {} de VALIDACIONES: {}", rowIndex + 1, e.getMessage(), e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Procesa una fila de la hoja MLA-ENVIOS
     * Columnas: MLA, SKU, CATEGORIA, etc.
     */
    private void procesarFilaMlaEnvios(Row row, int rowIndex, Map<String, Integer> columnasMap) {
        String mla = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "MLA"));
        if (mla == null || mla.isBlank()) {
            throw new IllegalArgumentException("MLA es requerido");
        }

        String skuRaw = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "SKU"));
        if (skuRaw == null || skuRaw.isBlank()) {
            throw new IllegalArgumentException("SKU es requerido");
        }

        // Normalizar SKU (eliminar .0 si es un número decimal)
        String skuTemp = skuRaw.trim();
        if (skuTemp.endsWith(".0")) {
            skuTemp = skuTemp.substring(0, skuTemp.length() - 2);
        }
        final String sku = skuTemp; // Variable final para usar en lambda

        // Buscar o crear producto (similar a procesarFilaMaster)
        Producto producto = productoRepository.findBySku(sku)
                .orElseGet(() -> {
                    // Si el producto no existe, crear uno básico con valores por defecto para
                    // satisfacer @NotNull
                    Producto nuevo = new Producto();
                    nuevo.setSku(sku);
                    nuevo.setDescripcion(""); // Default para @NotNull
                    nuevo.setTituloWeb(""); // Default para @NotNull
                    nuevo.setIva(BigDecimal.ZERO);
                    nuevo.setFechaCreacion(LocalDateTime.now(ZONA_ARG));
                    // Establecer valores por defecto para satisfacer @NotNull
                    nuevo.setOrigen(buscarOCrearOrigen("SIN ORIGEN"));
                    nuevo.setClasifGral(buscarOCrearClasifGral("SIN CLASIFICACION"));
                    nuevo.setTipo(buscarOCrearTipoPorId(0, "SIN TIPO")); // Buscar o crear tipo por defecto
                    return productoRepository.save(nuevo);
                });

        // Buscar si ya existe el MLA por código, o crear uno nuevo
        Mla mlaEntity = mlaRepository.findByMla(mla)
                .orElseGet(() -> {
                    Mla nuevo = new Mla();
                    nuevo.setMla(mla);
                    return nuevo;
                });

        // Actualizar precio de envío si existe la columna
        if (columnasMap.containsKey("PRECIO FINAL")) {
            String precioStr = obtenerValorCelda(row, obtenerIndiceColumna(columnasMap, "PRECIO FINAL"));
            if (precioStr != null && !precioStr.isBlank()) {
                try {
                    BigDecimal precio = new BigDecimal(precioStr.replace(",", "."));
                    mlaEntity.setPrecioEnvio(precio);
                } catch (NumberFormatException e) {
                    // Ignorar si no se puede parsear
                }
            }
        }

        mlaRepository.save(mlaEntity);
        // Asignar MLA al producto
        producto.setMla(mlaEntity);
        productoRepository.save(producto);
    }

    // ============================================================
    // MÉTODOS AUXILIARES PARA BUSCAR O CREAR ENTIDADES
    // ============================================================

    private Marca buscarOCrearMarca(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        String nombreNormalizado = nombre.trim();
        String key = nombreNormalizado.toUpperCase();
        return cacheMarcas.computeIfAbsent(key, k -> {
            return marcaRepository.findByNombreIgnoreCase(nombreNormalizado)
                    .orElseGet(() -> {
                        Marca nueva = new Marca();
                        nueva.setNombre(nombreNormalizado);
                        return marcaRepository.save(nueva);
                    });
        });
    }

    private Origen buscarOCrearOrigen(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        String nombreNormalizado = nombre.trim();
        String key = nombreNormalizado.toUpperCase();
        return cacheOrigenes.computeIfAbsent(key, k -> {
            return origenRepository.findByOrigenIgnoreCase(nombreNormalizado)
                    .orElseGet(() -> {
                        Origen nuevo = new Origen();
                        nuevo.setOrigen(nombreNormalizado);
                        return origenRepository.save(nuevo);
                    });
        });
    }

    private Tipo buscarOCrearTipo(String nombre) {
        return buscarOCrearTipo(nombre, null);
    }

    private Tipo buscarOCrearTipo(String nombre, Tipo padre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return buscarOCrearTipoPorId(0, "SIN TIPO");
        }
        String nombreNormalizado = nombre.trim();
        // Clave del cache: incluir el padre para diferenciar tipos con el mismo nombre pero diferentes padres
        // Si el padre es null, usar "null", si tiene padre, usar su ID
        String keyPadre = (padre == null || padre.getId() == null) ? "null" : String.valueOf(padre.getId());
        String key = nombreNormalizado.toUpperCase() + "|PADRE:" + keyPadre;
        
        return cacheTipos.computeIfAbsent(key, k -> {
            // Buscar por nombre Y padre (puede haber tipos con el mismo nombre pero diferentes padres)
            // Si el padre tiene ID, buscar por nombre y padre
            // Si el padre es null, buscar solo por nombre (tipos raíz)
            Optional<Tipo> tipoOpt;
            if (padre != null && padre.getId() != null) {
                tipoOpt = tipoRepository.findByNombreIgnoreCaseAndPadre(nombreNormalizado, padre);
            } else {
                // Para tipos sin padre, buscar por nombre y padre null
                tipoOpt = tipoRepository.findByNombreIgnoreCaseAndPadre(nombreNormalizado, null);
            }
            
            if (tipoOpt.isPresent()) {
                return tipoOpt.get();
            }
            
            // Si no existe, crear uno nuevo
            Tipo nuevo = new Tipo();
            nuevo.setNombre(nombreNormalizado);
            nuevo.setPadre(padre);
            Tipo guardado = tipoRepository.save(nuevo);
            // Hacer flush para asegurar que el tipo tenga ID antes de usarlo como padre
            tipoRepository.flush();
            return guardado;
        });
    }

    /**
     * Busca un tipo por ID, o lo crea si no existe
     * @param id El ID del tipo a buscar
     * @param nombreDefault El nombre a usar si se debe crear el tipo
     * @return El tipo encontrado o cREADo
     */
    private Tipo buscarOCrearTipoPorId(Integer id, String nombreDefault) {
        // Crear variable final para usar en lambdas
        final Integer idBuscado = (id == null) ? 0 : id;
        return tipoRepository.findById(idBuscado)
                .orElseGet(() -> {
                    // Si el tipo con id no existe, intentar buscar uno por nombre
                    if (nombreDefault != null && !nombreDefault.trim().isEmpty()) {
                        String nombreDefaultNormalizado = nombreDefault.trim();
                        String key = nombreDefaultNormalizado.toUpperCase();
                        return cacheTipos.computeIfAbsent(key, k -> {
                            return tipoRepository.findByNombreIgnoreCase(nombreDefaultNormalizado)
                                    .orElseGet(() -> {
                                        // Si tampoco existe por nombre, crear uno nuevo
                                        Tipo nuevo = new Tipo();
                                        nuevo.setNombre(nombreDefaultNormalizado);
                                        Tipo guardado = tipoRepository.save(nuevo);
                                        log.warn("Tipo con id {} no existía, se creó uno nuevo con id {} y nombre '{}'", 
                                                idBuscado, guardado.getId(), guardado.getNombre());
                                        return guardado;
                                    });
                        });
                    }
                    // Si no hay nombre por defecto, crear uno con nombre "SIN TIPO"
                    String key = "SIN TIPO";
                    return cacheTipos.computeIfAbsent(key, k -> {
                        Tipo nuevo = new Tipo();
                        nuevo.setNombre("SIN TIPO");
                        Tipo guardado = tipoRepository.save(nuevo);
                        log.warn("No había tipos en la BD, se creó uno nuevo con id {} y nombre 'SIN TIPO'", 
                                guardado.getId());
                        return guardado;
                    });
                });
    }

    private ClasifGral buscarOCrearClasifGral(String nombre) {
        return buscarOCrearClasifGral(nombre, null);
    }

    private ClasifGral buscarOCrearClasifGral(String nombre, ClasifGral padre) {
        // IMPORTANTE: La restricción UNIQUE está solo en 'nombre', no en (nombre,
        // id_padre)
        // Por lo tanto, solo puede haber UNA clasificación con ese nombre,
        // independientemente del padre
        
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        String nombreNormalizado = nombre.trim();
        String key = nombreNormalizado.toUpperCase();
        return cacheClasifGral.computeIfAbsent(key, k -> {
            // Primero buscar por nombre (sin considerar el padre, ya que la restricción es
            // solo en nombre)
            Optional<ClasifGral> existente = clasifGralRepository.findByNombreIgnoreCase(nombreNormalizado);

            if (existente.isPresent()) {
                // Si existe, verificar si el padre coincide. Si no coincide, usar la existente
                // de todas formas
                // (porque la restricción UNIQUE no permite crear otra con el mismo nombre)
                ClasifGral encontrada = existente.get();
                if (padre != null && encontrada.getPadre() != null
                        && !encontrada.getPadre().getId().equals(padre.getId())) {
                    log.warn("Clasificación '{}' existe con padre diferente. Usando la existente (ID: {})",
                            nombreNormalizado, encontrada.getId());
                } else if (padre == null && encontrada.getPadre() != null) {
                    log.warn("Clasificación '{}' existe con padre. Usando la existente (ID: {})",
                            nombreNormalizado, encontrada.getId());
                } else if (padre != null && encontrada.getPadre() == null) {
                    log.warn("Clasificación '{}' existe sin padre pero se esperaba con padre. Usando la existente (ID: {})",
                            nombreNormalizado, encontrada.getId());
                }
                return encontrada;
            }

            // Si no existe, intentar crear
            try {
                ClasifGral nueva = new ClasifGral();
                nueva.setNombre(nombreNormalizado);
                nueva.setPadre(padre);
                return clasifGralRepository.save(nueva);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // Si falla por duplicado (puede haber sido cREADo por otra fila en paralelo),
                // buscar nuevamente
                log.debug("Intento de crear clasificación duplicada '{}', buscando existente...", nombreNormalizado);
                return clasifGralRepository.findByNombreIgnoreCase(nombreNormalizado)
                        .orElseThrow(() -> new RuntimeException(
                                "No se pudo crear ni encontrar clasificación: " + nombreNormalizado + " - " + e.getMessage()));
            }
        });
    }

    private Catalogo buscarOCrearCatalogo(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        String nombreNormalizado = nombre.trim();
        String key = nombreNormalizado.toUpperCase();
        return cacheCatalogos.computeIfAbsent(key, k -> {
            return catalogoRepository.findByCatalogoIgnoreCase(nombreNormalizado)
                    .orElseGet(() -> {
                        Catalogo nuevo = new Catalogo();
                        nuevo.setCatalogo(nombreNormalizado);
                        return catalogoRepository.save(nuevo);
                    });
        });
    }

    private Canal buscarOCrearCanal(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        String nombreNormalizado = nombre.trim();
        String key = nombreNormalizado.toUpperCase();
        return cacheCanales.computeIfAbsent(key, k -> {
            return canalRepository.findByCanalIgnoreCase(nombreNormalizado)
                    .orElseGet(() -> {
                        Canal nuevo = new Canal();
                        nuevo.setCanal(nombreNormalizado);
                        return canalRepository.save(nuevo);
                    });
        });
    }

    private Material buscarOCrearMaterial(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        String nombreNormalizado = nombre.trim();
        String key = nombreNormalizado.toUpperCase();
        return cacheMateriales.computeIfAbsent(key, k -> {
            return materialRepository.findByMaterialIgnoreCase(nombreNormalizado)
                    .orElseGet(() -> {
                        Material nuevo = new Material();
                        nuevo.setMaterial(nombreNormalizado);
                        return materialRepository.save(nuevo);
                    });
        });
    }

    private Proveedor buscarOCrearProveedor(String nombre) {
        return buscarOCrearProveedor(nombre, null);
    }

    private Proveedor buscarOCrearProveedor(String nombre, String apodo) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        String nombreNormalizado = nombre.trim();
        String key = nombreNormalizado.toUpperCase();
        return cacheProveedores.computeIfAbsent(key, k -> {
            return proveedorRepository.findByProveedorIgnoreCase(nombreNormalizado)
                    .orElseGet(() -> {
                        Proveedor nuevo = new Proveedor();
                        nuevo.setProveedor(nombreNormalizado);
                        // El apodo es requerido
                        if (apodo != null && !apodo.trim().isEmpty()) {
                            String apodoNormalizado = apodo.trim();
                            nuevo.setApodo(apodoNormalizado.length() > 50 ? apodoNormalizado.substring(0, 50) : apodoNormalizado);
                        } else {
                            // Usar el nombre como apodo por defecto (truncado a 50 caracteres)
                            String apodoDefault = nombreNormalizado.length() > 50 ? nombreNormalizado.substring(0, 50) : nombreNormalizado;
                            nuevo.setApodo(apodoDefault);
                        }
                        return proveedorRepository.save(nuevo);
                    });
        });
    }
}

