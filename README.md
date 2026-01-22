# Super Master Backend

Backend REST para gestión de **productos**, **canales**, **precios calculados**, **reglas de descuento** y **conceptos de gasto**.

## Requisitos

- Java 25
- Maven 3.x
- MySQL 8.4 o superior

## Configuración

### Base de datos

1. Crear una base de datos MySQL llamada `supermaster`
2. Configurar las credenciales en `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/supermaster?useSSL=false&serverTimezone=America/Argentina/Buenos_Aires&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=admin
```

## Ejecución

### Usando Maven Wrapper (recomendado)

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

### Usando Maven instalado

```bash
mvn spring-boot:run
```

## Sistema de Cálculo de Precios

El backend incluye un sistema de cálculo de precios con **recálculo automático** cuando se modifican las entidades relacionadas.

### Triggers de Recálculo Automático

| Entidad Modificada | Alcance del Recálculo |
|--------------------|----------------------|
| Producto (costo, iva, clasifGastro) | Ese producto en todos sus canales |
| ProductoMargen (márgenes) | Ese producto en todos sus canales |
| ConceptoGasto (porcentaje, aplicaSobre) | Todos los productos de canales que usan ese concepto |
| CanalConcepto (asignar/quitar) | Todos los productos del canal |
| CanalConceptoCuota (porcentaje cuotas) | Todos los productos del canal |
| Canal (canalBase) | Todos los productos del canal |
| Proveedor (porcentaje financiación) | Todos los productos de ese proveedor |
| ReglaDescuento | Todos los productos del canal |
| Promoción (asignar/desasignar) | Ese producto en ese canal |
| MLA (precioEnvio) | Todos los productos con ese MLA |
| ClasifGastro (esMaquina) | Todos los productos de esa clasificación |

### Endpoint de Recálculo Manual

```
POST /api/precios/calcular
```

| Parámetro | Comportamiento |
|-----------|----------------|
| (sin parámetros) | Recalcula TODOS los productos en TODOS los canales |
| `productoId` | Recalcula ese producto en todos sus canales |
| `productoId` + `canalId` | Recalcula ese producto en ese canal |
| `productoId` + `canalId` + `cuotas` | Recalcula para esa combinación específica |

## Tests

### Ejecutar todos los tests

```bash
.\mvnw.cmd test
```

### Ejecutar test de recálculo automático

```bash
.\mvnw.cmd test -Dtest=RecalculoAutomaticoIntegrationTest
```

### Ejecutar un test específico

```bash
.\mvnw.cmd test -Dtest=RecalculoAutomaticoIntegrationTest#testRecalculoPorCambioCostoProducto
```

## Backup de Base de Datos

### Script de backup automático

Crear archivo `backup_mysql.bat`:

```batch
@echo off
setlocal

set MYSQL_USER=root
set MYSQL_PASS=admin
set MYSQL_DB=supermaster
set BACKUP_DIR=C:\backups\mysql
set MYSQLDUMP_PATH="C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqldump.exe"

for /f "tokens=1-3 delims=/" %%a in ('date /t') do set DATE=%%c-%%b-%%a
set FILENAME=%MYSQL_DB%_%DATE%.sql

if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

%MYSQLDUMP_PATH% -u%MYSQL_USER% -p%MYSQL_PASS% --single-transaction --routines --triggers %MYSQL_DB% > "%BACKUP_DIR%\%FILENAME%"

:: Eliminar backups de más de 7 días
forfiles /p "%BACKUP_DIR%" /m *.sql /d -7 /c "cmd /c del @path" 2>nul
```

### Programar backup diario (PowerShell como Admin)

```powershell
$action = New-ScheduledTaskAction -Execute "C:\backups\backup_mysql.bat"
$trigger = New-ScheduledTaskTrigger -Daily -At 3:00AM
Register-ScheduledTask -TaskName "MySQL Backup Diario" -Action $action -Trigger $trigger
```

## Tecnologías

- **Spring Boot 4.0.1** - Framework principal
- **Spring Data JPA** - Persistencia de datos
- **MySQL 8.4** - Base de datos
- **Lombok** - Reducción de código boilerplate
- **MapStruct 1.6.3** - Mapeo de objetos
- **Apache POI 5.5.0** - Procesamiento de archivos Excel

## Estructura del Proyecto

```
src/
├── main/
│   ├── java/ar/com/leo/super_master_backend/
│   │   ├── config/                 # Configuración global
│   │   └── dominio/
│   │       ├── canal/              # Canales y conceptos
│   │       ├── producto/           # Productos, precios, márgenes
│   │       │   ├── calculo/        # Servicio de cálculo de precios
│   │       │   └── mla/            # MLAs de MercadoLibre
│   │       ├── concepto_gasto/     # Conceptos de gasto
│   │       ├── regla_descuento/    # Reglas de descuento
│   │       ├── proveedor/          # Proveedores
│   │       └── ...
│   └── resources/
│       ├── application.properties  # Configuración
│       └── db/                     # Scripts de migración
├── test/
│   ├── java/                       # Tests de integración
│   └── resources/
│       └── application-test.properties
```

## Documentación

- **API Endpoints**: Ver `frontend_api.md`
- **Guía de desarrollo**: Ver `CLAUDE.md`

## Notas

- El proyecto está configurado para no crear ni modificar tablas automáticamente (`ddl-auto=none`)
- Los logs se guardan en `logs/application.log`
- El tamaño máximo de archivos subidos es de 10MB
- La API base está en `http://localhost:8080/api`
