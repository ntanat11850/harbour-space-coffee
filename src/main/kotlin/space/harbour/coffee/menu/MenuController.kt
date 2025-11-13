package space.harbour.coffee.menu

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

enum class Category {
    COFFEE,
    TEA,
    PASTRY,
    OTHER
}

enum class Size {
    SMALL,
    MEDIUM,
    LARGE
}

data class MenuItem(
    val id: Long,
    val name: String,
    val price: BigDecimal,
    val description: String?,
    val category: Category,
    val size: Size,
    val available: Boolean = true
)

data class MenuItemRequest(
    val name: String,
    val price: BigDecimal,
    val description: String? = null,
    val category: Category,
    val size: Size,
    val available: Boolean = true
)

data class ApiError(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String?,
    val path: String?
)

class MenuItemNotFoundException(message: String) : RuntimeException(message)

@Service
class MenuItemService {

    private val items = ConcurrentHashMap<Long, MenuItem>()
    private val idSequence = AtomicLong(0L)

    init {
        // Seed demo data
        create(
            MenuItemRequest(
                name = "Latte",
                price = BigDecimal("3.50"),
                description = "Espresso with steamed milk",
                category = Category.COFFEE,
                size = Size.MEDIUM,
                available = true
            )
        )
        create(
            MenuItemRequest(
                name = "Green Tea",
                price = BigDecimal("2.50"),
                description = "Hot Japanese green tea",
                category = Category.TEA,
                size = Size.SMALL,
                available = true
            )
        )
    }

    fun getAll(
        category: Category?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?,
        available: Boolean?
    ): List<MenuItem> {
        return items.values
            .asSequence()
            .filter { item -> category == null || item.category == category }
            .filter { item -> minPrice == null || item.price >= minPrice }
            .filter { item -> maxPrice == null || item.price <= maxPrice }
            .filter { item -> available == null || item.available == available }
            .sortedBy { it.id }
            .toList()
    }

    fun getById(id: Long): MenuItem {
        return items[id] ?: throw MenuItemNotFoundException("Menu item with id=$id not found")
    }

    fun create(request: MenuItemRequest): MenuItem {
        val id = idSequence.incrementAndGet()
        val item = MenuItem(
            id = id,
            name = request.name,
            price = request.price,
            description = request.description,
            category = request.category,
            size = request.size,
            available = request.available
        )
        items[id] = item
        return item
    }

    fun update(id: Long, request: MenuItemRequest): MenuItem {
        if (!items.containsKey(id)) {
            throw MenuItemNotFoundException("Menu item with id=$id not found")
        }

        val updated = MenuItem(
            id = id,
            name = request.name,
            price = request.price,
            description = request.description,
            category = request.category,
            size = request.size,
            available = request.available
        )
        items[id] = updated
        return updated
    }

    fun delete(id: Long) {
        val removed = items.remove(id)
        if (removed == null) {
            throw MenuItemNotFoundException("Menu item with id=$id not found")
        }
    }
}

@RestController
@RequestMapping("/menu-items")
class MenuController(
    private val menuItemService: MenuItemService
) {

    @GetMapping
    fun getAll(
        @RequestParam(required = false) category: Category?,
        @RequestParam(required = false) minPrice: BigDecimal?,
        @RequestParam(required = false) maxPrice: BigDecimal?,
        @RequestParam(required = false) available: Boolean?
    ): List<MenuItem> {
        return menuItemService.getAll(category, minPrice, maxPrice, available)
    }
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): MenuItem {
        return menuItemService.getById(id)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: MenuItemRequest): MenuItem {
        return menuItemService.create(request)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: MenuItemRequest
    ): MenuItem {
        return menuItemService.update(id, request)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        menuItemService.delete(id)
    }

    @ExceptionHandler(MenuItemNotFoundException::class)
    fun handleNotFound(
        ex: MenuItemNotFoundException,
        request: WebRequest
    ): ResponseEntity<ApiError> {
        val error = ApiError(
            status = HttpStatus.NOT_FOUND.value(),
            error = HttpStatus.NOT_FOUND.reasonPhrase,
            message = ex.message,
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error)
    }
}
