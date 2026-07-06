package com.campus.server.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 商品微服务 Feign 客户端 — SOA 服务契约 */
@FeignClient(name = "product-service", url = "${product.service.url:http://product-service:8002}")
public interface ProductServiceClient {

    @GetMapping("/products")
    List<Map<String, Object>> listProducts();

    @GetMapping("/products/{name}")
    Map<String, Object> getProduct(@PathVariable("name") String name);

    @GetMapping("/store/{sid}/products")
    List<Map<String, Object>> getStoreProducts(@PathVariable("sid") String storeId);

    @PostMapping("/products")
    Map<String, Object> addProduct(@RequestBody Map<String, Object> body);

    @PutMapping("/products/{id}")
    Map<String, Object> updateProduct(@PathVariable("id") int id, @RequestBody Map<String, Object> body);

    @DeleteMapping("/products/{id}")
    Map<String, Object> deleteProduct(@PathVariable("id") int id);
}
