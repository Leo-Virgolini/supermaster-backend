package ar.com.leo.super_master_backend.dominio.common.exception;

import ar.com.leo.super_master_backend.dominio.common.response.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ex.getMessage(), request.getDescription(false)));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ex.getMessage(), request.getDescription(false)));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(ex.getMessage(), request.getDescription(false)));
    }

    @ExceptionHandler(ServiceNotConfiguredException.class)
    public ResponseEntity<ErrorResponse> handleServiceNotConfigured(ServiceNotConfiguredException ex, WebRequest request) {
        String mensaje = String.format("[%s] %s", ex.getServicio(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(mensaje, request.getDescription(false)));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        String mensaje = "Error de integridad de datos";
        String rootMessage = getRootCauseMessage(ex);

        if (rootMessage.contains("Duplicate entry")) {
            String detalle = extraerDetalleDuplicado(rootMessage);
            mensaje = detalle != null
                    ? "Ya existe un registro con " + detalle
                    : "Ya existe un registro con estos datos";
        } else if (rootMessage.contains("foreign key constraint") || rootMessage.contains("FOREIGN KEY") || rootMessage.contains("Cannot delete or update")) {
            String tablaRelacionada = extraerTablaRelacionada(rootMessage);
            if (tablaRelacionada != null) {
                mensaje = "No se puede eliminar porque tiene registros relacionados en: " + tablaRelacionada;
            } else {
                System.err.println("[DEBUG FK] No se pudo extraer tabla del mensaje: " + rootMessage);
                mensaje = "No se puede eliminar porque tiene registros relacionados";
            }
        } else if (rootMessage.contains("cannot be null")) {
            String campo = extraerCampoNull(rootMessage);
            mensaje = campo != null
                    ? "El campo '" + campo + "' es requerido y está vacío"
                    : "Un campo requerido está vacío";
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(mensaje, request.getDescription(false)));
    }

    private String getRootCauseMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : "";
    }

    private String extraerTablaRelacionada(String message) {
        // Patrón MySQL: (`schema`.`tabla`, CONSTRAINT ...
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("`[^`]+`\\.`([^`]+)`.*CONSTRAINT");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return formatearNombreTabla(matcher.group(1));
        }
        // Patrón alternativo MySQL: constraint fails (`schema`.`tabla`,
        pattern = java.util.regex.Pattern.compile("fails\\s*\\(`[^`]+`\\.`([^`]+)`");
        matcher = pattern.matcher(message);
        if (matcher.find()) {
            return formatearNombreTabla(matcher.group(1));
        }
        // Patrón sin schema: (`tabla`, CONSTRAINT
        pattern = java.util.regex.Pattern.compile("\\(`([^`]+)`,\\s*CONSTRAINT");
        matcher = pattern.matcher(message);
        if (matcher.find()) {
            return formatearNombreTabla(matcher.group(1));
        }
        // Patrón H2/PostgreSQL: tabla referencing
        pattern = java.util.regex.Pattern.compile("table\\s+[\"']?([\\w_]+)[\"']?");
        matcher = pattern.matcher(message.toLowerCase());
        if (matcher.find()) {
            return formatearNombreTabla(matcher.group(1));
        }
        // Patrón genérico: FOREIGN KEY ... REFERENCES `tabla`
        pattern = java.util.regex.Pattern.compile("REFERENCES\\s+`([^`]+)`");
        matcher = pattern.matcher(message);
        if (matcher.find()) {
            return formatearNombreTabla(matcher.group(1));
        }
        return null;
    }

    private String extraerDetalleDuplicado(String message) {
        // Patrón: Duplicate entry 'valor' for key 'nombre_key'
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Duplicate entry '([^']+)' for key '([^']+)'");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            String valor = matcher.group(1);
            String key = matcher.group(2);
            // Limpiar nombre de key (quitar prefijos como tabla.UK_xxx)
            if (key.contains(".")) {
                key = key.substring(key.lastIndexOf(".") + 1);
            }
            return String.format("este valor: '%s'", valor);
        }
        return null;
    }

    private String extraerCampoNull(String message) {
        // Patrón MySQL: Column 'nombre_campo' cannot be null
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Column '([^']+)' cannot be null");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return formatearNombreCampo(matcher.group(1));
        }
        // Patrón Hibernate: propertyName.campo
        pattern = java.util.regex.Pattern.compile("property(?:Name)?[^:]*:\\s*([\\w.]+)");
        matcher = pattern.matcher(message);
        if (matcher.find()) {
            String campo = matcher.group(1);
            if (campo.contains(".")) {
                campo = campo.substring(campo.lastIndexOf(".") + 1);
            }
            return formatearNombreCampo(campo);
        }
        return null;
    }

    private String formatearNombreCampo(String campo) {
        // Convierte id_producto -> id producto, nombreCampo -> nombre campo
        return campo.replace("_", " ")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .toLowerCase();
    }

    private String formatearNombreTabla(String tabla) {
        // Convierte producto_catalogo -> Producto catalogo
        return tabla.replace("_", " ")
                .substring(0, 1).toUpperCase() + tabla.replace("_", " ").substring(1);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        StringBuilder mensaje = new StringBuilder("Errores de validación: ");
        ex.getBindingResult().getFieldErrors().forEach(error ->
                mensaje.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ")
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(mensaje.toString().trim(), request.getDescription(false)));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String mensaje = String.format("El parámetro '%s' debe ser de tipo %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "válido");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(mensaje, request.getDescription(false)));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex, WebRequest request) {
        String mensaje = String.format("El parámetro '%s' es requerido (tipo: %s)",
                ex.getParameterName(),
                ex.getParameterType());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(mensaje, request.getDescription(false)));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("Endpoint no encontrado: " + ex.getResourcePath(), request.getDescription(false)));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        String metodosPermitidos = ex.getSupportedHttpMethods() != null
                ? String.join(", ", ex.getSupportedHttpMethods().stream().map(Object::toString).toList())
                : "desconocido";
        String mensaje = String.format("Método '%s' no permitido. Métodos soportados: %s",
                ex.getMethod(), metodosPermitidos);
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of(mensaje, request.getDescription(false)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ex.getMessage(), request.getDescription(false)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, WebRequest request) {
        ex.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("Error interno del servidor", request.getDescription(false)));
    }

}