# Reglas de ofuscación/optimización para la variante release.
# La app no usa reflexión ni serialización dinámica, así que las reglas
# por defecto de AGP y de las librerías de AndroidX son suficientes.

# Conserva los nombres de fichero y las líneas para que los informes de fallo
# sigan siendo legibles.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
