# Microservicio MS-Médicos (MedicTime)

Microservicio backend desarrollado en Spring Boot 3 para la gestión de médicos, integrado con Microsoft Entra ID para autenticación JWT y preparado para despliegue en AWS RDS / ECR.

## Estructura del Proyecto
- **Model**: `Medico.java` (Campos `nombre` y `apellido` separados)
- **Repository**: `MedicoRepository.java`
- **Service**: `MedicoService.java`
- **Controller**: `MedicoController.java` (`/api/medicos`)
- **Config**: `SecurityConfig.java` (Modo pruebas `.permitAll()`)

## Ejecución Local
```bash
mvn spring-boot:run
```
Acceso a la consola H2: `http://localhost:8089/h2-console`
