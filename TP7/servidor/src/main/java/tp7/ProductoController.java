/*
 * Ignacio Ruiz - DNI 39.040.338
 */

package tp7;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/productos")
public class ProductoController {

    private final List<Producto> productos = new ArrayList<>();
    private final ObjectMapper mapper;
    private int proximoId = 1;

    public ProductoController(ObjectMapper mapper) {
        this.mapper = mapper;
        agregar(new Producto(null, "Teclado", 25000, 10));
        agregar(new Producto(null, "Mouse", 12000, 25));
        agregar(new Producto(null, "Monitor 24", 180000, 4));
    }

    @GetMapping
    public List<Producto> listar() {
        System.out.println("GET /productos -> " + productos.size() + " productos");
        return productos;
    }

    @GetMapping("/{id}")
    public Producto buscar(@PathVariable int id) {
        System.out.println("GET /productos/" + id);
        for (Producto p : productos) {
            if (p.getId() == id) {
                return p;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el producto con id " + id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Producto crear(@RequestBody String json) throws Exception {
        System.out.println("POST /productos");
        System.out.println("  JSON recibido: " + json);

        Producto nuevo = mapper.readValue(json, Producto.class);

        if (nuevo.getNombre() == null || nuevo.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        if (nuevo.getPrecio() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio tiene que ser mayor a 0");
        }
        if (nuevo.getStock() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El stock no puede ser negativo");
        }

        agregar(nuevo);
        System.out.println("  Producto guardado con id " + nuevo.getId());
        return nuevo;
    }

    private void agregar(Producto p) {
        p.setId(proximoId++);
        productos.add(p);
    }
}
