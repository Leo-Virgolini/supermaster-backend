# Super Master Backend

Backend del sistema Super Master desarrollado con Spring Boot.

## Requisitos

- Java 25
- Maven 3.x
- MySQL 8.x o superior

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

## Tecnologías

- **Spring Boot 3.5.7** - Framework principal
- **Spring Data JPA** - Persistencia de datos
- **MySQL** - Base de datos
- **Lombok** - Reducción de código boilerplate
- **MapStruct** - Mapeo de objetos
- **Apache POI** - Procesamiento de archivos Excel
- **Spring Boot DevTools** - Herramientas de desarrollo

## Estructura del proyecto

```
src/
├── main/
│   ├── java/
│   │   └── ar/com/leo/super_master_backend/
│   │       ├── dominio/          # Entidades y lógica de negocio
│   │       ├── infraestructura/  # Repositorios y servicios
│   │       └── SuperMasterBackendApplication.java
│   └── resources/
│       └── application.properties # Configuración de la aplicación
```

## Notas

- El proyecto está configurado para no crear ni modificar tablas automáticamente (`ddl-auto=none`)
- Los logs se guardan en la carpeta `logs/application.log`
- El tamaño máximo de archivos subidos es de 10MB
