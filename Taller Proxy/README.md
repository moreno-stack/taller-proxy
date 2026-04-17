# Proxy IA con Spring Boot y Frontend JavaScript

Este proyecto es una demo de un proxy de IA con backend en Spring Boot y frontend estático en JavaScript.

## Estructura

- `backend/` - aplicación Spring Boot
- `backend/src/main/resources/static/` - frontend HTML/CSS/JS servidos por Spring Boot

## Características

- Control de cuota mensual por usuario
- Rate limit por minuto
- Simulación de respuesta de IA
- Interfaz tipo chat
- Indicador de tokens usados y cuota restante
- Historial de uso diario (últimos 7 días)
- Modal de upgrade simulado

## Ejecutar

1. Abre una terminal en `c:\Users\dulce\OneDrive\Escritorio\Taller Proxy\backend`
2. Ejecuta:

```powershell
mvn spring-boot:run
```

3. Abre `http://localhost:8080` en el navegador.

## Notas

- El backend guarda el estado en memoria.
- El frontend usa `fetch` para consumir `/api/chat` y `/api/usage`.
- Puedes cambiar el usuario enviando el header `X-User-Id` desde el cliente si quieres múltiples sesiones.
