package com.github.echodot69420.fakeartist;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
class ItemController {
    private final Map<Integer, Item> items = new LinkedHashMap<>();
    private final AtomicInteger nextItemId = new AtomicInteger(1);

    @GetMapping("/")
    ResponseEntity<Resource> homePage() {
        return ResponseEntity.ok(new FileSystemResource(Path.of("index.html")));
    }

    @GetMapping("/lobby")
    ResponseEntity<Resource> lobbyPage() {
        return ResponseEntity.ok(new FileSystemResource(Path.of("lobby.html")));
    }

    @GetMapping("/items")
    synchronized List<Map<String, Object>> listItems() {
        return items.entrySet().stream().map(entry -> itemMap(entry.getKey(), entry.getValue())).toList();
    }

    @GetMapping("/items/{itemId}")
    synchronized Map<String, Object> getItem(@PathVariable int itemId) {
        Item item = items.get(itemId);
        if (item == null) throw new ApiException(HttpStatus.NOT_FOUND, "Item not found");
        return itemMap(itemId, item);
    }

    @PostMapping("/items")
    synchronized ResponseEntity<Map<String, Object>> createItem(@RequestBody Item item) {
        int id = nextItemId.getAndIncrement();
        items.put(id, item);
        return ResponseEntity.status(HttpStatus.CREATED).body(itemMap(id, item));
    }

    @DeleteMapping("/items/{itemId}")
    synchronized ResponseEntity<Void> deleteItem(@PathVariable int itemId) {
        if (items.remove(itemId) == null) throw new ApiException(HttpStatus.NOT_FOUND, "Item not found");
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> itemMap(int id, Item item) {
        return Map.of("id", id, "name", item.name(), "price", item.price(), "in_stock", item.in_stock());
    }
}
