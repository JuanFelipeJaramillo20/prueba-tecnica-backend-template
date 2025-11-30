# Respuestas - Prueba Técnica Backend Developer

## 1. Escenario de Concurrencia (Black Friday) 🏃‍♂️

### Problema
Es Black Friday y el sistema recibe 50 pedidos por segundo del iPhone 15 que solo tiene 10 unidades en stock. El resultado es un inventario negativo (-5 unidades).

### Pregunta
¿Qué mecanismo de base de datos o de Spring Boot utilizarías para asegurar que nunca se venda más stock del que existe, asumiendo múltiples instancias de la API corriendo en paralelo?

### Tu Respuesta
```
Para garantizar que nunca se venda más stock del que existe, incluso con múltiples instancias de la API corriendo en paralelo, centraría la solución en la base de datos usando una operación atómica sobre la fila del producto.

La idea es que la verificación de stock y la actualización se hagan en un solo paso atómico, en lugar de:

1. SELECT stock
2. if (stock ≥ qty) { UPDATE stock = stock - qty }

porque entre el SELECT y el UPDATE otra transacción puede modificar el stock.

Entonces usaría un UPDATE condicional a nivel de base de datos:

sql
UPDATE products
SET stock = stock - :quantity
WHERE id = :productId
  AND stock >= :quantity;

Si queremos usar Spring data para hacer esta validación se podría ver como algo así:

  @Modifying
  @Query("""
      UPDATE Product p
      SET p.stock = p.stock - :quantity
      WHERE p.id = :productId
        AND p.stock >= :quantity
  """)
  int tryReserveStock(@Param("productId") Long productId,
                      @Param("quantity") int quantity);

y desde el service se llamaría:

  @Transactional
  public void reserveStock(Long productId, int quantity) {
      int updatedRows = productRepository.tryReserveStock(productId, quantity);
      if (updatedRows == 0) {
          throw new InsufficientStockException("Not enough stock");
      }
  }

Utilizaría esta solución ya que como la base de datos garantiza atomicidad, me evito tener que gestionar locks manualmente.
```

---

## 2. Pregunta Trampa de Arquitectura 🎯

### Propuesta del Junior Developer
Configurar TODAS las relaciones JPA (`@OneToMany`, `@ManyToOne`) con `FetchType.EAGER` para:
- Traer toda la data en una sola consulta
- Evitar `LazyInitializationException`
- Mejorar el rendimiento

### Pregunta
¿Aceptarías este Pull Request? ¿Por qué sí o por qué no? ¿Qué impacto tendría con millones de registros?

### Tu Respuesta
```
No aceptaría este Pull Request.

¿Por qué no pondría todo en EAGER?

Terminamos cargando muchos datos innecesarios

Si cada vez que leo una entidad raíz arrastro todas sus relaciones @OneToMany y lo que venga detrás, termino trayendo medio modelo a memoria aunque el caso de uso solo necesite una parte.

Con muchos datos eso significa:

- Mucha RAM consumida innecesariamente

- Más tiempo de respuesta

- Más presión sobre el GC y pausas más largas

Consultas gigantes y difíciles de optimizar

- Hacer EAGER en todas partes suele generar joins enormes o múltiples consultas.

- Es muy fácil terminar con queries con cientos de columnas y muchas filas duplicadas solo para reconstruir el grafo de objetos en memoria.

- Eso en producción, con volumen real, se traduce en tiempos de respuesta malos.

Escala muy mal

- En local y con pocos datos puede “parecer” que funciona bien.

- Pero cuando tengamos millones de registros, cada endpoint que toca una entidad principal arrastra todo lo que cuelga de ella.

Esto va a generar endpoints lentos, alto consumo de memoria y servidores innecesariamente.

No ataca el problema real del LazyInitializationException

El LazyInitializationException casi siempre es síntoma de otro problema:

- Acceder a entidades fuera del contexto transaccional (por ejemplo, en el controller).

- Falta de una capa de servicio que se encargue de cargar lo necesario dentro de una transacción.

Poner todo en EAGER es básicamente tratar el síntoma y no la enfermedad.
```

---

## 3. Reflexiones Adicionales (Opcional) 💭

### Sobre el Refactoring Realizado
```
En el refactor el objetivo principal fue que OrderService.createOrder() dejara de ser un “método Dios” y se convirtiera en un orquestador claro del flujo de negocio.

Las decisiones más importantes que tomé fueron:

- Separar responsabilidades que antes estaban todas mezcladas:

  - Validación de la request = OrderRequestValidator

  - Mapeo de request a un modelo de dominio = OrderLinesFactory (OrderLine = producto + cantidad)

  - Validación de stock = StockValidator

  - Cálculo de precios = PriceCalculator

  - Regla de descuento = DiscountPolicy / VarietyDiscountPolicy

- Actualización de inventario = InventoryUpdater

- Introducir la clase OrderLine como modelo intermedio para trabajar la lógica de negocio sin depender directamente de la entidad JPA OrderItem.

- Mantener OrderService como un método que se lea como lenguaje natural:

  - Valido la request

  - Creo la orden

  - Construyo las líneas

  - Verifico stock

  - Calculo subtotal

  - Aplico descuentos

  - Actualizo inventario

  - Guardo
```

### Patrones de Diseño Aplicados
```
En el refactoring apliqué varios patrones (y principios) de forma bastante directa:

SRP (Single Responsibility Principle)
Cada clase hace una cosa:

  - OrderRequestValidator solo valida entrada

  - StockValidator solo se preocupa por stock

  - PriceCalculator solo calcula precios

  - VarietyDiscountPolicy solo decide el descuento de variedad, etc.

Strategy Pattern
La lógica de descuentos se abstrae detrás de una interfaz:

  - DiscountPolicy es el contrato.

  - VarietyDiscountPolicy es una implementación concreta.

Esto permite que en el futuro podamos agregar nuevas políticas de descuentos sin necesidad de tocar la clase de OrderService

Factory
Centraliza la creación de OrderLine a partir del CreateOrderRequest y evita que el servicio esté lleno de lógica de mapeo y búsquedas de producto.

Service de Orquestación
OrderService quedó como un servicio de dominio que orquesta los pasos del negocio pero delega la lógica a componentes especializados.

Testability como guía de diseño
El diseño está pensado para que cada pieza se pueda probar de manera aislada:

  - Tests unitarios de VarietyDiscountPolicy

  - Tests sobre OrderService usando mocks para las dependencias.
```

### Posibles Mejoras Futuras
```
Si tuviera más tiempo, algunas mejoras que implementaría serían:

Manejo robusto de concurrencia en el stock:
Implementar a nivel de base de datos un mecanismo para evitar overselling:

UPDATE ... WHERE stock >= cantidad

Más políticas de descuento
Permitir encadenar varias DiscountPolicy, por ejemplo:

  - Descuento por variedad

  - Descuento por cliente frecuente

  - Descuento por campaña (Black Friday, etc.)

y tener un “motor de descuentos” que las aplique en orden.

DTOs y separación más fuerte entre dominio y API
Introducir DTOs de respuesta específicos (OrderResponse) en lugar de exponer directamente entidades JPA, y así tener más control sobre lo que se expone en la API.

Más trazabilidad y observabilidad
  - Logs estructurados en cada paso del flujo (validación, cálculo, descuento, stock, persistencia).

  - Métricas básicas (número de órdenes, descuentos aplicados, fallos por stock).

Tests adicionales
  - Tests de integración con H2 para probar el flujo de punta a punta.

  - Casos negativos: producto no encontrado, stock insuficiente, request inválida, etc.
```