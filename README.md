# AAStore (base)
Catálogo + instalador de APKs compatibles con Android Auto.

1. Sube `catalog.json` a GitHub y pon su URL raw en `CatalogRepo.CATALOG_URL`.
2. Calcula el hash de cada APK: `sha256sum app.apk`.
3. Abre la carpeta en Android Studio y ejecuta en tu móvil.
4. En el móvil: concede "Instalar apps desconocidas" a AAStore.
5. En Android Auto: Ajustes > toca 10 veces en "Versión" > Modo desarrollador > activa "Fuentes desconocidas".
